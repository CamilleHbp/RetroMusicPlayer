package code.name.monkey.retromusic.model

data class MusicTagSet(
    val genres: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val tags: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = genres.isEmpty() && moods.isEmpty() && tags.isEmpty()
}

enum class MusicTagType {
    GENRE,
    MOOD,
    TAG
}
