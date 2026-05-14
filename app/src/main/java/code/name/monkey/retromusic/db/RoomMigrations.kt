package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE LyricsEntity")
        database.execSQL("DROP TABLE BlackListStoreEntity")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS MusicTagEntity (
                tag_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                type TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_MusicTagEntity_normalized_name_type " +
                "ON MusicTagEntity(normalized_name, type)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_MusicTagEntity_type ON MusicTagEntity(type)"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS SongTagEntity (
                song_id INTEGER NOT NULL,
                song_path TEXT NOT NULL,
                date_modified INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY(song_id, song_path, tag_id),
                FOREIGN KEY(tag_id) REFERENCES MusicTagEntity(tag_id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_SongTagEntity_song_id ON SongTagEntity(song_id)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_SongTagEntity_song_path ON SongTagEntity(song_path)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_SongTagEntity_tag_id ON SongTagEntity(tag_id)"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS DynamicPlaylistEntity (
                dynamic_playlist_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                include_mode TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS DynamicPlaylistRuleEntity (
                dynamic_playlist_rule_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dynamic_playlist_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                type TEXT,
                polarity TEXT NOT NULL,
                FOREIGN KEY(dynamic_playlist_id) REFERENCES DynamicPlaylistEntity(dynamic_playlist_id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_DynamicPlaylistRuleEntity_dynamic_playlist_id " +
                "ON DynamicPlaylistRuleEntity(dynamic_playlist_id)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_DynamicPlaylistRuleEntity_normalized_name_type_polarity " +
                "ON DynamicPlaylistRuleEntity(normalized_name, type, polarity)"
        )
    }
}
