package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.db.DynamicPlaylistEntity
import code.name.monkey.retromusic.db.DynamicPlaylistRuleEntity
import code.name.monkey.retromusic.db.MusicTagDao
import code.name.monkey.retromusic.db.MusicTagEntity
import code.name.monkey.retromusic.model.DynamicTagIncludeMode
import code.name.monkey.retromusic.model.DynamicTagPlaylist
import code.name.monkey.retromusic.model.DynamicTagRulePolarity
import code.name.monkey.retromusic.model.MusicTagRule
import code.name.monkey.retromusic.model.MusicTagSet
import code.name.monkey.retromusic.model.MusicTagType
import code.name.monkey.retromusic.model.SavedDynamicTagPlaylist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicTagMetadata

interface MusicTagRepository {
    suspend fun tags(): List<MusicTagEntity>
    suspend fun searchTags(query: String): List<MusicTagEntity>
    suspend fun replaceSongTags(song: Song, tagSet: MusicTagSet)
    suspend fun dynamicSongs(spec: DynamicTagPlaylist): List<Song>
    suspend fun dynamicPlaylists(): List<SavedDynamicTagPlaylist>
    suspend fun createDynamicPlaylist(name: String, spec: DynamicTagPlaylist): Long
    suspend fun replaceDynamicPlaylistRules(dynamicPlaylistId: Long, spec: DynamicTagPlaylist)
    suspend fun deleteDynamicPlaylist(dynamicPlaylistId: Long)
}

class RealMusicTagRepository(
    private val musicTagDao: MusicTagDao,
    private val songRepository: SongRepository
) : MusicTagRepository {

    override suspend fun tags(): List<MusicTagEntity> = musicTagDao.tags()

    override suspend fun searchTags(query: String): List<MusicTagEntity> =
        musicTagDao.searchTags("%${MusicTagMetadata.normalizeKey(query)}%")

    override suspend fun replaceSongTags(song: Song, tagSet: MusicTagSet) {
        musicTagDao.replaceSongTags(
            songId = song.id,
            songPath = song.data,
            dateModified = song.dateModified,
            tags = tagSet.toEntities()
        )
    }

    override suspend fun dynamicSongs(spec: DynamicTagPlaylist): List<Song> {
        val allSongs = songRepository.songs()
        val includedTagIds = tagIds(spec.includedTags)
        val excludedTagIds = tagIds(spec.excludedTags)

        val includedSongIds = when {
            includedTagIds.isEmpty() -> allSongs.map { it.id }.toSet()
            spec.includeMode == DynamicTagIncludeMode.ANY ->
                musicTagDao.songIdsWithAnyTag(includedTagIds).toSet()
            else -> includedTagIds
                .map { musicTagDao.songIdsWithAnyTag(listOf(it)).toSet() }
                .reduceOrNull { acc, ids -> acc.intersect(ids) }
                .orEmpty()
        }
        val excludedSongIds = if (excludedTagIds.isEmpty()) {
            emptySet()
        } else {
            musicTagDao.songIdsWithAnyTag(excludedTagIds).toSet()
        }

        return allSongs.filter { it.id in includedSongIds && it.id !in excludedSongIds }
    }

    override suspend fun dynamicPlaylists(): List<SavedDynamicTagPlaylist> {
        return musicTagDao.dynamicPlaylists().map { playlist ->
            val rules = musicTagDao.dynamicPlaylistRules(playlist.dynamicPlaylistId)
            SavedDynamicTagPlaylist(
                id = playlist.dynamicPlaylistId,
                name = playlist.name,
                spec = DynamicTagPlaylist(
                    includedTags = rules.toMusicTagRules(DynamicTagRulePolarity.INCLUDE),
                    excludedTags = rules.toMusicTagRules(DynamicTagRulePolarity.EXCLUDE),
                    includeMode = runCatching {
                        DynamicTagIncludeMode.valueOf(playlist.includeMode)
                    }.getOrDefault(DynamicTagIncludeMode.ALL)
                )
            )
        }
    }

    override suspend fun createDynamicPlaylist(name: String, spec: DynamicTagPlaylist): Long {
        val playlistId = musicTagDao.insertDynamicPlaylist(
            DynamicPlaylistEntity(name = name.trim(), includeMode = spec.includeMode.name)
        )
        replaceDynamicPlaylistRules(playlistId, spec)
        return playlistId
    }

    override suspend fun replaceDynamicPlaylistRules(
        dynamicPlaylistId: Long,
        spec: DynamicTagPlaylist
    ) {
        musicTagDao.replaceDynamicPlaylistRules(
            dynamicPlaylistId,
            spec.toRuleEntities(dynamicPlaylistId)
        )
    }

    override suspend fun deleteDynamicPlaylist(dynamicPlaylistId: Long) {
        musicTagDao.deleteDynamicPlaylist(dynamicPlaylistId)
    }

    private suspend fun tagIds(rules: List<MusicTagRule>): List<Long> {
        if (rules.isEmpty()) return emptyList()
        return rules
            .groupBy { it.type }
            .flatMap { (type, typedRules) ->
                val normalizedNames = typedRules
                    .map { MusicTagMetadata.normalizeKey(it.name) }
                    .filter { it.isNotEmpty() }
                    .distinct()
                if (normalizedNames.isEmpty()) {
                    emptyList()
                } else {
                    musicTagDao.tagIds(normalizedNames, type?.name)
                }
            }
    }

    private fun MusicTagSet.toEntities(): List<MusicTagEntity> {
        return genres.toEntities(MusicTagType.GENRE) +
            moods.toEntities(MusicTagType.MOOD) +
            tags.toEntities(MusicTagType.TAG)
    }

    private fun List<String>.toEntities(type: MusicTagType): List<MusicTagEntity> {
        return map { MusicTagMetadata.normalizeName(it) }
            .filter { it.isNotEmpty() }
            .distinctBy { MusicTagMetadata.normalizeKey(it) }
            .map {
                MusicTagEntity(
                    name = it,
                    normalizedName = MusicTagMetadata.normalizeKey(it),
                    type = type.name
                )
            }
    }

    private fun DynamicTagPlaylist.toRuleEntities(
        dynamicPlaylistId: Long
    ): List<DynamicPlaylistRuleEntity> {
        return includedTags.toRuleEntities(dynamicPlaylistId, DynamicTagRulePolarity.INCLUDE) +
            excludedTags.toRuleEntities(dynamicPlaylistId, DynamicTagRulePolarity.EXCLUDE)
    }

    private fun List<MusicTagRule>.toRuleEntities(
        dynamicPlaylistId: Long,
        polarity: DynamicTagRulePolarity
    ): List<DynamicPlaylistRuleEntity> {
        return mapNotNull { rule ->
            val name = MusicTagMetadata.normalizeName(rule.name)
            val normalizedName = MusicTagMetadata.normalizeKey(name)
            if (normalizedName.isEmpty()) {
                null
            } else {
                DynamicPlaylistRuleEntity(
                    dynamicPlaylistId = dynamicPlaylistId,
                    name = name,
                    normalizedName = normalizedName,
                    type = rule.type?.name,
                    polarity = polarity.name
                )
            }
        }.distinctBy { "${it.polarity}:${it.type}:${it.normalizedName}" }
    }

    private fun List<DynamicPlaylistRuleEntity>.toMusicTagRules(
        polarity: DynamicTagRulePolarity
    ): List<MusicTagRule> {
        return filter { it.polarity == polarity.name }
            .map {
                MusicTagRule(
                    name = it.name,
                    type = it.type?.let { type -> runCatching { MusicTagType.valueOf(type) }.getOrNull() }
                )
            }
    }
}
