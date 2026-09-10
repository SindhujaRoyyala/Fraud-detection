package com.dems.storage.service;

import com.dems.common.exception.ApiException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService {

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final boolean secure;

    private MinioClient minioClient;

    public MinioStorageService(
            @Value("${app.minio.endpoint}") String endpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket-name}") String bucketName,
            @Value("${app.minio.secure:false}") boolean secure) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.secure = secure;
    }

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket exists: {}", bucketName);
            }
        } catch (Exception e) {
            minioClient = null;
            log.warn("MinIO is unavailable at {}. Storage-backed features will be disabled until MinIO is running. {}", endpoint, e.getMessage());
        }
    }

    private void ensureClientAvailable() {
        if (minioClient == null) {
            throw new ApiException("Storage service is unavailable. Start MinIO or configure the backend to the running MinIO endpoint.",
                    HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE");
        }
    }

    public void uploadFile(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ensureClientAvailable();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            log.debug("Uploaded file to MinIO: {}", objectKey);
        } catch (Exception e) {
            throw new ApiException("Failed to upload file to storage: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_UPLOAD_FAILED");
        }
    }

    public InputStream downloadFile(String objectKey) {
        try {
            ensureClientAvailable();
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new ApiException("Failed to download file from storage: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_DOWNLOAD_FAILED");
        }
    }

    public void deleteFile(String objectKey) {
        try {
            ensureClientAvailable();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            log.debug("Deleted file from MinIO: {}", objectKey);
        } catch (Exception e) {
            throw new ApiException("Failed to delete file from storage: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_DELETE_FAILED");
        }
    }

    public boolean fileExists(String objectKey) {
        try {
            ensureClientAvailable();
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generatePresignedDownloadUrl(String objectKey, long expiryMinutes) {
        try {
            ensureClientAvailable();
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry((int) expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new ApiException("Failed to generate presigned URL: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "PRESIGNED_URL_FAILED");
        }
    }

    public String getBucketName() {
        return bucketName;
    }
}
