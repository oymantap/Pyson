package com.pyson

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pyson)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            PysonModernTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun PysonModernTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF09090B),
            surface = Color(0xFF121215),
            primary = Color(0xFF38BDF8),
            onBackground = Color(0xFFF8FAFC)
        ),
        content = content
    )
}

data class RealEditorTab(
    val file: File,
    var content: TextFieldValue
)

@Composable
fun MainAppEntry() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermission = map.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } else {
                hasPermission = true
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0.2f) }

    LaunchedEffect(Unit) {
        delay(200)
        loadingProgress = 0.6f
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        loadingProgress = 1.0f
        delay(200)
        isLoading = false
    }

    if (isLoading) {
        PysonSplashScreen(loadingProgress)
    } else {
        PysonMainWorkspace()
    }
}

@Composable
fun PysonSplashScreen(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🐍", fontSize = 72.sp)
            Text("PYSON IDE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 24.sp)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(180.dp).height(4.dp),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF27272A)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PysonMainWorkspace() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pysonDir = remember {
        val dir = File(Environment.getExternalStorageDirectory(), "PysonProjects")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val tabs = remember { mutableStateListOf<RealEditorTab>() }
    var activeTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val existingFiles = pysonDir.listFiles { _, name -> name.endsWith(".py") }
        if (existingFiles.isNullOrEmpty()) {
            val mainFile = File(pysonDir, "main.py")
            mainFile.writeText("name = input(\"Enter your name: \")\nprint(f\"Hello, {name}!\")")
            tabs.add(RealEditorTab(mainFile, TextFieldValue(mainFile.readText())))
        } else {
            existingFiles.forEach { file ->
                tabs.add(RealEditorTab(file, TextFieldValue(file.readText())))
            }
        }
    }

    var showAddFileDialog by remember { mutableStateOf(false) }
    var showTerminalCL by remember { mutableStateOf(false) }

    if (showTerminalCL) {
        TerminalCLScreen(currentDir = pysonDir, onClose = { showTerminalCL = false })
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF121215),
                drawerContentColor = Color(0xFFF8FAFC)
            ) {
                Column(modifier = Modifier.fillMaxHeight().width(280.dp).padding(16.dp)) {
                    Text("⚡ PYSON EXPLORER", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("PROJECT FILES", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    tabs.forEachIndexed { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (activeTabIndex == index) Color(0xFF1E293B) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    activeTabIndex = index
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📄 ${tab.file.name}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            IconButton(
                                onClick = {
                                    if (tab.file.exists()) tab.file.delete()
                                    tabs.removeAt(index)
                                    if (activeTabIndex >= tabs.size) activeTabIndex = maxOf(0, tabs.size - 1)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("X", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showTerminalCL = true; scope.launch { drawerState.close() } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💻 Open Terminal CL", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    ) {
        if (tabs.isNotEmpty()) {
            PysonEditorScreen(
                tabs = tabs,
                activeTabIndex = activeTabIndex,
                pysonDir = pysonDir,
                onTabSelected = { activeTabIndex = it },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onAddFileClick = { showAddFileDialog = true }
            )
        }
    }

    if (showAddFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            title = { Text("Create New Python File", color = Color.White, fontFamily = FontFamily.Monospace) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. script.py)") },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotEmpty()) {
                        val formattedName = if (newFileName.endsWith(".py")) newFileName else "$newFileName.py"
                        val newFile = File(pysonDir, formattedName)
                        if (!newFile.exists()) newFile.createNewFile()
                        tabs.add(RealEditorTab(newFile, TextFieldValue("")))
                        activeTabIndex = tabs.size - 1
                        showAddFileDialog = false
                    }
                }) { Text("Create") }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}

@Composable
fun PysonEditorScreen(
    tabs: MutableList<RealEditorTab>,
    activeTabIndex: Int,
    pysonDir: File,
    onTabSelected: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onAddFileClick: () -> Unit
) {
    val currentTab = tabs[activeTabIndex]
    var codeText by remember(activeTabIndex) { mutableStateOf(currentTab.content) }
    val scope = rememberCoroutineScope()

    var terminalOutput by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    // Channel direfresh tiap kali sesi run dimulai
    var inputChannel by remember { mutableStateOf(Channel<String>(Channel.UNLIMITED)) }
    val focusRequester = remember { FocusRequester() }
    val consoleScrollState = rememberScrollState()

    LaunchedEffect(codeText) {
        tabs[activeTabIndex].content = codeText
        currentTab.file.writeText(codeText.text)
    }

    LaunchedEffect(terminalOutput) {
        if (isRunning) {
            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
        }
    }

    fun runPythonScript() {
        isRunning = true
        terminalOutput = ">>> Running ${currentTab.file.name}...\n"
        inputChannel = Channel(Channel.UNLIMITED) // Reset channel baru untuk run session ini

        scope.launch(Dispatchers.IO) {
            try {
                val py = Python.getInstance()
                val builtins = py.getModule("builtins")

                val currentPath = pysonDir.absolutePath
                builtins.callAttr(
                    "exec",
                    """
                    import os, sys
                    os.chdir('$currentPath')
                    if '$currentPath' not in sys.path:
                        sys.path.insert(0, '$currentPath')
                    """.trimIndent()
                )

                val sys = py.getModule("sys")

                val customIO = object {
                    fun write(text: String) {
                        scope.launch(Dispatchers.Main) {
                            terminalOutput += text
                        }
                    }

                    fun flush() {}

                    fun readline(): String {
                        // Mengambil input terkirim secara synchronous untuk Python thread
                        val inputVal = kotlinx.coroutines.runBlocking { inputChannel.receive() }
                        
                        scope.launch(Dispatchers.Main) {
                            terminalOutput += "$inputVal\n"
                        }
                        
                        return inputVal + "\n"
                    }
                }

                sys.put("stdout", customIO)
                sys.put("stderr", customIO)
                sys.put("stdin", customIO)

                val globals = py.getModule("types").callAttr("ModuleType", "__main__").get("__dict__")
                builtins.callAttr("exec", codeText.text, globals)

                withContext(Dispatchers.Main) {
                    terminalOutput += "\n[Process finished with exit code 0]"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalOutput += "\n⚠ ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isRunning = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF09090B)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Header Tabs + Tombol RUN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121215))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(32.dp)) {
                    Text("☰", color = Color.White, fontSize = 18.sp)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Surface(
                            onClick = { onTabSelected(index) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeTabIndex == index) Color(0xFF0EA5E9) else Color(0xFF1E1E24)
                        ) {
                            Text(
                                text = tab.file.name,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onAddFileClick, modifier = Modifier.size(32.dp)) {
                    Text("➕", color = Color(0xFF38BDF8), fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { if (!isRunning) runPythonScript() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFF374151) else Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isRunning,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (isRunning) "⏳" else "▶ RUN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }

            // Code Editor & Fixed Line Numbers
            val scrollState = rememberScrollState()
            
            val lineCount = remember(codeText.text) {
                val count = codeText.text.count { it == '\n' } + 1
                maxOf(1, count)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF09090B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    // Column Nomor Baris
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = "$i",
                                color = Color(0xFF52525B),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Input Text Area
                    BasicTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color(0xFFF8FAFC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        keyboardOptions = KeyboardOptions(autoCorrect = false)
                    )
                }
            }

            // Symbol Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121215))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tab", ":", "(", ")", "\"", "'", "=", "+", "-", "*", "[", "]", "{", "}", "#").forEach { symbol ->
                    Surface(
                        onClick = {
                            val insert = if (symbol == "Tab") "    " else symbol
                            val sel = codeText.selection
                            val newTxt = codeText.text.substring(0, sel.start) + insert + codeText.text.substring(sel.end)
                            codeText = TextFieldValue(newTxt, androidx.compose.ui.text.TextRange(sel.start + insert.length))
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF27272A)
                    ) {
                        Text(
                            text = symbol,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Interactive Console Panel
            if (isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color(0xFF000000))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interactive Output", color = Color(0xFF10B981), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        TextButton(onClick = { isRunning = false }) {
                            Text("✕ Close", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(consoleScrollState)
                    ) {
                        Text(terminalOutput, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF18181B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("> ", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                        BasicTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            cursorBrush = SolidColor(Color(0xFF38BDF8)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                // Membersihkan newline ekstra dari keyboard HP
                                val sentText = userInput.trim()
                                userInput = ""
                                scope.launch {
                                    inputChannel.send(sentText)
                                }
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}