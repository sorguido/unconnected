package it.sorguido.unconnected

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.sorguido.unconnected.domain.NumberRule
import it.sorguido.unconnected.domain.Rules
import it.sorguido.unconnected.domain.Settings
import it.sorguido.unconnected.domain.TimeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var resumeVersion by mutableIntStateOf(0)
    private var foreground by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        val locale = if (config.locales[0].language == "it") Locale.ITALIAN else Locale.ENGLISH
        config.setLocales(LocaleList(locale))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UnconnectedTheme { Home(resumeVersion, foreground) } }
    }

    override fun onResume() {
        super.onResume()
        resumeVersion++
        foreground = true
    }

    override fun onPause() {
        foreground = false
        super.onPause()
    }
}

@Composable
private fun UnconnectedTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF9EDBC4), onPrimary = Color(0xFF003829),
        secondary = Color(0xFFB8CCC2), background = Color(0xFF101512),
        surface = Color(0xFF151C18), surfaceContainer = Color(0xFF1C2520)
    ) else lightColorScheme(
        primary = Color(0xFF246B53), onPrimary = Color.White,
        secondary = Color(0xFF52685C), background = Color(0xFFF5F7F1),
        surface = Color(0xFFFBFDF7), surfaceContainer = Color(0xFFEBF1E8)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Home(resumeVersion: Int, foreground: Boolean) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    var settings by remember { mutableStateOf<Settings?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settings = withContext(Dispatchers.IO) { store.load() }
        loaded = true
    }
    var busy by remember { mutableStateOf(false) }
    val sessionPaused by SettingsStore.sessionPauseFlow.collectAsState()
    var message by remember { mutableStateOf<Int?>(null) }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var numberEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNumberEditor by rememberSaveable { mutableStateOf(false) }
    var windowEditor by remember { mutableStateOf<TimeWindow?>(null) }
    var showWindowEditor by remember { mutableStateOf(false) }
    var deleteNumber by remember { mutableStateOf<NumberRule?>(null) }
    var deleteWindow by remember { mutableStateOf<TimeWindow?>(null) }
    var emptyCandidate by remember { mutableStateOf<Settings?>(null) }
    var resetConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val role = remember(resumeVersion, permissionVersion) { ScreeningAccess.hasRole(context) }
    val contacts = remember(resumeVersion, permissionVersion) { ScreeningAccess.hasContacts(context) }
    val roleAvailable = remember(resumeVersion) { ScreeningAccess.roleAvailable(context) }
    val permissionsReady = role && contacts
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    // A foreground-only clock refreshes the summary; screening never depends on it.
    LaunchedEffect(foreground, resumeVersion) {
        if (foreground) while (true) {
            now = ZonedDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val locale = displayLocale()
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissionVersion++ }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionVersion++ }

    fun persist(value: Settings) {
        if (busy) return
        SettingsStore.beginUpdate()
        busy = true
        message = null
        scope.launch {
            val saved = withContext(Dispatchers.IO) { store.save(value) }
            settings = if (saved) value else withContext(Dispatchers.IO) { store.load() }
            if (!saved) message = R.string.save_failed
            busy = false
        }
    }
    fun requestSave(value: Settings) {
        if (value.windows.isEmpty() && !value.emptyScheduleConfirmed) emptyCandidate = value
        else persist(value.copy(emptyScheduleConfirmed = value.windows.isEmpty() && value.emptyScheduleConfirmed))
    }
    fun openSettings() {
        try {
            context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
        } catch (_: RuntimeException) { message = R.string.settings_action_failed }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                val current = settings
                if (!loaded) {
                    Text(stringResource(R.string.loading))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else if (current == null) {
                    Section(stringResource(R.string.corrupt_title)) {
                        Text(stringResource(R.string.corrupt_detail))
                        OutlinedButton(onClick = { resetConfirmation = true }, enabled = !busy) { Text(stringResource(R.string.reset)) }
                    }
                } else {
                    val enabled = current.enabled
                    // One observable sample supplies both heading and explanation, including
                    // when this composition starts while another save is still finishing.
                    val status = filterStatus(enabled, permissionsReady, busy, sessionPaused)
                    Section(stringResource(status.title)) {
                        Text(stringResource(status.detail))
                        Button(
                            onClick = { if (enabled) persist(current.copy(enabled = false)) else requestSave(current.copy(enabled = true)) },
                            enabled = !busy && (enabled || permissionsReady),
                            colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFFB3261E) else Color(0xFF17613D), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            contentPadding = PaddingValues(18.dp)
                        ) { Text(stringResource(if (enabled) R.string.deactivate else R.string.activate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    }
                    Section(stringResource(R.string.setup)) {
                        Text(stringResource(R.string.setup_detail), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(if (role) R.string.role_granted else R.string.role_missing), fontWeight = FontWeight.Medium)
                        if (!role) {
                            if (roleAvailable) OutlinedButton(onClick = {
                                try {
                                    roleLauncher.launch(context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                                } catch (_: RuntimeException) { message = R.string.settings_action_failed }
                            }) { Text(stringResource(R.string.choose_role)) }
                            else Text(stringResource(R.string.role_unavailable), color = MaterialTheme.colorScheme.error)
                        }
                        Text(stringResource(if (contacts) R.string.contacts_granted else R.string.contacts_missing), fontWeight = FontWeight.Medium)
                        if (!contacts) {
                            OutlinedButton(onClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }) { Text(stringResource(R.string.grant_contacts)) }
                            Text(stringResource(R.string.permission_help), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { openSettings() }) { Text(stringResource(R.string.permission_settings)) }
                        }
                    }
                    Section(stringResource(R.string.numbers_title)) {
                        Text(stringResource(R.string.numbers_detail), style = MaterialTheme.typography.bodyMedium)
                        if (current.numbers.isEmpty()) Text(stringResource(R.string.numbers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        current.numbers.forEach { number ->
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (number.label.isNotBlank()) Text(number.label, style = MaterialTheme.typography.titleMedium)
                                    Text(number.e164, style = MaterialTheme.typography.bodyLarge)
                                }
                                val toggleDescription = stringResource(R.string.number_enabled, number.label.ifBlank { number.e164 })
                                Switch(checked = number.enabled, onCheckedChange = { checked ->
                                    requestSave(current.copy(numbers = current.numbers.map { if (it.id == number.id) it.copy(enabled = checked) else it }))
                                }, enabled = !busy, modifier = Modifier.semantics { contentDescription = toggleDescription })
                            }
                            FlowRow {
                                TextButton(onClick = { numberEditorId = number.id; showNumberEditor = true }, enabled = !busy) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { deleteNumber = number }, enabled = !busy) { Text(stringResource(R.string.delete)) }
                            }
                        }
                        OutlinedButton(onClick = { numberEditorId = null; showNumberEditor = true }, enabled = !busy && current.numbers.size < 200) { Text(stringResource(R.string.add_number)) }
                        if (current.numbers.size >= 200) Text(stringResource(R.string.entry_limit, 200))
                    }
                    Section(stringResource(R.string.schedule_title)) {
                        Text(stringResource(R.string.schedule_detail, now.zone.id), style = MaterialTheme.typography.bodyMedium)
                        val next = Rules.nextAllowed(current.windows, now)
                        if (current.windows.isEmpty()) Text(stringResource(R.string.schedule_empty), color = MaterialTheme.colorScheme.error)
                        else if (next != null) {
                            val formatter = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context.applicationContext)) "EEE d MMM, HH:mm" else "EEE d MMM, h:mm a", locale)
                            val until = Rules.allowedUntil(current.windows, next)
                            Text(
                                if (!next.isAfter(now)) {
                                    if (until == null) stringResource(R.string.allowed_continuously)
                                    else stringResource(R.string.allowed_until, until.format(formatter))
                                } else {
                                    if (until == null) stringResource(R.string.next_continuous, next.format(formatter))
                                    else stringResource(R.string.next_interval, next.format(formatter), until.format(formatter))
                                },
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary
                            )
                        }
                        current.windows.forEach { window ->
                            HorizontalDivider()
                            Text(window.label.ifBlank { stringResource(R.string.default_window) }, style = MaterialTheme.typography.titleMedium)
                            Text(window.days.sorted().joinToString(" · ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) })
                            Text(stringResource(R.string.window_time, timeText(window.startMinute), timeText(window.endMinute)), style = MaterialTheme.typography.headlineSmall)
                            if (window.endMinute < window.startMinute) Text(stringResource(R.string.overnight), style = MaterialTheme.typography.bodySmall)
                            FlowRow {
                                TextButton(onClick = { windowEditor = window; showWindowEditor = true }, enabled = !busy) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { deleteWindow = window }, enabled = !busy) { Text(stringResource(R.string.delete)) }
                            }
                        }
                        OutlinedButton(onClick = { windowEditor = null; showWindowEditor = true }, enabled = !busy && current.windows.size < 100) { Text(stringResource(R.string.add_window)) }
                        if (current.windows.size >= 100) Text(stringResource(R.string.entry_limit, 100))
                    }
                }
                Section(stringResource(R.string.limits_title)) { Text(stringResource(R.string.limits_detail), style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (showNumberEditor && settings != null) {
        val originalNumber = settings!!.numbers.find { it.id == numberEditorId }
        if (numberEditorId == null || originalNumber != null) NumberEditor(originalNumber, settings!!, onDismiss = { showNumberEditor = false }) { number, region ->
            val current = settings!!
            val numbers = if (numberEditorId == null) current.numbers + number else current.numbers.map { if (it.id == number.id) number else it }
            requestSave(current.copy(numbers = numbers, region = region))
            showNumberEditor = false
        }
    }
    if (showWindowEditor && settings != null) {
        WindowEditor(windowEditor, onDismiss = { showWindowEditor = false }) { window ->
            val current = settings!!
            val windows = if (windowEditor == null) current.windows + window else current.windows.map { if (it.id == window.id) window else it }
            requestSave(current.copy(windows = windows, emptyScheduleConfirmed = false))
            showWindowEditor = false
        }
    }
    deleteNumber?.let { number ->
        Confirmation(R.string.confirm_delete, R.string.delete_number_detail, R.string.delete, { deleteNumber = null }) {
            settings?.let { requestSave(it.copy(numbers = it.numbers.filterNot { entry -> entry.id == number.id })) }
            deleteNumber = null
        }
    }
    deleteWindow?.let { window ->
        Confirmation(R.string.confirm_delete, R.string.delete_window_detail, R.string.delete, { deleteWindow = null }) {
            settings?.let { requestSave(it.copy(windows = it.windows.filterNot { entry -> entry.id == window.id }, emptyScheduleConfirmed = false)) }
            deleteWindow = null
        }
    }
    emptyCandidate?.let { candidate ->
        Confirmation(R.string.empty_confirm_title, R.string.empty_confirm_detail, R.string.empty_confirm, { emptyCandidate = null }) {
            persist(candidate.copy(emptyScheduleConfirmed = true))
            emptyCandidate = null
        }
    }
    if (resetConfirmation) Confirmation(R.string.reset_title, R.string.reset_detail, R.string.reset, { resetConfirmation = false }) {
        persist(Settings())
        resetConfirmation = false
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun Confirmation(title: Int, body: Int, confirm: Int, dismiss: () -> Unit, accept: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(title)) }, text = { Text(stringResource(body)) },
        confirmButton = { TextButton(onClick = accept) { Text(stringResource(confirm)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun NumberEditor(original: NumberRule?, settings: Settings, onDismiss: () -> Unit, onSave: (NumberRule, String) -> Unit) {
    var label by rememberSaveable { mutableStateOf(original?.label ?: "") }
    var raw by rememberSaveable { mutableStateOf(original?.e164 ?: "") }
    var region by rememberSaveable { mutableStateOf(settings.region) }
    var error by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    var pendingContactUri by rememberSaveable { mutableStateOf<String?>(null) }
    var contactChoices by remember { mutableStateOf<ContactPickResult.Choices?>(null) }
    val loadingContact = pendingContactUri != null && contactChoices == null
    fun acceptContact(result: ContactPickResult) {
        val draft = applyContactPick(NumberDraft(label, raw), result)
        label = draft.label
        raw = draft.number
        error = validateNumberEntry(raw, region, settings.numbers, original?.id).error
        contactChoices = null
        pendingContactUri = null
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) pendingContactUri = result.data?.data?.toString() ?: ""
        // Cancel (including an empty native picker) leaves the existing draft untouched.
    }
    LaunchedEffect(pendingContactUri) {
        val uri = pendingContactUri ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            readContactPick(Activity.RESULT_OK, uri, region, ScreeningAccess.hasContacts(context)) { selectedUri, projection, selection, args ->
                context.contentResolver.query(selectedUri.toUri(), projection, selection, args, null)
            }
        }
        when (result) {
            is ContactPickResult.Selected -> acceptContact(result)
            is ContactPickResult.Choices -> contactChoices = result // Keep URI for safe recreation/re-query.
            else -> {
                error = when (result) {
                    ContactPickResult.PermissionDenied -> R.string.contact_permission_needed
                    ContactPickResult.Empty -> R.string.contact_no_numbers
                    ContactPickResult.InvalidNumbers -> R.string.contact_invalid_numbers
                    ContactPickResult.TooMany -> R.string.contact_too_many
                    ContactPickResult.Cancelled -> error
                    else -> R.string.contact_pick_failed
                }
                pendingContactUri = null
            }
        }
    }
    contactChoices?.let { choices ->
        AlertDialog(
            onDismissRequest = { contactChoices = null; pendingContactUri = null },
            title = { Text(stringResource(R.string.contact_choose_number)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    choices.name?.let { Text(it) }
                    Text(stringResource(R.string.contact_number_help))
                    if (choices.hasUnusableNumbers) Text(stringResource(R.string.contact_some_invalid), color = MaterialTheme.colorScheme.error)
                    choices.numbers.forEach { number ->
                        OutlinedButton(onClick = { acceptContact(chooseContactNumber(choices, number.e164)) }, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(number.e164)
                                val labels = number.labels.map { Phone.getTypeLabel(context.resources, it.type, it.custom).toString() }.distinct()
                                Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { contactChoices = null; pendingContactUri = null }) { Text(stringResource(R.string.cancel)) } },
        )
        return
    }
    val locale = displayLocale()
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(if (original == null) R.string.add_number else R.string.edit_number)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (original == null) {
                    OutlinedButton(onClick = {
                        if (!ScreeningAccess.hasContacts(context)) error = R.string.contact_permission_needed
                        else try { contactPicker.launch(contactPickerIntent()) }
                        catch (_: RuntimeException) { error = R.string.contact_picker_unavailable }
                    }, enabled = !loadingContact, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.choose_contact)) }
                    Text(stringResource(R.string.contact_pick_help), style = MaterialTheme.typography.bodySmall)
                }
                if (loadingContact) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.contact_loading))
                }
                OutlinedTextField(label, { label = it.take(80) }, enabled = !loadingContact, label = { Text(stringResource(R.string.number_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(raw, { raw = it.take(80); error = null }, enabled = !loadingContact, label = { Text(stringResource(R.string.phone_number)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
                OutlinedTextField(region, { region = it.filter(Char::isLetter).take(2).uppercase(Locale.ROOT); error = null }, enabled = !loadingContact, label = { Text(stringResource(R.string.region)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (region in Locale.getISOCountries()) Text(stringResource(R.string.region_selected, Locale.Builder().setRegion(region).build().getDisplayCountry(locale), region), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.region_help), style = MaterialTheme.typography.bodySmall)
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !loadingContact, onClick = {
            error = saveNumberEntry(NumberDraft(label, raw), region, settings.numbers, original, onSave)
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WindowEditor(original: TimeWindow?, onDismiss: () -> Unit, onSave: (TimeWindow) -> Unit) {
    var label by rememberSaveable { mutableStateOf(original?.label ?: "") }
    var days by remember { mutableStateOf(original?.days ?: setOf(1, 2, 3, 4, 5)) }
    var start by rememberSaveable { mutableIntStateOf(original?.startMinute ?: 510) }
    var end by rememberSaveable { mutableIntStateOf(original?.endMinute ?: 1050) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = displayLocale()
    fun pickTime(value: Int, accept: (Int) -> Unit) {
        TimePickerDialog(context, { _, hour, minute -> accept(hour * 60 + minute) }, value / 60, value % 60, DateFormat.is24HourFormat(context.applicationContext)).show()
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(if (original == null) R.string.add_window else R.string.edit_window)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(label, { label = it.take(80) }, label = { Text(stringResource(R.string.window_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.window_days), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        val fullName = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, locale)
                        FilterChip(selected = day in days, onClick = { days = if (day in days) days - day else days + day; error = false },
                            label = { Text(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale)) },
                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = fullName })
                    }
                }
                OutlinedButton(onClick = { pickTime(start) { start = it; error = false } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_time, timeText(start))) }
                OutlinedButton(onClick = { pickTime(end) { end = it; error = false } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.end_time, timeText(end))) }
                if (end < start) Text(stringResource(R.string.overnight), color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.boundary_help), style = MaterialTheme.typography.bodySmall)
                if (error) Text(stringResource(R.string.window_invalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (days.isEmpty() || start == end) error = true
            else onSave(TimeWindow(original?.id ?: UUID.randomUUID().toString(), label.trim(), days, start, end))
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun displayLocale(): Locale = if (LocalContext.current.resources.configuration.locales[0].language == "it") Locale.ITALIAN else Locale.ENGLISH

@Composable
private fun timeText(minute: Int): String = LocalTime.of(minute / 60, minute % 60).format(
    DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(LocalContext.current.applicationContext)) "HH:mm" else "h:mm a", displayLocale())
)
