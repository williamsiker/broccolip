package com.example.testapp.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ConvertersP {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Entity(
    tableName = "archivo",
    indices = [Index(value = ["filename"], unique = true)]
)
data class Archivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val filename: String,
    val thumbnail: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Archivo

        if (id != other.id) return false
        if (uri != other.uri) return false
        if (filename != other.filename) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}

@Dao
interface ArchivoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArchivo(archivo: Archivo): Long

    @Query("SELECT * FROM archivo")
    fun getAllFiles(): Flow<List<Archivo>>

    @Query("SELECT * FROM archivo WHERE id = :archivoId")
    fun getArchivoById(archivoId: Long): Flow<Archivo>

    @Query("DELETE FROM archivo WHERE id = :archivoId")
    suspend fun deleteArchivoById(archivoId: Long)
}

@Database(entities = [Archivo::class], version = 4, exportSchema = false)
@TypeConverters(ConvertersP::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun archivoDao(): ArchivoDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "archivo_database"
                ).addMigrations(MIGRATION_1_2) // Añade la migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE archivo ADD COLUMN thumbnail BLOB")
    }
}
