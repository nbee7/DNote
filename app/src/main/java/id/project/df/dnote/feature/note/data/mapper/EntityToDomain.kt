package id.project.df.dnote.feature.note.data.mapper

import id.project.df.dnote.core.database.NoteEntity
import id.project.df.dnote.feature.note.domain.model.Note

fun NoteEntity.toDomain(): Note? =
    if (deletedAt != null) null else Note(id, content, createdAt, updatedAt)

fun Note.toEntity(): NoteEntity =
    NoteEntity(id, content, createdAt, updatedAt, deletedAt = null)
