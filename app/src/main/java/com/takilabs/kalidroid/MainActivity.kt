package com.takilabs.kalidroid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takilabs.kalidroid.core.CommandPolicy
import com.takilabs.kalidroid.core.ExecutionMode
import com.takilabs.kalidroid.runtime.RuntimeManager
import com.takilabs.kalidroid.runtime.ShellSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF090B10)
private val Panel = Color(0xFF141923)
private val Red = Color(0xFFE34B55)
private val Green = Color(0xFF64D99B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KaliDroidScreen(RuntimeManager(applicationContext)) }
    }
}

@Composable
private fun KaliDroidScreen(runtime: RuntimeManager) {
    var installed by remember { mutableStateOf(runtime.isInstalled()) }
    var hasRunner by remember { mutableStateOf(runtime.hasRunner()) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf("البيئة غير مثبتة") }
    var command by remember { mutableStateOf("cat /etc/os-release") }
    var output by remember { mutableStateOf("KaliDroid 0.4.0\nRootless ARM64 workspace\nلم يتم الاتصال ببيئة Kali بعد.") }
    var shell by remember { mutableStateOf<ShellSession?>(null) }
    var connected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun append(text: String) {
        output = (output + text).takeLast(48_000)
    }

    DisposableEffect(Unit) {
        onDispose { shell?.close() }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Ink, surface = Panel)) {
        Column(
            Modifier.fillMaxSize().background(Ink).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KaliDroid", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("ROOTLESS LINUX SECURITY WORKSPACE", color = Color.Gray, fontSize = 9.sp, letterSpacing = 1.2.sp)
                }
                Surface(color = Color(0xFF163426), shape = RoundedCornerShape(50)) {
                    Text("NO ROOT", color = Green, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("KALI NET HUNTER · ARM64 MINIMAL", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(if (installed) "INSTALLED" else "NOT INSTALLED", color = if (installed) Green else Color(0xFFFFC36B), fontSize = 9.sp)
                    }
                    Text(stage, color = Color.LightGray, fontSize = 11.sp)
                    if (busy) LinearProgressIndicator(progress = { if (progress < 0) 0.35f else progress / 100f }, modifier = Modifier.fillMaxWidth(), color = Red)
                    Button(
                        onClick = {
                            if (!busy) {
                                busy = true; progress = 0
                                scope.launch {
                                    try {
                                        runtime.install { p -> mainHandler.post { stage = p.stage; progress = p.percent } }
                                        installed = runtime.isInstalled()
                                        hasRunner = runtime.hasRunner()
                                        stage = if (hasRunner) "تم تثبيت Kali · محرك PRoot متاح" else "تم تثبيت ملفات Kali؛ محرك PRoot يحتاج تضمين مكتباته الأصلية."
                                        append("\n[installer] تم تنزيل rootfs والتحقق من SHA-256 وفك الضغط.")
                                    } catch (t: Throwable) {
                                        stage = "فشل الإعداد: ${t.message ?: "خطأ غير معروف"}"
                                        append("\n[installer:error] ${t.message}")
                                    } finally { busy = false }
                                }
                            }
                        }, enabled = !busy && !installed, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp)
                    ) { Text(if (busy) "جارٍ إعداد Kali…" else if (installed) "Kali مثبتة" else "تنزيل وتثبيت Kali · ~120 MB") }
                    if (installed && !hasRunner) Text("ملفات النظام موجودة، لكن لا يمكن تشغيل أوامرها قبل تضمين PRoot وloader الأصليين.", color = Color(0xFFFFC36B), fontSize = 10.sp)
                    if (installed) OutlinedButton(onClick = {
                        shell?.close(); shell = null; connected = false
                        runtime.removeEnvironment(); installed = false; stage = "تم حذف بيئة Kali"; append("\n[system] تمت إزالة بيئة Kali.")
                    }, modifier = Modifier.fillMaxWidth()) { Text("حذف بيئة Kali") }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TERMINAL", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.weight(1f))
                Text(if (connected) "SHELL CONNECTED" else "DISCONNECTED", color = if (connected) Green else Color(0xFFFFC36B), fontSize = 9.sp)
            }

            Surface(color = Color(0xFF030508), shape = RoundedCornerShape(15.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text(
                        output, color = Color(0xFFB7D9C2), fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)
                    )
                    OutlinedTextField(
                        value = command, onValueChange = { command = it }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, label = { Text("اكتب أمر Kali / Linux") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Red)
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (!connected) {
                                        busy = true
                                        try {
                                            val newShell = runtime.openShell(
                                                onOutput = { chunk -> mainHandler.post { append(chunk) } },
                                                onExit = { code -> mainHandler.post { connected = false; shell = null; append("\n[session ended: $code]") } }
                                            )
                                            shell = newShell; connected = true
                                            append("\n[system] Kali shell connected.\n")
                                        } catch (t: Throwable) { append("\n[shell:error] ${t.message}") }
                                        finally { busy = false }
                                    } else {
                                        val cmd = command.trim()
                                        if (cmd.isEmpty()) return@launch
                                        val profile = CommandPolicy.classify(cmd)
                                        when (profile.mode) {
                                            ExecutionMode.SIMULATED -> append("\n\$ $cmd\n[SIMULATION ONLY] ${profile.message}")
                                            ExecutionMode.UNSUPPORTED -> append("\n\$ $cmd\n[UNSUPPORTED] ${profile.message}")
                                            ExecutionMode.REAL -> try {
                                                shell?.send(cmd)
                                                append("\n\$ $cmd\n")
                                                command = ""
                                            } catch (t: Throwable) { append("\n[command:error] ${t.message}") }
                                        }
                                    }
                                }
                            }, enabled = !busy && installed && hasRunner, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                        ) { Text(if (busy) "…" else if (connected) "SEND" else "CONNECT SHELL") }
                        OutlinedButton(
                            onClick = { shell?.close(); shell = null; connected = false; append("\n[system] Shell disconnected.") },
                            enabled = connected, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                        ) { Text("DISCONNECT") }
                    }
                }
            }
            Text("جلسة Shell مستمرة عبر PRoot · دون Root · ميزات PTY المتقدمة قيد التطوير", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
