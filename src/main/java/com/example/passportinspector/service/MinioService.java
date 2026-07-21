package com.example.passportinspector.service;

import com.example.passportinspector.config.MinioConfig;
import com.example.passportinspector.exception.FileUploadException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioService {

    private static final int DEFAULT_PART_SIZE_BYTES = 10 * 1024 * 1024;

    private final MinioClient minioClient;
    private final MinioConfig.MinioProps props;

    public void upload(String objectName, MultipartFile file) {
        checkBucketExists();

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(inputStream, -1, resolvePartSizeBytes())
                            .contentType(resolveContentType(file))
                            .build()
            );

            log.info("File uploaded to MinIO. bucket={}, objectName={}", props.getBucket(), objectName);
        } catch (Exception e) {
            log.error("Error uploading file to MinIO. bucket={}, objectName={}", props.getBucket(), objectName, e);
            throw new FileUploadException("Failed to upload file to MinIO");
        }
    }

    private void checkBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(props.getBucket())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(props.getBucket())
                                .build()
                );

                log.info("MinIO bucket created: {}", props.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to check or create MinIO bucket: {}", props.getBucket(), e);
            throw new FileUploadException("Failed to check or create MinIO bucket");
        }
    }

    private int resolvePartSizeBytes() {
        if (props.getPartSizeBytes() == null) {
            return DEFAULT_PART_SIZE_BYTES;
        }

        return props.getPartSizeBytes();
    }

    private String resolveContentType(MultipartFile file) {
        if (file.getContentType() == null || file.getContentType().isBlank()) {
            return "text/csv";
        }

        return file.getContentType();
    }
}
