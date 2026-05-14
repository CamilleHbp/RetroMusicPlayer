package code.name.monkey.retromusic.model

data class MusicTagRule(
    val name: String,
    val type: MusicTagType? = null
)

data class DynamicTagPlaylist(
    val includedTags: List<MusicTagRule> = emptyList(),
    val excludedTags: List<MusicTagRule> = emptyList(),
    val includeMode: DynamicTagIncludeMode = DynamicTagIncludeMode.ALL
)

data class SavedDynamicTagPlaylist(
    val id: Long,
    val name: String,
    val spec: DynamicTagPlaylist
)

enum class DynamicTagIncludeMode {
    ALL,
    ANY
}

enum class DynamicTagRulePolarity {
    INCLUDE,
    EXCLUDE
}
