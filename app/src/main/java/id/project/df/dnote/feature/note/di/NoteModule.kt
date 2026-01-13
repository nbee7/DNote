package id.project.df.dnote.feature.note.di

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import id.project.df.dnote.core.ui.navigation.EntryProviderInstaller
import id.project.df.dnote.core.ui.navigation.Navigator
import id.project.df.dnote.feature.note.data.repository.NotesRepositoryImpl
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.ObserveNotesUseCase
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
import id.project.df.dnote.feature.note.presentation.editor.NoteEditorRoute
import id.project.df.dnote.feature.note.presentation.editor.NoteEditorViewModel
import id.project.df.dnote.feature.note.presentation.list.NoteListRoute
import id.project.df.dnote.feature.note.presentation.list.NotesListViewModel
import kotlinx.serialization.Serializable
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NoteRepositoryInterface
}

@Serializable
data object NoteList : NavKey

@Serializable
data class NoteEditor(val id: String?) : NavKey


@Module
@InstallIn(ActivityRetainedComponent::class)
object UseCaseModule {

    @Provides
    fun provideObserveNotesUseCase(noteRepository: NoteRepositoryInterface): ObserveNotesUseCase {
        return ObserveNotesUseCase(noteRepository)
    }

    @Provides
    fun provideDeleteNoteUseCase(noteRepository: NoteRepositoryInterface): DeleteNoteUseCase {
        return DeleteNoteUseCase(noteRepository)
    }

    @Provides
    fun provideUpsertNoteUseCase(noteRepository: NoteRepositoryInterface): UpsertNoteUseCase {
        return UpsertNoteUseCase(noteRepository)
    }

    @Provides
    @ActivityRetainedScoped
    fun provideNavigator() : Navigator {
        return Navigator(startDestination = NoteEditor(null))
    }

    @IntoSet
    @Provides
    fun provideEntryProviderInstaller(
        navigator: Navigator,
        observeNotesUseCase: ObserveNotesUseCase,
        deleteNoteUseCase: DeleteNoteUseCase,
        upsertNoteUseCase: UpsertNoteUseCase,
        repo: NoteRepositoryInterface
    ): EntryProviderInstaller {
        return {
            entry<NoteList> {
                val viewModel = hiltViewModel<NotesListViewModel, NotesListViewModel.Factory>(
                    creationCallback = { factory ->
                        factory.create(
                            observeNotes = observeNotesUseCase,
                            deleteNote = deleteNoteUseCase
                        )
                    }
                )

                NoteListRoute(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        navigator.goTo(NoteEditor(noteId))
                    }
                )
            }

            entry<NoteEditor> { key ->
                val viewModel = hiltViewModel<NoteEditorViewModel, NoteEditorViewModel.Factory>(
                    creationCallback = { factory ->
                        factory.create(
                            navKey = key,
                            upsertNote = upsertNoteUseCase,
                            repo = repo
                        )
                    }
                )

                NoteEditorRoute(
                    onCloseEditor = {
                        navigator.navigateToList()
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}
