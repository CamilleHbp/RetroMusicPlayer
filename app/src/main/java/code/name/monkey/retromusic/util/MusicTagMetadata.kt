package code.name.monkey.retromusic.util

import code.name.monkey.retromusic.model.MusicTagSet
import org.jaudiotagger.tag.FieldDataInvalidException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag

object MusicTagMetadata {
    private val splitRegex = Regex("[,;]")

    fun read(tag: Tag): MusicTagSet {
        return MusicTagSet(
            genres = tag.values(FieldKey.GENRE),
            moods = tag.values(FieldKey.MOOD),
            tags = tag.values(FieldKey.TAGS)
        )
    }

    fun write(tag: Tag, tagSet: MusicTagSet) {
        tag.writeValues(FieldKey.GENRE, tagSet.genres)
        tag.writeValues(FieldKey.MOOD, tagSet.moods)
        tag.writeValues(FieldKey.TAGS, tagSet.tags)
    }

    fun parseText(value: CharSequence?): List<String> {
        return value
            ?.split(splitRegex)
            ?.map { normalizeName(it) }
            ?.filter { it.isNotEmpty() }
            ?.distinctBy { normalizeKey(it) }
            .orEmpty()
    }

    fun formatText(values: List<String>): String = values.joinToString(", ")

    fun normalizeName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    fun normalizeKey(value: String): String = normalizeName(value).lowercase()

    private fun Tag.values(fieldKey: FieldKey): List<String> {
        return runCatching { getAll(fieldKey) }
            .getOrDefault(emptyList())
            .flatMap { it.split(splitRegex) }
            .map { normalizeName(it) }
            .filter { it.isNotEmpty() }
            .distinctBy { normalizeKey(it) }
    }

    private fun Tag.writeValues(fieldKey: FieldKey, values: List<String>) {
        val normalizedValues = values
            .map { normalizeName(it) }
            .filter { it.isNotEmpty() }
            .distinctBy { normalizeKey(it) }

        try {
            if (normalizedValues.isEmpty()) {
                deleteField(fieldKey)
            } else {
                setField(fieldKey, *normalizedValues.toTypedArray())
            }
        } catch (_: KeyNotFoundException) {
        } catch (_: FieldDataInvalidException) {
        }
    }
}
