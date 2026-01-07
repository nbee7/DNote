package id.project.df.dnote.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
        ORDER BY updatedAt DESC
    """)
    fun observeAllActive(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
          AND content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun observeSearchActive(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("""
        UPDATE notes
        SET content = :content,
            updatedAt = :updatedAt
        WHERE id = :id
          AND deletedAt IS NULL
    """)
    suspend fun updateContent(id: String, content: String, updatedAt: Long): Int

    @Query("""
        UPDATE notes
        SET deletedAt = :deletedAt
        WHERE id = :id
          AND deletedAt IS NULL
    """)
    suspend fun softDelete(id: String, deletedAt: Long): Int

    @Query("""
        UPDATE notes
        SET deletedAt = NULL
        WHERE id = :id
    """)
    suspend fun undoDelete(id: String): Int
}
