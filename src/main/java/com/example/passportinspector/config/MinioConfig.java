package com.example.passportinspector.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MinioConfig {

    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinioProps minioProps() {
        return new MinioProps();
    }

    @Bean
    public MinioClient minioClient(MinioProps props) {
        return MinioClient.builder()
                .endpoint(props.getUrl())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @Getter
    @Setter
    public static class MinioProps {
        private String url;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private Integer partSizeBytes;
    }
}
