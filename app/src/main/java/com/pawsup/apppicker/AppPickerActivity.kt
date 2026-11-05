package com.pawsup.apppicker

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawsup.R
import com.pawsup.ui.theme.PawsUpTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppPickerActivity : ComponentActivity() {

    private val vm: AppPickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PawsUpTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                AppPickerScreen(
                    state = state,
                    onSearch = vm::setSearchQuery,
                    onToggle = vm::toggleMonitored,
                    onDone = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onSearch: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_picker_title)) },
                actions = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.app_picker_done)) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text(stringResource(R.string.app_picker_search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (state.searchQuery.isNotBlank() && state.filteredApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.app_picker_empty_search))
                }
                return@Scaffold
            }

            LazyColumn {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    val monitored = app.packageName in state.monitoredPackages
                    AppRow(app, monitored = monitored, onToggle = { onToggle(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, monitored: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = monitored, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawableImage(drawable = app.icon, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = monitored, onCheckedChange = null)
    }
}

@Composable
private fun DrawableImage(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
}
