package id.project.df.dnote.feature.note.domain.repository

import id.project.df.dnote.feature.note.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepositoryInterface {
    fun observeNotes(query: String): Flow<List<Note>>
    suspend fun getNote(id: String): Note?
    suspend fun create(content: String): String
    suspend fun update(id: String, content: String)
    suspend fun delete(id: String)}