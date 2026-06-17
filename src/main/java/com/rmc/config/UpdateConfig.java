package com.rmc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UpdateConfig {

    private final String jsonUrl;
    private final String channel;

    public UpdateConfig(String jsonUrl, String channel) {
        this.jsonUrl = jsonUrl;
        this.channel = channel;
    }

    public String getJsonUrl() {
        return jsonUrl;
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
                props.getProperty("update.json.url"),
                props.getProperty("update.channel")
        );
    }
}