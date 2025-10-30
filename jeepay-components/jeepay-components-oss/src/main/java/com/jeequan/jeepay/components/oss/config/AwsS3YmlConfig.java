package com.jeequan.jeepay.components.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="isys.oss.aws-s3")
public class AwsS3YmlConfig {
    private String endpoint;
    private String bucketName;
    private String accessKey;
    private String secretKey;
}
