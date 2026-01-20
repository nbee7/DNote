package id.project.df.dnote.feature.note.data.repository

import id.project.df.dnote.core.common.util.IdGenerator
import id.project.df.dnote.core.common.util.TimeProvider
import id.project.df.dnote.core.database.NoteEntity
import id.project.df.dnote.core.database.NotesDao
import id.project.df.dnote.feature.note.data.mapper.toDomain
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val dao: NotesDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : NoteRepositoryInterface {

    override fun observeNotes(query: String): Flow<Result<List<Note>>> = flow {
        try {
            val q = query.trim()
            val source = if (q.isEmpty()) dao.observeAllActive() else dao.observeSearchActive(q)

            source.collect { list ->
                val notes = list.mapNotNull { it.toDomain() }
                emit(Result.Success(notes))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }


    override fun getNote(id: String): Flow<Result<Note>> = flow {
        try {
            val note = dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()

            if (note != null) {
                emit(Result.Success(note))
            } else {
                emit(Result.Error(IllegalArgumentException("Note not found or deleted")))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }


    override suspend fun create(title: String, content: String): String {
        val now = timeProvider.nowMillis()
        val id = idGenerator.newId()
        dao.insert(
            NoteEntity(
                id = id,
                title = title,
                content = content,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
        )
        return id
    }

    override suspend fun update(id: String, title: String, content: String) {
        dao.updateContent(id = id, title = title, content = content, updatedAt = timeProvider.nowMillis())
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, deletedAt = timeProvider.nowMillis())
    }
}