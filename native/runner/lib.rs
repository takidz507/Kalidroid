//! Small policy core. This is NOT a Linux kernel and does not itself execute Kali.
//! Android/Kotlin will call this layer once the PRoot runner is integrated.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ExecutionMode { Real, Simulated, Unsupported }

#[derive(Debug, Clone)]
pub struct ToolProfile {
    pub id: &'static str,
    pub command: &'static str,
    pub mode: ExecutionMode,
    pub explanation: &'static str,
}

pub fn profile_for(command: &str) -> ToolProfile {
    match command.trim() {
        "nmap" => ToolProfile { id: "nmap", command: "nmap", mode: ExecutionMode::Real, explanation: "Runs only after Nmap is installed in Kali; scan only systems you own or are authorized to test." },
        "airmon-ng" | "aircrack-ng" => ToolProfile { id: "wireless-monitor", command: "", mode: ExecutionMode::Unsupported, explanation: "Monitor mode and packet injection generally require kernel/driver support unavailable to an unrooted Android app." },
        "demo-scan" => ToolProfile { id: "demo-scan", command: "", mode: ExecutionMode::Simulated, explanation: "Educational simulation only; no packets are sent and no host is scanned." },
        _ => ToolProfile { id: "shell", command: "", mode: ExecutionMode::Real, explanation: "Local command in the user's Kali userspace; execution is unavailable until PRoot is integrated." },
    }
}

#[no_mangle]
pub extern "C" fn kalidroid_core_version() -> u32 { 1 }
