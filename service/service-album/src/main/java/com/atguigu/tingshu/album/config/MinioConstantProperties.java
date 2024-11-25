package com.atguigu.tingshu.album.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="minio") //读取节点
@Data
@RefreshScope
public class MinioConstantProperties {

    private String endpointUrl;
    private String accessKey;
    private String secreKey;
    private String bucketName;

    //minio config detail info from nacos
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpointUrl)
                .credentials(accessKey, secreKey)
                .build();
    }
}
