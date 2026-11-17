package com.example.zolieapi.service.impl;

import com.example.zolieapi.config.ZolieProperties;
import com.example.zolieapi.service.StorageService;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3StorageServiceImpl implements StorageService {

    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofHours(1);
    private static final Region REGION = Region.of("auto");

    private final String bucket;
    private final String publicBaseUrl;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3StorageServiceImpl(ZolieProperties.Storage storage) {
        this.bucket = storage.bucket();
        this.publicBaseUrl = storage.publicBaseUrl();
        URI endpoint = URI.create(storage.endpoint());
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.accessKey(), storage.secretKey()));
        S3Configuration configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        this.client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(configuration)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(configuration)
                .build();
    }

    @Override
    public PresignedUpload presignUpload(String objectKey, String mimeType, long sizeBytes) {
        String url = presigner.presignPutObject(presign -> presign
                        .signatureDuration(UPLOAD_TTL)
                        .putObjectRequest(put -> put
                                .bucket(bucket)
                                .key(objectKey)
                                .contentType(mimeType)
                                .contentLength(sizeBytes)))
                .url()
                .toString();
        return new PresignedUpload(url, Instant.now().plus(UPLOAD_TTL));
    }

    @Override
    public String downloadUrl(String objectKey) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + objectKey;
        }
        return presigner.presignGetObject(presign -> presign
                        .signatureDuration(DOWNLOAD_TTL)
                        .getObjectRequest(get -> get.bucket(bucket).key(objectKey)))
                .url()
                .toString();
    }

    @Override
    public Optional<StoredObject> stat(String objectKey) {
        try {
            HeadObjectResponse head = client.headObject(request -> request.bucket(bucket).key(objectKey));
            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String objectKey) {
        client.deleteObject(request -> request.bucket(bucket).key(objectKey));
    }

    @Override
    public void download(String objectKey, Path target) {
        client.getObject(request -> request.bucket(bucket).key(objectKey), target);
    }

    @Override
    public void upload(Path source, String objectKey, String mimeType) {
        client.putObject(request -> request.bucket(bucket).key(objectKey).contentType(mimeType),
                RequestBody.fromFile(source));
    }
}
