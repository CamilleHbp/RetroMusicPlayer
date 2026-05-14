package code.name.monkey.retromusic.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["normalized_name", "type"], unique = true),
        Index(value = ["type"])
    ]
)
data class MusicTagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "tag_id")
    val tagId: Long = 0L,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    val type: String
)
