package com.example.a002

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.a002.model.AutomationConfig
import com.example.a002.service.AutomationService
import com.example.a002.ui.theme._002Theme
import com.example.a002.viewmodel.AutomationViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AutomationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            _002Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutomationScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(viewModel: AutomationViewModel) {
    val context = LocalContext.current
    val serviceEnabled = remember { mutableStateOf(false) }
    val automationService = AutomationService.getInstance()
    var showSaveDialog by remember { mutableStateOf(false) }
    var configName by remember { mutableStateOf("") }

    val isRecording by viewModel.isRecording.collectAsState()
    val savedConfigs by viewModel.savedConfigs.collectAsState()
    val currentConfig by viewModel.currentConfig.collectAsState()

    LaunchedEffect(Unit) {
        automationService?.serviceStatus?.collect { status ->
            serviceEnabled.value = status
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!serviceEnabled.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.service_not_enabled),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text(stringResource(R.string.enable_service))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.toggleRecording() },
                    enabled = serviceEnabled.value,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isRecording) 
                            stringResource(R.string.save_actions)
                        else 
                            stringResource(R.string.record_actions)
                    )
                }

                if (isRecording) {
                    Button(
                        onClick = { showSaveDialog = true }
                    ) {
                        Text(stringResource(R.string.save_recording))
                    }
                }
            }

            if (savedConfigs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.saved_configs),
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedConfigs) { config ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentConfig?.name == config.name)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(config.name)
                                Row {
                                    IconButton(
                                        onClick = { viewModel.startAutomation(config) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.start)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteConfig(config) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_config_title)) },
            text = {
                TextField(
                    value = configName,
                    onValueChange = { configName = it },
                    label = { Text(stringResource(R.string.config_name)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (configName.isNotEmpty()) {
                            viewModel.saveCurrentRecording(configName)
                            configName = ""
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_config))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}