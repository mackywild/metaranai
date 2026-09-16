package jp.metaranai.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * V0.7.0 archive database.
 *
 * Important compatibility rule:
 * - SharedPreferences `external_artists` remains the portable JSON source used by backups.
 * - This SQLite database is a local indexed mirror for archive scale/performance.
 * - Old JSON backups can therefore rebuild this database at any time.
 */
class MetalArchiveDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    data class Row(
        val key: String,
        val name: String,
        val country: String,
        val genresText: String,
        val payloadJson: String
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE artists (
                artist_key TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                country TEXT NOT NULL,
                genres_text TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_artists_normalized_name ON artists(normalized_name)")
        db.execSQL("CREATE INDEX idx_artists_country ON artists(country)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // First DB schema. Future versions must add explicit migrations here.
    }

    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM artists", null).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    fun payloads(): List<String> = readableDatabase.rawQuery(
        "SELECT payload_json FROM artists ORDER BY normalized_name ASC",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }

    fun replaceAll(rows: List<Row>) {
        writableDatabase.transaction {
            delete("artists", null, null)
            rows.forEach { insertOrReplace(it) }
        }
    }

    fun bootstrapIfEmpty(rows: List<Row>) {
        if (rows.isEmpty() || count() > 0) return
        writableDatabase.transaction { rows.forEach { insertOrReplace(it) } }
    }

    private fun SQLiteDatabase.insertOrReplace(row: Row) {
        val values = ContentValues().apply {
            put("artist_key", row.key)
            put("name", row.name)
            put("normalized_name", normalize(row.name))
            put("country", row.country)
            put("genres_text", row.genresText)
            put("payload_json", row.payloadJson)
            put("updated_at", System.currentTimeMillis())
        }
        insertWithOnConflict("artists", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "metaranai_archive.db"
        private const val DATABASE_VERSION = 1

        fun artistKey(name: String, mbid: String?): String =
            mbid?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { "mbid:$it" }
                ?: "name:${normalize(name)}"

        private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
