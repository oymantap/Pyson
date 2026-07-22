package com.pyson

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun TerminalCLScreen(
    currentDir: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var workingDir by remember { mutableStateOf(currentDir) }
    var commandInput by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf(listOf("Pyson Shell [Version 1.0.0]", "Type 'help' or commands: ls, mkdir, rm, pip, py install git, git...")) }
    val scrollState = rememberScrollState()

    // Folder tempat simpan binary CLI git internal
    val binDir = remember { File(context.filesDir, "bin").apply { if (!exists()) mkdirs() } }
    val gitExecutable = remember { File(binDir, "git") }

    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Fungsi buat nge-run perintah System Executable Native (Git CLI Asli)
    fun runNativeCommand(cmdParts: List<String>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder(cmdParts)
                    .directory(workingDir)
                    .redirectErrorStream(true)

                val env = processBuilder.environment()
                env["PATH"] = "${binDir.absolutePath}:" + (env["PATH"] ?: "")
                env["HOME"] = context.filesDir.absolutePath

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                withContext(Dispatchers.Main) {
                    logs = logs + if (output.isEmpty()) "Command executed." else output
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs = logs + "❌ Execution Error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun executeCommand(cmdStr: String) {
        val trimmed = cmdStr.trim()
        if (trimmed.isEmpty()) return

        logs = logs + "$ ${workingDir.name} > $trimmed"
        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0]

        when {
            command == "clear" -> logs = emptyList()
            
            command == "ls" -> {
                val files = workingDir.listFiles()
                if (files.isNullOrEmpty()) {
                    logs = logs + "Directory is empty."
                } else {
                    val listStr = files.joinToString("\n") { file ->
                        if (file.isDirectory) "📁 ${file.name}/" else "📄 ${file.name} (${file.length()} B)"
                    }
                    logs = logs + listStr
                }
            }
            
            command == "mkdir" -> {
                if (parts.size > 1) {
                    val newDir = File(workingDir, parts[1])
                    if (newDir.mkdir()) logs = logs + "Created directory: ${parts[1]}"
                    else logs = logs + "Failed to create directory."
                } else logs = logs + "Usage: mkdir <folder_name>"
            }
            
            command == "rm" -> {
                if (parts.size > 1) {
                    val isRecursive = parts.contains("-rf") || parts.contains("-r")
                    val targetName = parts.last()
                    val target = File(workingDir, targetName)
                    if (target.exists()) {
                        val deleted = if (isRecursive) target.deleteRecursively() else target.delete()
                        if (deleted) logs = logs + "Deleted: $targetName"
                        else logs = logs + "Failed to delete: $targetName"
                    } else logs = logs + "File/Directory not found: $targetName"
                } else logs = logs + "Usage: rm [-rf] <target>"
            }
            
            // FIX: PIP Handling via Chaquopy Launcher
            command == "pip" -> {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val py = Python.getInstance()
                        // Menggunakan pip internal installer Chaquopy / python module runner
                        val sys = py.getModule("sys")
                        val args = parts.drop(1).toTypedArray()
                        
                        // Panggil pip module via runtime runner
                        val pipModule = py.getModule("pip._internal.cli.main")
                        val code = pipModule.callAttr("main", args).toInt()
                        
                        withContext(Dispatchers.Main) {
                            logs = logs + "pip finished with exit code: $code"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            logs = logs + "pip error: ${e.localizedMessage}\n(Note: Standalone PIP install at runtime requires build-time wheel config or Chaquopy target)"
                        }
                    }
                }
            }
            
            // FIX: Pure Kotlin Download untuk Native Git CLI (Bebas SystemError)
            trimmed.startsWith("py install git") -> {
                logs = logs + "🌀 Downloading Native Git CLI Binary for Android ARM64..."
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val gitUrl = "https://raw.githubusercontent.com/its-pointless/its-pointless.github.io/master/bin/git"
                        
                        // Download lewat Kotlin I/O Stream
                        URL(gitUrl).openStream().use { input ->
                            FileOutputStream(gitExecutable).use { output ->
                                input.copyTo(output)
                            }
                        }

                        // Beri izin akses Eksekusi (chmod +x / 0755)
                        gitExecutable.setExecutable(true, false)

                        withContext(Dispatchers.Main) {
                            logs = logs + "✅ Native Git CLI Installed successfully!"
                            logs = logs + "Location: ${gitExecutable.absolutePath}"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            logs = logs + "❌ Failed installing Git binary: ${e.localizedMessage}"
                        }
                    }
                }
            }

            // Panggilan Perintah Git Native (clone, push, pull, remote, init, dll)
            command == "git" -> {
                if (!gitExecutable.exists()) {
                    logs = logs + "❌ Git is not installed yet. Type 'py install git' first!"
                } else {
                    val fullCmd = mutableListOf(gitExecutable.absolutePath)
                    fullCmd.addAll(parts.drop(1))
                    runNativeCommand(fullCmd)
                }
            }
            
            else -> logs = logs + "Command not recognized: $command"
        }
    }

    // Tambahan imePadding() & systemBarsPadding() biar terdorong keyboard HP
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .systemBarsPadding()
            .imePadding() 
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121215))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💻 Pyson Terminal CL", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.Gray, fontSize = 16.sp)
            }
        }

        // Terminal Content Logs
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            logs.forEach { log ->
                Text(
                    text = log,
                    color = if (log.startsWith("$")) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // Input Line (Auto terangkat saat keyboard muncul)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121215))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", color = Color(0xFF10B981), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    executeCommand(commandInput)
                    commandInput = ""
                }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
