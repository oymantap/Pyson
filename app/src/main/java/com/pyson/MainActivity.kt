package com.pyson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide Status Bar & Navigation Bar (Full Immersive Dark Mode)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Inisialisasi Chaquopy Engine
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            PysonModernTheme {
                PysonEditorScreen()
            }
        }
    }
}

@Composable
fun PysonModernTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            primary = Color(0xFF3B82F6),
            onBackground = Color(0xFFE0E0E0)
        ),
        content = content
    )
}

@Composable
fun PysonEditorScreen() {
    val context = LocalContext.current
    var codeText by remember { 
        mutableStateOf(TextFieldValue("def main():\n    print(\"Hello from Pyson!\")\n    print(\"Result:\", 10 * 5)\n\nmain()")) 
    }
    var engineStats by remember { mutableStateOf("Ready") }
    
    // State Terminal
    var outputText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(codeText.text) {
        engineStats = NativeEngine.analyzeCodeFast(codeText.text)
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isRunning = true
                    isError = false
                    try {
                        val py = Python.getInstance()
                        val sys = py.getModule("sys")
                        val io = py.getModule("io")

                        val stringOutput = io.callAttr("StringIO")
                        sys.put("stdout", stringOutput)
                        sys.put("stderr", stringOutput)

                        val builtins = py.getModule("builtins")
                        val globals = py.getModule("types").callAttr("ModuleType", "user_script").get("__dict__")

                        builtins.callAttr("exec", codeText.text, globals)

                        val result = stringOutput.callAttr("getvalue").toString()
                        outputText = if (result.isEmpty()) "Process finished with exit code 0." else result
                    } catch (e: Exception) {
                        isError = true
                        outputText = e.localizedMessage ?: "Unknown Error occurred"
                    }
                },
                containerColor = Color(0xFF10B981), // Modern Emerald Green
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("▶", fontSize = 14.sp)
                    Text("RUN", fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
            }
        },
        bottomBar = {
            // Modern Bottom Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E1E)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                        Text("C++ Engine: $engineStats", color = Color(0xFFA0A0A0), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("Python 3.10", color = Color(0xFF60A5FA), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Minimalist Header Bar (Ganti TopAppBar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🐍", fontSize = 16.sp)
                    Text("main.py", color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Pyson IDE",
                        color = Color(0xFF888888),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Code Symbols Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tab", ":", "(", ")", "\"", "'", "=", "+", "-", "*", "[", "]", "{", "}", "#").forEach { symbol ->
                    Surface(
                        onClick = {
                            val insert = if (symbol == "Tab") "    " else symbol
                            val newText = codeText.text.substring(0, codeText.selection.start) + insert + codeText.text.substring(codeText.selection.end)
                            codeText = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(codeText.selection.start + insert.length))
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2D2D2D)
                    ) {
                        Text(
                            text = symbol,
                            color = Color(0xFFD4D4D4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Editor Area (Line numbers & Code Input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF121212))
                    .verticalScroll(rememberScrollState())
            ) {
                val lineCount = codeText.text.lines().size
                Column(
                    modifier = Modifier
                        .background(Color(0xFF121212))
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp)
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            color = Color(0xFF4A4A4A),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                BasicTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    textStyle = TextStyle(
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            // Terminal Console Output (Slide up Panel)
            if (isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFF0D0D0D))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isError) Color(0xFFEF4444) else Color(0xFF10B981), CircleShape)
                            )
                            Text("Terminal", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        TextButton(onClick = { isRunning = false }) {
                            Text("✕ Close", color = Color(0xFF6B7280), fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = outputText,
                            color = if (isError) Color(0xFFF87171) else Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
