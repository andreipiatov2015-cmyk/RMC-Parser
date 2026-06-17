package com.rmc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UpdateConfig {

    private final String owner;
    private final String repo;
    private final String apiUrl;
    private final String channel;

    public UpdateConfig(String owner, String repo, String apiUrl, String channel) {
        this.owner = owner;
        this.repo = repo;
        this.apiUrl = apiUrl;
        this.channel = channel;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getChannel() {
        return channel;
    }

    public static UpdateConfig load() throws IOException {
        Properties props = new Properties();
        try (InputStream input = UpdateConfig.class.getClassLoader().getResourceAsStream("update.properties")) {
            if (input == null) {
                throw new IOException("Unable to find update.properties");
            }
            props.load(input);
        }

        return new UpdateConfig(
                props.getProperty("github.owner"),
                props.getProperty("github.repo"),
                props.getProperty("github.api"),
                props.getProperty("update.channel")
        );
    }
}