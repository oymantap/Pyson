package com.pyson

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
import java.net.HttpURLConnection
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
    var logs by remember {
        mutableStateOf(
            listOf(
                "Pyson Shell [Version 1.0.0]",
                "Type 'help' or commands: ls, cd, mkdir, rm, pip, py install git, git..."
            )
        )
    }
    val scrollState = rememberScrollState()

    val binDir = remember { File(context.filesDir, "bin").apply { if (!exists()) mkdirs() } }
    val gitExecutable = remember { File(binDir, "git") }

    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

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

            command == "help" -> {
                logs = logs + listOf(
                    "Available commands:",
                    "  ls               - List directory files",
                    "  cd <dir>         - Change directory",
                    "  mkdir <name>     - Create folder",
                    "  rm [-rf] <target>- Delete file/folder",
                    "  pip <args>       - Chaquopy Package Manager notice",
                    "  py install git   - Download & Install standalone Git binary",
                    "  git <args>       - Execute Git CLI"
                )
            }

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

            command == "cd" -> {
                if (parts.size > 1) {
                    val targetName = parts[1]
                    val newDir = if (targetName == "..") workingDir.parentFile else File(workingDir, targetName)
                    if (newDir != null && newDir.exists() && newDir.isDirectory) {
                        workingDir = newDir
                    } else {
                        logs = logs + "Directory not found: $targetName"
                    }
                } else {
                    logs = logs + "Usage: cd <folder_name>"
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

            // FIX 1: PIP Handling - Menjelaskan limitation Chaquopy runtime
            command == "pip" -> {
                logs = logs + listOf(
                    "⚠️ Chaquopy Runtime Notice:",
                    "Runtime 'pip install' dynamic loading is restricted on Android.",
                    "To add Python packages (like 'requests'), declare them in 'build.gradle.kts':",
                    "  python { pip { install('requests') } }"
                )
            }

            // FIX 2: Download Git CLI dari mirror binary yang valid
            trimmed == "py install git" -> {
                logs = logs + "🌀 Downloading Native Git CLI Binary for Android..."
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // Binary Git ARM64 valid
                        val gitUrl = "https://raw.githubusercontent.com/termux/termux-packages/master/packages/git/build.sh" // Fallback / Direct mirror
                        val targetUrl = "https://github.com/its-pointless/its-pointless.github.io/raw/master/bin/git"

                        val url = URL(targetUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        connection.instanceFollowRedirects = true
                        connection.connect()

                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.use { input ->
                                FileOutputStream(gitExecutable).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            gitExecutable.setExecutable(true, false)
                            withContext(Dispatchers.Main) {
                                logs = logs + "✅ Native Git CLI Installed successfully!"
                                logs = logs + "Location: ${gitExecutable.absolutePath}"
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                logs = logs + "❌ HTTP Error ${connection.responseCode}: Unable to fetch binary."
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            logs = logs + "❌ Failed installing Git binary: ${e.localizedMessage}"
                        }
                    }
                }
            }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .systemBarsPadding()
            .imePadding()
    ) {
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
