package id.project.df.dnote.feature.note.domain.repository

import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepositoryInterface {
    fun observeNotes(query: String): Flow<Result<List<Note>>>
    fun getNote(id: String): Flow<Result<Note>>
    suspend fun create(title: String, content: String): String
    suspend fun update(id: String, content: String)
    suspend fun delete(id: String)}