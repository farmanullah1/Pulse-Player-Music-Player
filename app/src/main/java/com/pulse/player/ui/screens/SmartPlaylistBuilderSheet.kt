package com.pulse.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pulse.player.library.SmartPlaylistEngine
import com.pulse.player.library.SmartPlaylistEngine.Condition
import com.pulse.player.library.SmartPlaylistEngine.Match
import com.pulse.player.library.SmartPlaylistEngine.Operator
import com.pulse.player.library.SmartPlaylistEngine.SmartField
import com.pulse.player.library.SmartPlaylistEngine.SmartRules
import com.pulse.player.ui.theme.TabularMono
import com.pulse.player.ui.viewmodel.MainViewModel
import com.pulse.player.ui.viewmodel.SmartPlaylistBuilderTarget

private data class BuilderFieldOption(
    val field: SmartField,
    val label: String,
    val isNumeric: Boolean = false,
    val isBoolean: Boolean = false,
    val isDurationSec: Boolean = false
)

private val builderFieldOptions = listOf(
    BuilderFieldOption(SmartField.GENRE, "Genre"),
    BuilderFieldOption(SmartField.CODEC, "Codec (FLAC/MP3)"),
    BuilderFieldOption(SmartField.ARTIST, "Artist"),
    BuilderFieldOption(SmartField.ALBUM, "Album"),
    BuilderFieldOption(SmartField.TITLE, "Title"),
    BuilderFieldOption(SmartField.FOLDER, "Folder Path"),
    BuilderFieldOption(SmartField.YEAR, "Release Year", isNumeric = true),
    BuilderFieldOption(SmartField.DURATION_MS, "Duration (sec)", isNumeric = true, isDurationSec = true),
    BuilderFieldOption(SmartField.PLAY_COUNT, "Play Count", isNumeric = true),
    BuilderFieldOption(SmartField.RATING, "Rating (0–5)", isNumeric = true),
    BuilderFieldOption(SmartField.IS_FAVORITE, "Favourited", isBoolean = true)
)

private fun operatorsFor(option: BuilderFieldOption): List<Operator> = when {
    option.isBoolean -> listOf(Operator.EQUALS)
    option.isNumeric -> listOf(
        Operator.EQUALS,
        Operator.NOT_EQUALS,
        Operator.GREATER_THAN,
        Operator.LESS_THAN,
        Operator.BETWEEN
    )
    else -> listOf(
        Operator.CONTAINS,
        Operator.NOT_CONTAINS,
        Operator.EQUALS,
        Operator.NOT_EQUALS,
        Operator.STARTS_WITH,
        Operator.IS_EMPTY,
        Operator.IS_NOT_EMPTY
    )
}

private fun operatorLabel(op: Operator): String = when (op) {
    Operator.EQUALS -> "Equals"
    Operator.NOT_EQUALS -> "Does Not Equal"
    Operator.CONTAINS -> "Contains"
    Operator.NOT_CONTAINS -> "Does Not Contain"
    Operator.STARTS_WITH -> "Starts With"
    Operator.GREATER_THAN -> "Greater Than"
    Operator.LESS_THAN -> "Less Than"
    Operator.BETWEEN -> "Between"
    Operator.IS_EMPTY -> "Is Empty"
    Operator.IS_NOT_EMPTY -> "Is Not Empty"
}

private fun Condition.toEditableUi(): Condition {
    if (field != SmartField.DURATION_MS) return this
    val sec1 = value.toLongOrNull()?.div(1000L)?.toString() ?: value
    val sec2 = value2.toLongOrNull()?.div(1000L)?.toString() ?: value2
    return copy(value = sec1, value2 = sec2)
}

private fun Condition.toEngineCondition(): Condition {
    if (field != SmartField.DURATION_MS) return this
    val ms1 = value.trim().toLongOrNull()?.times(1000L)?.toString() ?: ""
    val ms2 = value2.trim().toLongOrNull()?.times(1000L)?.toString() ?: ""
    return copy(value = ms1, value2 = ms2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistBuilderSheet(
    vm: MainViewModel,
    target: SmartPlaylistBuilderTarget
) {
    BackHandler { vm.closeSmartPlaylistBuilder() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = target.existing
    val engine = remember(vm.db) { SmartPlaylistEngine(vm.db) }
    val parsedExisting = remember(existing) {
        existing?.let { engine.rulesFromJson(it.rulesJson) }
    }

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var matchMode by remember(existing) { mutableStateOf(parsedExisting?.match ?: Match.ALL) }
    var conditions by remember(existing) {
        mutableStateOf(
            parsedExisting?.conditions?.map { it.toEditableUi() }?.ifEmpty { null }
                ?: listOf(Condition(SmartField.GENRE, Operator.CONTAINS, ""))
        )
    }
    var sortKey by remember(existing) { mutableStateOf(existing?.sortKey ?: "dateAdded") }
    var sortDesc by remember(existing) { mutableStateOf((existing?.sortOrder ?: -1) <= 0) }
    var limitText by remember(existing) { mutableStateOf(existing?.limitCount?.toString().orEmpty()) }
    var previewCount by remember { mutableIntStateOf(0) }

    val engineRules = remember(matchMode, conditions) {
        SmartRules(
            match = matchMode,
            conditions = conditions.map { it.toEngineCondition() }
        )
    }
    val parsedLimit = limitText.trim().toIntOrNull()?.takeIf { it > 0 }

    LaunchedEffect(engineRules, parsedLimit) {
        previewCount = runCatching {
            vm.previewSmartRuleMatchCount(engineRules, parsedLimit)
        }.getOrDefault(0)
    }

    val canSave = name.isNotBlank() && conditions.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = { vm.closeSmartPlaylistBuilder() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (existing == null) "New Smart Playlist" else "Edit Smart Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$previewCount matches",
                        style = MaterialTheme.typography.labelMedium.merge(TabularMono),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("smart_playlist_match_badge")
                    )
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                placeholder = { Text("e.g. Late Night Lossless") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_playlist_name_input")
            )

            // Match Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Match:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = matchMode == Match.ALL,
                    onClick = { matchMode = Match.ALL },
                    label = { Text("ALL rules (AND)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = matchMode == Match.ANY,
                    onClick = { matchMode = Match.ANY },
                    label = { Text("ANY rule (OR)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Rule Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                conditions.forEachIndexed { index, condition ->
                    SmartConditionCard(
                        condition = condition,
                        canRemove = conditions.size > 1,
                        onUpdate = { updated ->
                            conditions = conditions.toMutableList().apply { set(index, updated) }
                        },
                        onRemove = {
                            if (conditions.size > 1) {
                                conditions = conditions.toMutableList().apply { removeAt(index) }
                            }
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        conditions = conditions + Condition(SmartField.ARTIST, Operator.CONTAINS, "")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("smart_playlist_add_rule_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Condition Rule", fontWeight = FontWeight.SemiBold)
                }
            }

            // Sort & Limit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sort & Limit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "dateAdded" to "Date Added",
                        "title" to "Title",
                        "artist" to "Artist",
                        "playCount" to "Plays",
                        "duration" to "Duration",
                        "year" to "Year"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = sortKey == key,
                            onClick = { sortKey = key },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = sortDesc,
                        onClick = { sortDesc = !sortDesc },
                        label = { Text(if (sortDesc) "Descending" else "Ascending") }
                    )
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { raw ->
                            limitText = raw.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Max tracks (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existing != null) {
                    TextButton(
                        onClick = {
                            vm.deleteSmartPlaylist(existing.id)
                            vm.closeSmartPlaylistBuilder()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.closeSmartPlaylistBuilder() }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            vm.saveSmartPlaylist(
                                id = existing?.id,
                                name = name,
                                rules = engineRules,
                                sortKey = sortKey,
                                sortOrder = if (sortDesc) -1 else 1,
                                limitCount = parsedLimit
                            ) {
                                vm.closeSmartPlaylistBuilder()
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("smart_playlist_save_button")
                    ) {
                        Text("Save Smart Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartConditionCard(
    condition: Condition,
    canRemove: Boolean,
    onUpdate: (Condition) -> Unit,
    onRemove: () -> Unit
) {
    val fieldOption = builderFieldOptions.firstOrNull { it.field == condition.field } ?: builderFieldOptions.first()
    val validOps = remember(fieldOption) { operatorsFor(fieldOption) }
    var fieldMenuOpen by remember { mutableStateOf(false) }
    var opMenuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Field Picker
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { fieldMenuOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(fieldOption.label, modifier = Modifier.weight(1f), maxLines = 1)
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = fieldMenuOpen,
                        onDismissRequest = { fieldMenuOpen = false }
                    ) {
                        builderFieldOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    fieldMenuOpen = false
                                    val nextOps = operatorsFor(opt)
                                    val nextOp = if (condition.op in nextOps) condition.op else nextOps.first()
                                    val nextVal = when {
                                        opt.isBoolean -> if (condition.value.equals("false", true)) "false" else "true"
                                        opt.isNumeric && condition.value.toLongOrNull() == null -> ""
                                        else -> condition.value
                                    }
                                    onUpdate(Condition(opt.field, nextOp, nextVal, ""))
                                }
                            )
                        }
                    }
                }

                // Operator Picker
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { opMenuOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(operatorLabel(condition.op), modifier = Modifier.weight(1f), maxLines = 1)
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = opMenuOpen,
                        onDismissRequest = { opMenuOpen = false }
                    ) {
                        validOps.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(operatorLabel(op)) },
                                onClick = {
                                    opMenuOpen = false
                                    onUpdate(condition.copy(op = op))
                                }
                            )
                        }
                    }
                }

                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove condition",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            when {
                fieldOption.isBoolean -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = condition.value != "false",
                            onClick = { onUpdate(condition.copy(value = "true")) },
                            label = { Text("Yes (True)") }
                        )
                        FilterChip(
                            selected = condition.value == "false",
                            onClick = { onUpdate(condition.copy(value = "false")) },
                            label = { Text("No (False)") }
                        )
                    }
                }
                condition.op == Operator.IS_EMPTY || condition.op == Operator.IS_NOT_EMPTY -> {
                    // No value input needed
                }
                condition.op == Operator.BETWEEN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = condition.value,
                            onValueChange = { onUpdate(condition.copy(value = it)) },
                            label = { Text("Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = condition.value2,
                            onValueChange = { onUpdate(condition.copy(value2 = it)) },
                            label = { Text("Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = condition.value,
                        onValueChange = { onUpdate(condition.copy(value = it)) },
                        label = {
                            Text(
                                when {
                                    fieldOption.isDurationSec -> "Seconds (e.g. 240)"
                                    fieldOption.field == SmartField.RATING -> "Stars (0–5)"
                                    fieldOption.field == SmartField.YEAR -> "Year (e.g. 2020)"
                                    fieldOption.isNumeric -> "Number"
                                    else -> "Match text"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = if (fieldOption.isNumeric) {
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                        } else {
                            KeyboardOptions.Default
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
