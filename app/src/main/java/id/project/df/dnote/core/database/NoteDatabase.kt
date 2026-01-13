package id.project.df.dnote.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
