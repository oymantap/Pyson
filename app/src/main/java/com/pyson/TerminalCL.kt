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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
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
                "Pyson Shell [Version 1.2.0]",
                "Type 'help' or commands: ls, cd, python <file.py>, pip install <pkg>, git..."
            )
        )
    }
    val scrollState = rememberScrollState()

    val binDir = remember { File(context.filesDir, "bin").apply { if (!exists()) mkdirs() } }
    val gitExecutable = remember { File(binDir, "git") }

    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Runner khusus file Python via Terminal (support 'python file.py' atau 'py file.py')
    fun runPythonScriptFile(fileName: String) {
        val pyFile = File(workingDir, fileName)
        if (!pyFile.exists() || !pyFile.isFile) {
            logs = logs + "❌ File not found: $fileName"
            return
        }

        logs = logs + "🚀 Executing ${pyFile.name}..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val py = Python.getInstance()
                val code = pyFile.readText()

                // Capture Output Stdout/Stderr
                val outputStream = ByteArrayOutputStream()
                val printStream = PrintStream(outputStream)

                val oldOut = System.out
                val oldErr = System.err
                System.setOut(printStream)
                System.setErr(printStream)

                // Set sys.path biar module lokal di folder sama bisa di-import
                val sys = py.getModule("sys")
                val pathList = sys.get("path")
                val currentPath = workingDir.absolutePath
                py.getModule("builtins").callAttr("exec", "import sys; sys.path.insert(0, '$currentPath')")

                // Eksekusi kode
                val globals = py.getModule("types").callAttr("ModuleType", "__main__").get("__dict__")
                py.getModule("builtins").callAttr("exec", code, globals)

                System.setOut(oldOut)
                System.setErr(oldErr)

                val res = outputStream.toString()
                withContext(Dispatchers.Main) {
                    logs = logs + if (res.isEmpty()) "[Process finished with no output]" else res
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs = logs + "❌ Script Execution Error: ${e.localizedMessage}"
                }
            }
        }
    }

    // Perintah Native System (Git CLI)
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

    // Fungsi PIP Installer Runtime via PyPI API Stream
    fun runPipInstall(packageName: String) {
        logs = logs + "🌀 Installing '$packageName' via PyPI Direct Downloader..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val py = Python.getInstance()
                val script = """
import sys, os, urllib.request, json, zipfile, tarfile

pkg = "$packageName"
url = f"https://pypi.org/pypi/{pkg}/json"

try:
    req = urllib.request.urlopen(url)
    data = json.loads(req.read().decode())
    urls = data['urls']
    
    download_url = None
    filename = ""
    for u in urls:
        if u['filename'].endswith('.whl') and 'py3-none-any' in u['filename']:
            download_url = u['url']
            filename = u['filename']
            break
            
    if not download_url and len(urls) > 0:
        download_url = urls[-1]['url']
        filename = urls[-1]['filename']

    if download_url:
        site_packages = [p for p in sys.path if 'site-packages' in p or 'files' in p][0]
        dest_file = os.path.join(site_packages, filename)
        
        urllib.request.urlretrieve(download_url, dest_file)
        
        if filename.endswith('.whl') or filename.endswith('.zip'):
            with zipfile.ZipFile(dest_file, 'r') as zip_ref:
                zip_ref.extractall(site_packages)
        elif filename.endswith('.tar.gz') or filename.endswith('.tgz'):
            with tarfile.open(dest_file, 'r:gz') as tar_ref:
                tar_ref.extractall(site_packages)
                
        if os.path.exists(dest_file):
            os.remove(dest_file)
            
        print(f"✅ Successfully installed {pkg} to {site_packages}")
    else:
        print(f"❌ Could not find compatible package release for {pkg}")
except Exception as e:
    print(f"❌ PIP Error: {str(e)}")
                """.trimIndent()

                val outputStream = ByteArrayOutputStream()
                val printStream = PrintStream(outputStream)

                val oldOut = System.out
                System.setOut(printStream)
                
                py.getModule("builtins").callAttr("exec", script)
                
                System.setOut(oldOut)
                val res = outputStream.toString()

                withContext(Dispatchers.Main) {
                    logs = logs + if (res.trim().isEmpty()) "Done." else res.trim()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs = logs + "❌ PIP Installer Failed: ${e.localizedMessage}"
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
                    "  ls                 - List directory files",
                    "  cd <dir>           - Change directory",
                    "  mkdir <name>       - Create folder",
                    "  rm [-rf] <target>  - Delete file/folder",
                    "  python <file.py>   - Run Python file (or 'py <file.py>')",
                    "  pip install <pkg>  - Install Python package from PyPI",
                    "  py install git     - Install standalone Native Git CLI",
                    "  git <args>         - Execute Git CLI"
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

            // SUPPORT UNTUK 'python main.py' ATAU 'py main.py'
            (command == "python" || command == "py") && parts.size > 1 && parts[1] != "install" -> {
                val targetFile = parts[1]
                runPythonScriptFile(targetFile)
            }

            // PIP INSTALLER
            command == "pip" -> {
                if (parts.size >= 3 && parts[1] == "install") {
                    val pkg = parts[2]
                    runPipInstall(pkg)
                } else {
                    logs = logs + "Usage: pip install <package_name>"
                }
            }

            // GIT INSTALLER
            trimmed == "py install git" -> {
                logs = logs + "🌀 Downloading Native Git CLI Binary for Android..."
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val url = URL("https://github.com/its-pointless/its-pointless.github.io/raw/master/bin/git")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        connection.instanceFollowRedirects = true
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                            val inputStream = if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                                val redirectUrl = connection.getHeaderField("Location")
                                URL(redirectUrl).openStream()
                            } else {
                                connection.inputStream
                            }

                            inputStream.use { input ->
                                FileOutputStream(gitExecutable).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            gitExecutable.setExecutable(true, false)
                            withContext(Dispatchers.Main) {
                                logs = logs + "✅ Native Git CLI Installed successfully!"
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                logs = logs + "❌ HTTP Error $responseCode: Binary host unavailable."
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
