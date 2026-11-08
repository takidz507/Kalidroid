package com.takilabs.kalidroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandPolicyTest {
    @Test fun demoScanIsAlwaysMarkedAsSimulation() {
        assertEquals(ExecutionMode.SIMULATED, CommandPolicy.classify("demo-scan").mode)
    }

    @Test fun monitorModeToolsAreMarkedUnsupported() {
        assertEquals(ExecutionMode.UNSUPPORTED, CommandPolicy.classify("sudo airmon-ng start wlan0").mode)
        assertEquals(ExecutionMode.UNSUPPORTED, CommandPolicy.classify("aireplay-ng --test wlan0").mode)
    }

    @Test fun normalLinuxCommandsRemainReal() {
        assertEquals(ExecutionMode.REAL, CommandPolicy.classify("cat /etc/os-release").mode)
        assertEquals(ExecutionMode.REAL, CommandPolicy.classify("nmap -sV 192.0.2.1").mode)
    }
}
