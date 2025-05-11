package dev.joetul.tao.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.joetul.tao.ui.theme.ThemeManager
import dev.joetul.tao.ui.theme.ThemeMode
import dev.joetul.tao.util.DoNotDisturbManager
import dev.joetul.tao.ui.components.ImportExportSection
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.core.net.toUri
import androidx.compose.material.icons.filled.PlayArrow
import android.media.MediaPlayer
import dev.joetul.tao.model.MeditationSounds
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("meditation_settings", Context.MODE_PRIVATE)
    val doNotDisturbManager = remember { DoNotDisturbManager(context) }
    val themeManager = remember { ThemeManager.getInstance(context) }

    var keepScreenOn by rememberSaveable {
        mutableStateOf(sharedPrefs.getBoolean("keep_screen_on", true))
    }

    var doNotDisturb by rememberSaveable {
        mutableStateOf(sharedPrefs.getBoolean(DoNotDisturbManager.DO_NOT_DISTURB_KEY, false))
    }

    val currentTheme by themeManager.currentTheme
    var hasNotificationAccess by remember {
        mutableStateOf(doNotDisturbManager.hasNotificationPolicyAccess())
    }

    // Add state for permission dialog
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Add states for about dialogs
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(backDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = doNotDisturbManager.hasNotificationPolicyAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_section_display),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.setting_keep_screen_on),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.setting_keep_screen_on_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { isChecked ->
                            keepScreenOn = isChecked
                            sharedPrefs.edit { putBoolean("keep_screen_on", isChecked) }
                        }
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                var showThemeDialog by remember { mutableStateOf(false) }

                // The main clickable row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.setting_theme),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.setting_choose_appearance_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentTheme.getDisplayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Theme selection dialog
                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text(stringResource(id = R.string.title_select_theme)) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                themeManager.setThemeMode(mode)
                                                showThemeDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mode.getDisplayName())
                                        if (currentTheme == mode) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(id = R.string.cd_theme_selected),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { }  // No confirm button needed
                    )
                }
            }

            // Sound Section - Add this right after the Display section and before Notifications
            Text(
                text = stringResource(id = R.string.settings_section_sound),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                var showSoundDialog by remember { mutableStateOf(false) }
                val soundPrefs = remember { sharedPrefs }
                val currentSoundId = remember {
                    soundPrefs.getString("meditation_sound", MeditationSounds.DEFAULT.id) ?: MeditationSounds.DEFAULT.id
                }
                var selectedSound by remember {
                    mutableStateOf(MeditationSounds.getSoundById(currentSoundId))
                }

                // The main clickable row
                Surface(
                    onClick = { showSoundDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.setting_meditation_sound),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(id = R.string.setting_meditation_sound_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedSound.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Sound selection dialog with preview capability
                if (showSoundDialog) {
                    // Remember MediaPlayer for previewing sounds
                    val mediaPlayer = remember { MediaPlayer() }

                    // Cleanup when dialog closes
                    DisposableEffect(Unit) {
                        onDispose {
                            mediaPlayer.release()
                        }
                    }

                    // Function to preview a sound
                    fun previewSound(soundResourceId: Int) {
                        try {
                            mediaPlayer.reset()
                            val descriptor = context.resources.openRawResourceFd(soundResourceId)
                            mediaPlayer.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                            descriptor.close()
                            mediaPlayer.prepare()
                            mediaPlayer.start()
                        } catch (e: Exception) {
                            // Silent error handling
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showSoundDialog = false },
                        title = { Text(stringResource(id = R.string.title_select_sound)) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MeditationSounds.allSounds.forEach { sound ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSound = sound
                                                soundPrefs.edit {
                                                    putString("meditation_sound", sound.id)
                                                }
                                                showSoundDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sound.getDisplayName(),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Play button to preview the sound
                                            IconButton(
                                                onClick = { previewSound(sound.resourceId) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = stringResource(id = R.string.cd_preview_sound),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Selected indicator
                                            if (selectedSound.id == sound.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(id = R.string.cd_sound_selected),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSoundDialog = false }) {
                                Text(stringResource(id = R.string.action_close))
                            }
                        }
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.settings_section_notifications),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.setting_do_not_disturb),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.setting_do_not_disturb_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (hasNotificationAccess) {
                        Switch(
                            checked = doNotDisturb,
                            onCheckedChange = { isChecked ->
                                doNotDisturb = isChecked
                                sharedPrefs.edit {
                                    putBoolean(
                                        DoNotDisturbManager.DO_NOT_DISTURB_KEY,
                                        isChecked
                                    )
                                }
                            }
                        )
                    } else {
                        TextButton(
                            onClick = {
                                showPermissionDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(id = R.string.action_allow))
                        }
                    }
                }
            }

            ImportExportSection(
                journalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                modifier = Modifier.padding(top = 8.dp)
            )

            // About Section
            Text(
                text = stringResource(id = R.string.settings_section_about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Source Code
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Get the URL string resource outside the lambda
                            val repoUrl = context.getString(R.string.about_source_code_full_url)
                            val intent = Intent(Intent.ACTION_VIEW, repoUrl.toUri())
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.about_source_code),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.about_source_code_display_url),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(id = R.string.cd_visit_repository),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Privacy Policy
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showPrivacyPolicyDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.about_privacy_policy),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(id = R.string.cd_view_privacy_policy),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // License
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLicenseDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.about_license),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(id = R.string.cd_view_license),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(id = R.string.title_permission_required)) },
            text = {
                Text(stringResource(id = R.string.message_dnd_permission))
            },
            confirmButton = {
                Button(
                    onClick = {
                        doNotDisturbManager.requestNotificationPolicyAccess()
                        showPermissionDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.action_grant_permission))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text(stringResource(id = R.string.title_privacy_policy)) },
            text = {
                Text(stringResource(id = R.string.privacy_policy))
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false }
                ) {
                    Text(stringResource(id = R.string.action_close))
                }
            }
        )
    }

    // License Dialog
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(stringResource(id = R.string.title_mit_license)) },
            text = {
                Box(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(stringResource(id = R.string.mit_license_text))
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLicenseDialog = false }
                ) {
                    Text(stringResource(id = R.string.action_close))
                }
            }
        )
    }
}