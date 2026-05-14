package code.name.monkey.retromusic.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DynamicPlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "dynamic_playlist_id")
    val dynamicPlaylistId: Long = 0L,
    val name: String,
    @ColumnInfo(name = "include_mode")
    val includeMode: String
)
