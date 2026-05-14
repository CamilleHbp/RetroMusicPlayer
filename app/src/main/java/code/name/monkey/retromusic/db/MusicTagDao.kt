package code.name.monkey.retromusic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MusicTagDao {
    @Query("SELECT * FROM MusicTagEntity ORDER BY type ASC, name COLLATE NOCASE ASC")
    suspend fun tags(): List<MusicTagEntity>

    @Query(
        """
        SELECT * FROM MusicTagEntity
        WHERE normalized_name LIKE '%' || :query || '%'
        ORDER BY type ASC, name COLLATE NOCASE ASC
        """
    )
    suspend fun searchTags(query: String): List<MusicTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: MusicTagEntity): Long

    @Query(
        """
        SELECT tag_id FROM MusicTagEntity
        WHERE normalized_name = :normalizedName AND type = :type
        LIMIT 1
        """
    )
    suspend fun tagId(normalizedName: String, type: String): Long?

    @Query(
        """
        SELECT tag_id FROM MusicTagEntity
        WHERE normalized_name IN (:normalizedNames)
        AND (:type IS NULL OR type = :type)
        """
    )
    suspend fun tagIds(normalizedNames: List<String>, type: String?): List<Long>

    @Query(
        """
        SELECT DISTINCT song_id FROM SongTagEntity
        WHERE tag_id IN (:tagIds)
        """
    )
    suspend fun songIdsWithAnyTag(tagIds: List<Long>): List<Long>

    @Query(
        """
        SELECT song_id FROM SongTagEntity
        WHERE tag_id IN (:tagIds)
        GROUP BY song_id
        HAVING COUNT(DISTINCT tag_id) >= :requiredCount
        """
    )
    suspend fun songIdsWithAllTags(tagIds: List<Long>, requiredCount: Int): List<Long>

    @Query("DELETE FROM SongTagEntity WHERE song_id = :songId AND song_path = :songPath")
    suspend fun deleteTagsForSong(songId: Long, songPath: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongTags(songTags: List<SongTagEntity>)

    @Transaction
    suspend fun replaceSongTags(
        songId: Long,
        songPath: String,
        dateModified: Long,
        tags: List<MusicTagEntity>
    ) {
        deleteTagsForSong(songId, songPath)
        val songTags = tags.map { tag ->
            val tagId = findOrCreateTag(tag)
            SongTagEntity(
                songId = songId,
                songPath = songPath,
                dateModified = dateModified,
                tagId = tagId
            )
        }
        if (songTags.isNotEmpty()) {
            insertSongTags(songTags)
        }
    }

    @Insert
    suspend fun insertDynamicPlaylist(dynamicPlaylist: DynamicPlaylistEntity): Long

    @Query("SELECT * FROM DynamicPlaylistEntity ORDER BY name COLLATE NOCASE ASC")
    suspend fun dynamicPlaylists(): List<DynamicPlaylistEntity>

    @Query("SELECT * FROM DynamicPlaylistRuleEntity WHERE dynamic_playlist_id = :dynamicPlaylistId ORDER BY polarity ASC, name COLLATE NOCASE ASC")
    suspend fun dynamicPlaylistRules(dynamicPlaylistId: Long): List<DynamicPlaylistRuleEntity>

    @Query("DELETE FROM DynamicPlaylistEntity WHERE dynamic_playlist_id = :dynamicPlaylistId")
    suspend fun deleteDynamicPlaylist(dynamicPlaylistId: Long)

    @Query("DELETE FROM DynamicPlaylistRuleEntity WHERE dynamic_playlist_id = :dynamicPlaylistId")
    suspend fun deleteDynamicPlaylistRules(dynamicPlaylistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDynamicPlaylistRules(rules: List<DynamicPlaylistRuleEntity>)

    @Transaction
    suspend fun replaceDynamicPlaylistRules(
        dynamicPlaylistId: Long,
        rules: List<DynamicPlaylistRuleEntity>
    ) {
        deleteDynamicPlaylistRules(dynamicPlaylistId)
        if (rules.isNotEmpty()) {
            insertDynamicPlaylistRules(rules)
        }
    }

    @Transaction
    suspend fun findOrCreateTag(tag: MusicTagEntity): Long {
        val insertedId = insertTag(tag)
        if (insertedId != -1L) {
            return insertedId
        }
        return tagId(tag.normalizedName, tag.type) ?: error("Tag was not inserted or found")
    }
}
