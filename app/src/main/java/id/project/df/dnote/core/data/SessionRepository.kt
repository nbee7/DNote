package id.project.df.dnote.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SessionData(
    val noteIds: List<String>,
    val activeTabIndex: Int
)

@Singleton
class SessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val TAB_NOTE_IDS = stringPreferencesKey("tab_note_ids")
        val ACTIVE_TAB_INDEX = intPreferencesKey("active_tab_index")
    }

    suspend fun saveSession(noteIds: List<String>, activeIndex: Int) {
        dataStore.edit { prefs ->
            prefs[TAB_NOTE_IDS] = noteIds.joinToString(",")
            prefs[ACTIVE_TAB_INDEX] = activeIndex
        }
    }

    suspend fun getSession(): SessionData? {
        val prefs = dataStore.data.first()
        val idsString = prefs[TAB_NOTE_IDS] ?: return null
        if (idsString.isBlank()) return null
        val noteIds = idsString.split(",").filter { it.isNotBlank() }
        if (noteIds.isEmpty()) return null
        val activeIndex = prefs[ACTIVE_TAB_INDEX] ?: 0
        return SessionData(noteIds, activeIndex)
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(TAB_NOTE_IDS)
            prefs.remove(ACTIVE_TAB_INDEX)
        }
    }
}
