package com.example.zolieapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zolie")
public record ZolieProperties(Auth auth, Firebase firebase, Storage storage) {

    public record Auth(boolean devTokensEnabled) {
    }

    public record Firebase(String credentialsPath) {

        public boolean isConfigured() {
            return credentialsPath != null && !credentialsPath.isBlank();
        }
    }

    public record Storage(String endpoint, String bucket, String accessKey, String secretKey, String publicBaseUrl) {

        public boolean isConfigured() {
            return endpoint != null && !endpoint.isBlank();
        }
    }
}
