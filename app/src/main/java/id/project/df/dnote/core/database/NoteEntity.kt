package id.project.df.dnote.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["deletedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
