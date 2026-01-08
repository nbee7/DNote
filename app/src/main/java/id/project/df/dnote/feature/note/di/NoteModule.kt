package id.project.df.dnote.feature.note.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.project.df.dnote.feature.note.data.repository.NotesRepositoryImpl
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NoteRepositoryInterface
}