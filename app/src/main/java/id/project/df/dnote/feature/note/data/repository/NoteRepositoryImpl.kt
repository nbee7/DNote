package id.project.df.dnote.feature.note.data.repository

import id.project.df.dnote.core.common.util.IdGenerator
import id.project.df.dnote.core.common.util.TimeProvider
import id.project.df.dnote.core.database.NoteEntity
import id.project.df.dnote.core.database.NotesDao
import id.project.df.dnote.feature.note.data.mapper.toDomain
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val dao: NotesDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : NoteRepositoryInterface {

    override fun observeNotes(query: String): Flow<List<Note>> {
        val q = query.trim()
        val source = if (q.isEmpty()) dao.observeAllActive() else dao.observeSearchActive(q)
        return source.map { list -> list.map { it.toDomain() } as List<Note> }
    }

    override suspend fun getNote(id: String): Note? =
        dao.getById(id)?.takeIf { it.deletedAt == null }?.toDomain()

    override suspend fun create(content: String): String {
        val now = timeProvider.nowMillis()
        val id = idGenerator.newId()
        dao.insert(
            NoteEntity(
                id = id,
                content = content,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
        )
        return id
    }

    override suspend fun update(id: String, content: String) {
        dao.updateContent(id = id, content = content, updatedAt = timeProvider.nowMillis())
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id = id, deletedAt = timeProvider.nowMillis())
    }
}