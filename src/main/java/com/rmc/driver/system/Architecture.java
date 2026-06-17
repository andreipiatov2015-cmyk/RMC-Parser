package com.rmc.driver.system;

/**
 * Перечисление поддерживаемых архитектур.
 */
public enum Architecture {
    X64("amd64", "x64", true),
    X86("x86", "x86", false),
    ARM64("arm64", "aarch64", true),
    UNKNOWN("unknown", "unknown", false);

    private final String archName;
    private final String wdmName;
    private final boolean supported;

    Architecture(String archName, String wdmName, boolean supported) {
        this.archName = archName;
        this.wdmName = wdmName;
        this.supported = supported;
    }

    /**
     * Определить архитектуру текущей системы.
     */
    public static Architecture detect() {
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            return X64;
        } else if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) {
            return X86;
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return ARM64;
        }
        
        return UNKNOWN;
    }

    public String getArchName() {
        return archName;
    }

    public String getWdmName() {
        return wdmName;
    }

    public boolean isSupported() {
        return supported;
    }
}
