package com.mangareader.pagesmanagementservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Configuration {
  @Value("${s3.configuration.location}")
  private String location;
  @Value("${s3.configuration.accessKey}")
  private String acessKey;
  @Value("${s3.configuration.secretKey}")
  private String secretKey;

  @Bean
  MinioClient s3Client() {
    return MinioClient.builder()
        .endpoint(location)
        .credentials(acessKey, secretKey)
        .build();
  }
}
