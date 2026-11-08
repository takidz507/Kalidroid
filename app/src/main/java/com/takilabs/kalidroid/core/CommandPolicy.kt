package com.takilabs.kalidroid.core

enum class ExecutionMode { REAL, SIMULATED, UNSUPPORTED }
data class CommandProfile(val mode: ExecutionMode, val message: String)

object CommandPolicy {
    private val unsupportedOnUnrootedAndroid = setOf("airmon-ng", "aireplay-ng")

    fun classify(command: String): CommandProfile {
        val tokens = Regex("[A-Za-z0-9_./-]+").findAll(command).map { it.value.substringAfterLast('/') }.toSet()
        val executable = command.trim().split(Regex("\\s+"), limit = 2).firstOrNull().orEmpty().substringAfterLast('/')
        return when {
            executable == "demo-scan" -> CommandProfile(ExecutionMode.SIMULATED, "محاكاة تعليمية فقط؛ لا يتم إرسال حزم أو فحص أجهزة.")
            tokens.any { it in unsupportedOnUnrootedAndroid } -> CommandProfile(ExecutionMode.UNSUPPORTED, "هذه الوظيفة تحتاج دعم monitor mode/driver أو صلاحيات لا يوفرها التطبيق دون Root.")
            "aircrack-ng" in tokens -> CommandProfile(ExecutionMode.UNSUPPORTED, "تحليل ملفات الالتقاط ممكن بأدوات مناسبة، لكن وظائف الالتقاط/الحقن اللاسلكي ليست مضمونة دون دعم النظام والتعريفات.")
            executable == "nmap" -> CommandProfile(ExecutionMode.REAL, "فحص الشبكات مسموح فقط للأجهزة والشبكات التي تملكها أو لديك تصريح صريح لاختبارها.")
            else -> CommandProfile(ExecutionMode.REAL, "أمر حقيقي داخل بيئة Kali؛ التنفيذ يعتمد على توفر الأداة وصلاحيات Android.")
        }
    }
}
