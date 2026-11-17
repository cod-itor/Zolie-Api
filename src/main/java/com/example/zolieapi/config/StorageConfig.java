package com.example.zolieapi.config;

import com.example.zolieapi.service.StorageService;
import com.example.zolieapi.service.impl.FakeStorageServiceImpl;
import com.example.zolieapi.service.impl.S3StorageServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public StorageService storageService(ZolieProperties properties) {
        return properties.storage().isConfigured()
                ? new S3StorageServiceImpl(properties.storage())
                : new FakeStorageServiceImpl();
    }
}
