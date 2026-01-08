package id.project.df.dnote.core.common.util

import javax.inject.Inject
import java.util.UUID

interface IdGenerator {
    fun newId(): String
}

class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}