package com.leanote.next

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.prefs.SettingsStore
import com.leanote.next.ui.components.LoadingBox
import com.leanote.next.ui.navigation.LeanoteNavHost
import com.leanote.next.ui.theme.LeanoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var accountStore: AccountStore

    private var widgetNoteId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it > 0 }

        setContent {
            val settings by settingsStore.settings.collectAsState(initial = AppSettings())
            val accounts by accountStore.accounts.collectAsState(initial = null as List<Account>?)

            LeanoteTheme(settings) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val list = accounts
                    if (list == null) {
                        LoadingBox()
                    } else {
                        LeanoteNavHost(
                            hasAccounts = list.isNotEmpty(),
                            initialNoteId = widgetNoteId,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (id > 0) {
            setIntent(intent)
            widgetNoteId = id
            recreate()
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
