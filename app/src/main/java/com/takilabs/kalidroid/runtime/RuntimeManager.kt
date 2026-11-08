package com.takilabs.kalidroid.runtime

import android.content.Context
import android.os.StatFs
import android.system.Os
import com.takilabs.kalidroid.core.CommandPolicy
import com.takilabs.kalidroid.core.ExecutionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class InstallStatus {
    data class Progress(val stage: String, val percent: Int) : InstallStatus()
    data object Ready : InstallStatus()
}

data class CommandResult(val output: String, val exitCode: Int, val real: Boolean)

class RuntimeManager(private val context: Context) {
    companion object {
        private const val BASE = "https://kali.download/nethunter-images/current/rootfs/"
        private const val ARCHIVE = "kali-nethunter-rootfs-minimal-arm64.tar.xz"
        private const val CHECKSUMS = "SHA256SUMS"
    }

    private val root = File(context.noBackupFilesDir, "kali/rootfs")
    private val work = File(context.noBackupFilesDir, "kali/downloads")
    private val proot = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
    private val loader = File(context.applicationInfo.nativeLibraryDir, "libproot-loader.so")

    fun isInstalled(): Boolean = File(root, "etc/os-release").isFile && File(root, "bin/sh").exists()
    fun hasRunner(): Boolean = proot.canExecute() && loader.isFile
    fun rootPath(): String = root.absolutePath

    suspend fun install(onProgress: (InstallStatus.Progress) -> Unit) = withContext(Dispatchers.IO) {
        if (!android.os.Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
            error("هذا الإصدار يحتاج هاتف ARM64.")
        }
        val stat = StatFs(context.noBackupFilesDir.absolutePath)
        val available = stat.availableBytes
        if (available < 1_500L * 1024L * 1024L) error("المساحة غير كافية. وفر 1.5 GB على الأقل.")
        work.mkdirs()
        val archive = File(work, ARCHIVE)
        val sums = File(work, CHECKSUMS)
        download(BASE + CHECKSUMS, sums) { p -> onProgress(InstallStatus.Progress("تحميل قائمة التحقق", p)) }
        download(BASE + ARCHIVE, archive) { p -> onProgress(InstallStatus.Progress("تحميل Kali ARM64 Minimal", p)) }
        onProgress(InstallStatus.Progress("التحقق من SHA-256", 0))
        val expected = sums.readLines().firstNotNullOfOrNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 2 && parts.last().trimStart('*') == ARCHIVE) parts.first().lowercase() else null
        } ?: error("لم أجد بصمة الأرشيف في SHA256SUMS الرسمي.")
        val actual = sha256(archive)
        if (!actual.equals(expected, ignoreCase = true)) {
            archive.delete(); sums.delete()
            error("فشل التحقق من سلامة الأرشيف. تم رفض التثبيت.")
        }
        onProgress(InstallStatus.Progress("فك ضغط بيئة Kali", 0))
        val staging = File(root.parentFile, "rootfs-staging")
        staging.deleteRecursively(); staging.mkdirs()
        try {
            extractTarXz(archive, staging) { count ->
                if (count % 128 == 0) onProgress(InstallStatus.Progress("فك ضغط الملفات ($count)", -1))
            }
            if (!File(staging, "etc/os-release").isFile || !File(staging, "bin/sh").exists()) {
                error("الأرشيف لا يحتوي على بنية Kali متوقعة.")
            }
            root.deleteRecursively()
            root.parentFile?.mkdirs()
            if (!staging.renameTo(root)) error("تعذر نقل بيئة Kali إلى موقعها النهائي.")
        } catch (t: Throwable) {
            staging.deleteRecursively()
            throw t
        } finally {
            archive.delete(); sums.delete()
        }
        InstallStatus.Ready
    }

    /** Starts one persistent interactive shell inside the Kali userspace. */
    suspend fun openShell(onOutput: (String) -> Unit, onExit: (Int) -> Unit = {}): ShellSession = withContext(Dispatchers.IO) {
        if (!isInstalled()) error("بيئة Kali غير مثبتة. اضغط إعداد Kali أولاً.")
        if (!hasRunner()) error("محرك PRoot الأصلي غير مضمّن بعد. لا يمكن فتح Shell حقيقي.")
        val args = listOf(
            proot.absolutePath, "-r", root.absolutePath, "-0", "--link2symlink",
            "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root",
            "/bin/sh", "-i"
        )
        val tmp = File(context.noBackupFilesDir, "proot-tmp").apply { mkdirs() }
        val process = ProcessBuilder(args).redirectErrorStream(true).apply {
            environment()["PROOT_LOADER"] = loader.absolutePath
            environment()["PROOT_NO_SECCOMP"] = "1"
            environment()["PROOT_TMP_DIR"] = tmp.absolutePath
            environment()["HOME"] = "/root"
            environment()["USER"] = "root"
            environment()["TERM"] = "xterm-256color"
            environment()["PS1"] = "kali@kalidroid:\\w# "
            environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        }.start()
        ShellSession(process, onOutput, onExit)
    }

    suspend fun run(command: String): CommandResult = withContext(Dispatchers.IO) {
        require(command.isNotBlank()) { "اكتب أمراً أولاً." }
        require(command.length <= 4096) { "الأمر طويل جداً." }
        val profile = CommandPolicy.classify(command)
        if (profile.mode == ExecutionMode.UNSUPPORTED) error(profile.message)
        if (profile.mode == ExecutionMode.SIMULATED) error("هذا الأمر محاكاة تعليمية ولا يُرسل إلى بيئة Kali.")
        if (!isInstalled()) error("بيئة Kali غير مثبتة. اضغط إعداد Kali أولاً.")
        if (!hasRunner()) error("محرك PRoot الأصلي غير مضمّن بعد. أضف libproot.so وlibproot-loader.so إلى jniLibs/arm64-v8a ثم أعد البناء.")
        val args = listOf(
            proot.absolutePath, "-r", root.absolutePath, "-0", "--link2symlink",
            "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root",
            "/bin/sh", "-lc", command
        )
        val logFile = File(context.cacheDir, "command-${System.nanoTime()}.log")
        val process = ProcessBuilder(args).redirectErrorStream(true).redirectOutput(logFile).apply {
            environment()["PROOT_LOADER"] = loader.absolutePath
            environment()["PROOT_NO_SECCOMP"] = "1"
            environment()["PROOT_TMP_DIR"] = File(context.noBackupFilesDir, "proot-tmp").apply { mkdirs() }.absolutePath
        }.start()
        val finished = process.waitFor(180, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            logFile.delete()
            error("تجاوز الأمر مهلة 180 ثانية وتم إيقافه.")
        }
        val output = logFile.inputStream().bufferedReader().use { reader ->
            val buffer = CharArray(4096); val result = StringBuilder(); var remaining = 64_000
            while (remaining > 0) {
                val n = reader.read(buffer, 0, minOf(buffer.size, remaining))
                if (n < 0) break
                result.append(buffer, 0, n); remaining -= n
            }
            result.toString()
        }
        logFile.delete()
        CommandResult(output, process.exitValue(), true)
    }

    fun removeEnvironment(): Boolean = root.parentFile?.deleteRecursively() ?: false

    private fun download(url: String, target: File, progress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "KaliDroid/0.2")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) error("فشل التنزيل: HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            val temp = File(target.parentFile, target.name + ".part")
            connection.inputStream.use { input ->
                FileOutputStream(temp).buffered().use { output ->
                    val buffer = ByteArray(64 * 1024); var done = 0L
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        output.write(buffer, 0, n); done += n
                        if (total > 0) progress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                    output.flush()
                }
            }
            if (!temp.renameTo(target)) { temp.delete(); error("تعذر حفظ الملف المنزّل.") }
        } finally { connection.disconnect() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarXz(archive: File, destination: File, onEntry: (Int) -> Unit) {
        val deferredLinks = mutableListOf<TarLink>()
        var count = 0
        // Pass one extracts only directories and regular files. Deferring links prevents
        // a malicious symlink entry from redirecting a later file outside the staging root.
        TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(archive)))).use { tar ->
            while (true) {
                val e = tar.nextTarEntry ?: break
                val out = safeOutputPath(destination, e.name)
                when {
                    e.isDirectory -> out.toFile().mkdirs()
                    e.isSymbolicLink -> deferredLinks += TarLink(e.name, e.linkName, symbolic = true)
                    e.isLink -> deferredLinks += TarLink(e.name, e.linkName, symbolic = false)
                    e.isFile -> {
                        ensureRealParents(destination.toPath(), out.parent)
                        out.parent?.toFile()?.mkdirs()
                        FileOutputStream(out.toFile()).use { output -> copyCurrentTarEntry(tar, output, e.size) }
                        try { Os.chmod(out.toString(), e.mode and 0x1FF) } catch (_: Exception) { }
                    }
                }
                count++; onEntry(count)
            }
        }
        // Pass two restores the links after all regular files have been written.
        for (link in deferredLinks.filter { it.symbolic }) {
            val out = safeOutputPath(destination, link.name)
            ensureRealParents(destination.toPath(), out.parent)
            out.parent?.toFile()?.mkdirs()
            val target = Path.of(link.target)
            if (!target.isAbsolute) {
                val resolved = (out.parent ?: destination.toPath()).resolve(target).normalize()
                if (!resolved.startsWith(destination.toPath())) error("Symlink target escapes the Kali rootfs.")
            }
            if (!Files.exists(out, java.nio.file.LinkOption.NOFOLLOW_LINKS)) Files.createSymbolicLink(out, target)
        }
        for (link in deferredLinks.filter { !it.symbolic }) {
            val out = safeOutputPath(destination, link.name)
            val target = safeOutputPath(destination, link.target)
            ensureRealParents(destination.toPath(), out.parent)
            if (!Files.isRegularFile(target, java.nio.file.LinkOption.NOFOLLOW_LINKS)) continue
            out.parent?.toFile()?.mkdirs()
            if (!Files.exists(out, java.nio.file.LinkOption.NOFOLLOW_LINKS)) Files.createLink(out, target)
        }
    }

    private fun copyCurrentTarEntry(input: TarArchiveInputStream, output: FileOutputStream, entrySize: Long) {
        var remaining = entrySize
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) error("Unexpected end of TAR entry.")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private data class TarLink(val name: String, val target: String, val symbolic: Boolean)

    private fun safeOutputPath(destination: File, raw: String): Path {
        val relative = safeRelative(raw)
        val out = destination.toPath().resolve(relative).normalize()
        if (!out.startsWith(destination.toPath())) error("Archive path escapes the staging root.")
        return out
    }

    private fun ensureRealParents(rootPath: Path, parent: Path?) {
        if (parent == null || !parent.startsWith(rootPath)) error("Invalid archive parent path.")
        var cursor = rootPath
        val relative = rootPath.relativize(parent)
        for (part in relative) {
            cursor = cursor.resolve(part)
            if (Files.isSymbolicLink(cursor)) error("Archive attempts to write through a symlink.")
            if (Files.exists(cursor) && !Files.isDirectory(cursor)) error("Archive parent is not a directory.")
            Files.createDirectories(cursor)
        }
    }

    private fun safeRelative(raw: String): String {
        if (raw.startsWith("/" ) || raw.startsWith("\\")) {
            error("الأرشيف يحتوي مساراً مطلقاً مرفوضاً.")
        }
        val normalized = raw.removePrefix("./")
        val path = Path.of(normalized).normalize()
        if (path.isAbsolute || path.startsWith("..")) error("الأرشيف يحتوي مساراً خارجياً مرفوضاً.")
        return path.toString()
    }
}


/** A long-lived shell process. This uses pipes (not a kernel PTY), so basic shell commands
 * work persistently; full-screen TUI programs and job control require a PTY integration. */
class ShellSession internal constructor(
    private val process: Process,
    private val onOutput: (String) -> Unit,
    private val onExit: (Int) -> Unit
) {
    @Volatile private var closed = false
    private val writer = process.outputStream.bufferedWriter()

    init {
        Thread({
            try {
                process.inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(2048)
                    while (!closed) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        if (count > 0) onOutput(String(buffer, 0, count))
                    }
                }
            } catch (_: Exception) { }
            val code = try { process.waitFor() } catch (_: Exception) { -1 }
            onExit(code)
        }, "kalidroid-shell-output").apply { isDaemon = true; start() }
    }

    @Synchronized fun send(command: String) {
        check(!closed && process.isAlive) { "جلسة Kali غير متصلة." }
        writer.write(command)
        writer.newLine()
        writer.flush()
    }

    @Synchronized fun close() {
        if (closed) return
        closed = true
        try { writer.write("exit"); writer.newLine(); writer.flush() } catch (_: Exception) { }
        try { writer.close() } catch (_: Exception) { }
        if (process.isAlive) process.destroy()
    }
}
