package id.project.df.dnote.core.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import id.project.df.dnote.feature.note.di.NoteList
import javax.inject.Inject

typealias EntryProviderInstaller = EntryProviderScope<Any>.() -> Unit

class Navigator @Inject constructor(
    private val startDestination: NavKey
) {
    val backStack: SnapshotStateList<Any> = mutableStateListOf(startDestination)

    fun goTo(destination: Any) {
        backStack.add(destination)
    }

    fun goBack(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeLastOrNull()
            true
        } else {
            false
        }
    }

    fun navigateToList() {
        backStack.clear()
        backStack.add(NoteList)
    }
}
