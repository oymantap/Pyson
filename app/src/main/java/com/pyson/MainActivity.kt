package com.pyson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inisialisasi Chaquopy Engine
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            PysonTheme {
                PysonEditorScreen()
            }
        }
    }
}

@Composable
fun PysonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF1E1E1E),
            surface = Color(0xFF252526),
            primary = Color(0xFF007ACC),
            onBackground = Color(0xFFD4D4D4)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(codeText.text) {
        // C++ Native Engine buat kencengin analisis teks
        engineStats = NativeEngine.analyzeCodeFast(codeText.text)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pyson Code Editor", fontFamily = FontFamily.Monospace, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isRunning = true
                    try {
                        val py = Python.getInstance()
                        val sys = py.getModule("sys")
                        val io = py.getModule("io")

                        // Redirect print output Python ke string buffer
                        val stringOutput = io.callAttr("StringIO")
                        sys.put("stdout", stringOutput)
                        sys.put("stderr", stringOutput)

                        // Eksekusi kode Python dari editor
                        py.getModule("__main__").callAttr("exec", codeText.text)

                        // Ambil hasilnya
                        outputText = stringOutput.callAttr("getvalue").toString()
                    } catch (e: Exception) {
                        outputText = "Error:\n${e.localizedMessage}"
                    }
                },
                containerColor = Color(0xFF4EC9B0)
            ) {
                Text("▶ RUN", color = Color.Black, fontFamily = FontFamily.Monospace)
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF007ACC)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("C++ Engine: $engineStats", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Python 3.10", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        containerColor = Color(0xFF1E1E1E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Toolbar Symbol Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252526))
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tab", ":", "(", ")", "\"", "'", "=", "+", "-", "*", "[", "]", "{", "}").forEach { symbol ->
                    Button(
                        onClick = {
                            val insert = if (symbol == "Tab") "    " else symbol
                            val newText = codeText.text.substring(0, codeText.selection.start) + insert + codeText.text.substring(codeText.selection.end)
                            codeText = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(codeText.selection.start + insert.length))
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(symbol, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Editor Area (Top Half)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Line Numbers
                val lineCount = codeText.text.lines().size
                Column(
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E))
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            color = Color(0xFF858585),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Main Text Input
                BasicTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    textStyle = TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            // Terminal Console Output (Bottom Half - Tampil saat di-RUN)
            if (isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF0F0F0F))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Terminal Output:", color = Color(0xFF4EC9B0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        TextButton(onClick = { isRunning = false }) {
                            Text("Close", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (outputText.isEmpty()) "Executed with no output." else outputText,
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
