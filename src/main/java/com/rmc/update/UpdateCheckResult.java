package com.rmc.update;

import java.util.Optional;

/**
 * Результат проверки обновлений через GitHub Releases API.
 */
public class UpdateCheckResult {
    
    private final boolean success;
    private final boolean updateAvailable;
    private final String currentVersion;
    private final String latestVersion;
    private final String downloadUrl;
    private final String assetName;
    private final String releaseNotes;
    private final String releaseUrl;
    private final String errorMessage;
    
    private UpdateCheckResult(Builder builder) {
        this.success = builder.success;
        this.updateAvailable = builder.updateAvailable;
        this.currentVersion = builder.currentVersion;
        this.latestVersion = builder.latestVersion;
        this.downloadUrl = builder.downloadUrl;
        this.assetName = builder.assetName;
        this.releaseNotes = builder.releaseNotes;
        this.releaseUrl = builder.releaseUrl;
        this.errorMessage = builder.errorMessage;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public Optional<String> getLatestVersion() {
        return Optional.ofNullable(latestVersion);
    }
    
    public Optional<String> getDownloadUrl() {
        return Optional.ofNullable(downloadUrl);
    }
    
    public Optional<String> getAssetName() {
        return Optional.ofNullable(assetName);
    }
    
    public Optional<String> getReleaseNotes() {
        return Optional.ofNullable(releaseNotes);
    }
    
    public Optional<String> getReleaseUrl() {
        return Optional.ofNullable(releaseUrl);
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    public static class Builder {
        
        private boolean success;
        private boolean updateAvailable;
        private String currentVersion;
        private String latestVersion;
        private String downloadUrl;
        private String assetName;
        private String releaseNotes;
        private String releaseUrl;
        private String errorMessage;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder updateAvailable(boolean updateAvailable) {
            this.updateAvailable = updateAvailable;
            return this;
        }
        
        public Builder currentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
            return this;
        }
        
        public Builder latestVersion(String latestVersion) {
            this.latestVersion = latestVersion;
            return this;
        }
        
        public Builder downloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }
        
        public Builder assetName(String assetName) {
            this.assetName = assetName;
            return this;
        }
        
        public Builder releaseNotes(String releaseNotes) {
            this.releaseNotes = releaseNotes;
            return this;
        }
        
        public Builder releaseUrl(String releaseUrl) {
            this.releaseUrl = releaseUrl;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public UpdateCheckResult build() {
            return new UpdateCheckResult(this);
        }
    }
}
