package code.name.monkey.retromusic.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DynamicPlaylistEntity::class,
            parentColumns = ["dynamic_playlist_id"],
            childColumns = ["dynamic_playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dynamic_playlist_id"]),
        Index(value = ["normalized_name", "type", "polarity"])
    ]
)
data class DynamicPlaylistRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "dynamic_playlist_rule_id")
    val dynamicPlaylistRuleId: Long = 0L,
    @ColumnInfo(name = "dynamic_playlist_id")
    val dynamicPlaylistId: Long,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    val type: String?,
    val polarity: String
)
