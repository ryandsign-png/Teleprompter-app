package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.TeleprompterViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TeleprompterApp()
            }
        }
    }
}

@Composable
fun TeleprompterApp(
    viewModel: TeleprompterViewModel = viewModel()
) {
    val inPrompterMode by viewModel.inPrompterMode.collectAsStateWithLifecycle()

    if (inPrompterMode) {
        PrompterScreen(viewModel = viewModel)
    } else {
        DashboardScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TeleprompterViewModel) {
    val scripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val editTitle by viewModel.editTitle.collectAsStateWithLifecycle()
    val editContent by viewModel.editContent.collectAsStateWithLifecycle()
    val currentId by viewModel.currentId.collectAsStateWithLifecycle()

    val scrollSpeed by viewModel.scrollSpeed.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val isMirrored by viewModel.isMirrored.collectAsStateWithLifecycle()
    val alignment by viewModel.alignment.collectAsStateWithLifecycle()
    val countdownTime by viewModel.countdownTime.collectAsStateWithLifecycle()
    val themeName by viewModel.themeName.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Editor, 1: Daftar Skrip
    var searchQuery by remember { mutableStateOf("") }

    val filteredScripts = remember(scripts, searchQuery) {
        if (searchQuery.isBlank()) {
            scripts
        } else {
            scripts.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Prompter Mudah",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Asisten Pidato & Presentasi Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 650.dp

            if (isTablet) {
                // Wide Screen/Tablet layout: Edit Form and Scripts list side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column: Script Manager
                    Card(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Daftar Skrip Anda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Cari skrip...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (filteredScripts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Skrip tidak ditemukan",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredScripts.forEach { script ->
                                        ScriptCardItem(
                                            script = script,
                                            isActive = currentId == script.id,
                                            onSelect = { viewModel.selectScript(script) },
                                            onDelete = { viewModel.deleteScriptById(script.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right column: Detailed text Editor and Scroll speed / options tuning
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Editor Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            ScriptFormEditor(
                                viewModel = viewModel,
                                editTitle = editTitle,
                                editContent = editContent,
                                currentId = currentId
                            )
                        }

                        // Presets & Controls Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.wrapContentHeight()
                        ) {
                            ScriptQuickTuningControls(
                                viewModel = viewModel,
                                scrollSpeed = scrollSpeed,
                                fontSize = fontSize,
                                isMirrored = isMirrored,
                                alignment = alignment,
                                countdownTime = countdownTime,
                                themeName = themeName,
                                onStartPrompter = { viewModel.startPrompter() }
                            )
                        }
                    }
                }
            } else {
                // Mobile layout with Tabs
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Tulis & Edit", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Pilih Skrip (${filteredScripts.size})", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.List, contentDescription = "List") }
                        )
                    }

                    when (activeTab) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.height(300.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    ScriptFormEditor(
                                        viewModel = viewModel,
                                        editTitle = editTitle,
                                        editContent = editContent,
                                        currentId = currentId
                                    )
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.wrapContentHeight()
                                ) {
                                    ScriptQuickTuningControls(
                                        viewModel = viewModel,
                                        scrollSpeed = scrollSpeed,
                                        fontSize = fontSize,
                                        isMirrored = isMirrored,
                                        alignment = alignment,
                                        countdownTime = countdownTime,
                                        themeName = themeName,
                                        onStartPrompter = { viewModel.startPrompter() }
                                    )
                                }
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Cari skrip...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (filteredScripts.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Skrip tidak ditemukan",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        filteredScripts.forEach { script ->
                                            ScriptCardItem(
                                                script = script,
                                                isActive = currentId == script.id,
                                                onSelect = {
                                                    viewModel.selectScript(script)
                                                    activeTab = 0 // Auto focus editor / preview tab
                                                },
                                                onDelete = { viewModel.deleteScriptById(script.id) }
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
    }
}

@Composable
fun ScriptFormEditor(
    viewModel: TeleprompterViewModel,
    editTitle: String,
    editContent: String,
    currentId: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentId == null) "Skrip Baru" else "Edit Skrip",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editTitle.isNotEmpty() || editContent.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearEditor() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset")
                    }
                }
                Button(
                    onClick = { viewModel.saveScript() },
                    enabled = editTitle.isNotBlank() && editContent.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan")
                }
            }
        }

        OutlinedTextField(
            value = editTitle,
            onValueChange = { viewModel.setEditTitle(it) },
            label = { Text("Judul Pidato / Skrip") },
            placeholder = { Text("Masukkan judul pidato...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("script_title_field"),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = editContent,
            onValueChange = { viewModel.setEditContent(it) },
            label = { Text("Isi Teks Skrip") },
            placeholder = { Text("Ketik atau tempel (paste) skrip Anda di sini...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("script_content_field"),
            maxLines = Int.MAX_VALUE,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ScriptQuickTuningControls(
    viewModel: TeleprompterViewModel,
    scrollSpeed: Float,
    fontSize: Float,
    isMirrored: Boolean,
    alignment: String,
    countdownTime: Int,
    themeName: String,
    onStartPrompter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kustomisasi Kecepatan & Font",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        // SPEED tuning line and input
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Speed", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kecepatan Gulir", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "${String.format("%.1f", scrollSpeed)}x",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.adjustScrollSpeed(-0.5f) },
                    modifier = Modifier.testTag("speed_decrease_btn")
                ) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = scrollSpeed,
                    onValueChange = { viewModel.setScrollSpeed(it) },
                    valueRange = 1.0f..10.0f,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.adjustScrollSpeed(0.5f) },
                    modifier = Modifier.testTag("speed_increase_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Speed")
                }
            }
        }

        // FONT SIZE tuning line and input
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aa", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ukuran Huruf (Teks)", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "${fontSize.toInt()} sp",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.adjustFontSize(-2f) },
                    modifier = Modifier.testTag("font_decrease_btn")
                ) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = 14.0f..56.0f,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.adjustFontSize(2f) },
                    modifier = Modifier.testTag("font_increase_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                }
            }
        }

        HorizontalDivider()

        // GRID of configurations (Mirror state, Alignment, Countdown, Themes)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mirror Option Checkbox Card
                Surface(
                    onClick = { viewModel.toggleMirror() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMirrored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isMirrored) MaterialTheme.colorScheme.primary else Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isMirrored) Icons.Default.CheckCircle else Icons.Default.Settings,
                            contentDescription = "",
                            tint = if (isMirrored) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Pantul (Mirror)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Text Alignment Choice Panel
                Surface(
                    onClick = { viewModel.toggleAlignment() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (alignment == "LEFT") "Rata Kiri" else "Rata Tengah",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (alignment == "LEFT") Icons.Default.Check else Icons.Default.CheckCircle,
                            contentDescription = "",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown duration
                Text("Ketuk Mundur:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 3, 5).forEach { sec ->
                        FilterChip(
                            selected = countdownTime == sec,
                            onClick = { viewModel.setCountdownTime(sec) },
                            label = { Text(if (sec == 0) "Mulai Langsung" else "${sec} Detik") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Style theme Selection Row
                Text("Tema Baca:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val themes = listOf(
                        "YELLOW_ON_BLACK" to "Kuning/Hitam",
                        "WHITE_ON_BLACK" to "Putih/Hitam",
                        "BLACK_ON_WHITE" to "Hitam/Putih"
                    )
                    themes.forEach { (key, label) ->
                        FilterChip(
                            selected = themeName == key,
                            onClick = { viewModel.setThemeName(key) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }
        }

        Button(
            onClick = onStartPrompter,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_prompter_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Mulai Layar Teleprompter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScriptCardItem(
    script: com.example.data.Script,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                BorderStroke(
                    1.dp,
                    if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = script.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = script.content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kecepatan: ${String.format("%.1f", script.scrollSpeed)}x • Teks: ${script.fontSize.toInt()}sp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (isActive) {
                    Text(
                        text = "Sedang Diedit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PrompterScreen(viewModel: TeleprompterViewModel) {
    val editTitle by viewModel.editTitle.collectAsStateWithLifecycle()
    val editContent by viewModel.editContent.collectAsStateWithLifecycle()

    val scrollSpeed by viewModel.scrollSpeed.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val isMirrored by viewModel.isMirrored.collectAsStateWithLifecycle()
    val alignment by viewModel.alignment.collectAsStateWithLifecycle()
    val highlightActive by viewModel.highlightActive.collectAsStateWithLifecycle()
    val countdownTime by viewModel.countdownTime.collectAsStateWithLifecycle()
    val themeName by viewModel.themeName.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Control visibility of overlay panels (collapsible on screen tap)
    var showControls by remember { mutableStateOf(true) }

    // Intercept physical Back button
    BackHandler {
        viewModel.stopPrompter()
    }

    // Dynamic coloring based on choosen prompter theme name
    val backgroundColor = when (themeName) {
        "BLACK_ON_WHITE" -> Color.White
        else -> Color.Black
    }
    val textColor = when (themeName) {
        "BLACK_ON_WHITE" -> Color.Black
        "YELLOW_ON_BLACK" -> Color.Yellow
        else -> Color.White
    }

    // Pre-start countdown state management
    var secondsCountdownRemaining by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        if (countdownTime > 0) {
            secondsCountdownRemaining = countdownTime
            while (secondsCountdownRemaining > 0) {
                delay(1000L)
                secondsCountdownRemaining--
            }
            secondsCountdownRemaining = 0
            // start scroll automatically
            if (!isPlaying) {
                viewModel.togglePlayPause()
            }
        } else {
            secondsCountdownRemaining = 0
            // start scroll automatically
            if (!isPlaying) {
                viewModel.togglePlayPause()
            }
        }
    }

    // Auto-Scroll animation frame ticker
    LaunchedEffect(isPlaying, scrollSpeed, secondsCountdownRemaining) {
        // Only auto scroll if we finished countdown and user is in playing state
        if (isPlaying && secondsCountdownRemaining <= 0) {
            // base scaling: calculate step size per frame interval
            val stepSize = (scrollSpeed * 0.35f)
            try {
                while (isActive) {
                    scrollState.scroll(scrollPriority = MutatePriority.PreventUserInput) {
                        scrollBy(stepSize)
                    }
                    // Check if we hit bottom bounds, preventing instant pause if maxValue hasn't loaded (i.e. is 0)
                    if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue) {
                        viewModel.pausePrompter()
                        break
                    }
                    delay(16L) // Stable pacing around 60fps
                }
            } catch (e: Exception) {
                // Graceful cancellation handling
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("prompter_workspace")
    ) {
        var baseFontSizeFactor by remember { mutableStateOf(1f) }

        // Dynamic Text Body Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Tap anywhere around the scrolling screen to play/pause scroll
                    if (secondsCountdownRemaining <= 0) {
                        viewModel.togglePlayPause()
                    }
                }
                .pointerInput(Unit) {
                    // Support pinch-to-zoom on screen directly to adjust size
                    detectTransformGestures { _, _, zoom, _ ->
                        val newMultiplier = (baseFontSizeFactor * zoom).coerceIn(0.6f, 2.5f)
                        baseFontSizeFactor = newMultiplier
                        // Propagate sizing back to viewmodel safely
                        val targetSize = (fontSize * zoom).coerceIn(14f, 56f)
                        viewModel.setFontSize(targetSize)
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .graphicsLayer {
                        // Support high-speed clean mirror hardware rendering flip
                        if (isMirrored) {
                            scaleX = -1f
                        }
                    }
            ) {
                // Top header padding so text starts beautifully centered
                Spacer(modifier = Modifier.height(300.dp))

                Text(
                    text = editContent,
                    color = textColor,
                    fontSize = (fontSize * baseFontSizeFactor).sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (fontSize * baseFontSizeFactor * 1.5).sp,
                    textAlign = if (alignment == "CENTER") TextAlign.Center else TextAlign.Left,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )

                // Bottom footer padding so last line scrolls above screen center
                Spacer(modifier = Modifier.height(350.dp))
            }
        }

        // Horizontal Guide Markers Overlay Frame
        if (highlightActive && secondsCountdownRemaining <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.Center)
                    // High-contrast translucent highlight backdrop guide
                    .background(Color.Yellow.copy(alpha = 0.08f))
                    .border(BorderStroke(1.5.dp, Color.Yellow.copy(alpha = 0.25f)))
            ) {
                // Left marker arrow
                Text(
                    text = "►",
                    color = Color.Yellow,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                )
                // Right marker arrow
                Text(
                    text = "◄",
                    color = Color.Yellow,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                )
            }
        }

        // PRE-START COUNTDOWN OVERLAY DIALOG SCREEN
        if (secondsCountdownRemaining > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bersiap dalam...",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$secondsCountdownRemaining",
                        color = Color.Yellow,
                        fontSize = 110.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Button(
                        onClick = { secondsCountdownRemaining = 0; viewModel.togglePlayPause() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Lewati Countdown")
                    }
                }
            }
        }

        // FLOATING RESPONSIVE ADJUSTMENTS CONTROL DRAWER
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                .wrapContentSize()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEC1A1A1A) // high contrast dark backdrop
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier.widthIn(max = 620.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicator details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Layar Teleprompter",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = editTitle,
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Guide highlight toggle
                            IconButton(
                                onClick = { viewModel.toggleHighlight() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (highlightActive) Color.Yellow else Color.White
                                )
                            ) {
                                Text(if (highlightActive) "Guide On" else "Guide Off", color = if (highlightActive) Color.Yellow else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Alignment Toggle
                            IconButton(
                                onClick = { viewModel.toggleAlignment() },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                            ) {
                                Text(if (alignment == "LEFT") "Kiri" else "Tengah", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Mirror toggle button
                            IconButton(
                                onClick = { viewModel.toggleMirror() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (isMirrored) Color.Cyan else Color.White
                                )
                            ) {
                                Text(if (isMirrored) "Mirror" else "Normal", color = if (isMirrored) Color.Cyan else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // TUNE SPEED & FONT CONTROLLERS (MUDAH / EASY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick Speed Control Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Kecepatan", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.adjustScrollSpeed(-0.5f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${String.format("%.1f", scrollSpeed)}x",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                IconButton(
                                    onClick = { viewModel.adjustScrollSpeed(0.5f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White)
                                }
                            }
                        }

                        // Quick Font Size Control Box
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Ukuran Huruf", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.adjustFontSize(-2f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${fontSize.toInt()}sp",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                IconButton(
                                    onClick = { viewModel.adjustFontSize(2f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray)

                    // CORE GAME CONTROL PLAYBACK BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Re-scroll / Rewind Button
                        IconButton(
                            onClick = {
                                scope.launch { scrollState.scrollTo(0) }
                                viewModel.pausePrompter()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        // Play/Pause central round bubble button
                        Button(
                            onClick = { viewModel.togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color.Yellow else Color.White,
                                contentColor = Color.Black
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isPlaying) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(5.dp).height(20.dp).background(Color.Black))
                                    Box(modifier = Modifier.width(5.dp).height(20.dp).background(Color.Black))
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Close Exit Screen Button
                        IconButton(
                            onClick = { viewModel.stopPrompter() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = Color.Red, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Subtle bottom tag showing how to trigger HUD
        IconButton(
            onClick = { showControls = !showControls },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (showControls) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                contentDescription = "Toggle HUD",
                tint = Color.White
            )
        }
    }
}
