package com.rmc.driver.resolver;

/**
 * Supported architectures for Microsoft Edge WebDriver.
 */
public enum Architecture {
    X64,
    ARM64,
    UNKNOWN;

    /**
     * Detect the current system architecture.
     *
     * @return Architecture matching the current system
     */
    public static Architecture detect() {
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        
        if (osArch.contains("arm64") || osArch.contains("aarch64")) {
            return ARM64;
        } else if (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.contains("x64")) {
            return X64;
        }
        
        return X64; // Default to X64 for safety
    }

    /**
     * Get the architecture suffix for download URLs.
     *
     * @return Suffix used in Microsoft download URLs
     */
    public String getUrlSuffix() {
        switch (this) {
            case X64:
                return "x64";
            case ARM64:
                return "arm64";
            case UNKNOWN:
            default:
                return "x64";
        }
    }

    /**
     * Check if this architecture requires special handling.
     *
     * @return true if architecture is fully supported
     */
    public boolean isSupported() {
        return this == X64 || this == ARM64;
    }
}