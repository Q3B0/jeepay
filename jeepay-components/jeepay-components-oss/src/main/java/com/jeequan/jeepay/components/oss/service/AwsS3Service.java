package com.jeequan.jeepay.components.oss.service;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.jeequan.jeepay.components.oss.config.AliyunOssYmlConfig;
import com.jeequan.jeepay.components.oss.config.AwsS3YmlConfig;
import com.jeequan.jeepay.components.oss.constant.OssSavePlaceEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.net.URI;

@Service
@Slf4j
@ConditionalOnProperty(name = "isys.oss.service-type", havingValue = "asw-s3")
public class AwsS3Service implements IOssService {

    @Autowired
    private AwsS3YmlConfig awsS3YmlConfig;

    // ossClient 初始化
    private S3Client s3Client = null;

    @PostConstruct
    public void init(){
        s3Client = S3Client.builder().endpointOverride(URI.create(awsS3YmlConfig.getEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(awsS3YmlConfig.getAccessKey(), awsS3YmlConfig.getSecretKey())))
                .forcePathStyle(true).build();
    }
    @Override
    public String upload2PreviewUrl(OssSavePlaceEnum ossSavePlaceEnum, MultipartFile multipartFile, String saveDirAndFileName) {
        try {

            if(!bucketExists(awsS3YmlConfig.getBucketName())){
                createBucket(awsS3YmlConfig.getBucketName());
            }
            s3Client.putObject(PutObjectRequest.builder().bucket(awsS3YmlConfig.getBucketName()).key(saveDirAndFileName).contentType(multipartFile.getContentType()).build(),
                    RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
            return saveDirAndFileName;
        } catch (Exception e) {
            log.error("error", e);
            return null;
        }
    }

    @Override
    public boolean downloadFile(OssSavePlaceEnum ossSavePlaceEnum, String source, String target) {
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(software.amazon.awssdk.services.s3.model.GetObjectRequest.builder().bucket(awsS3YmlConfig.getBucketName()).key(source).build());
            byte[] data = response.readAllBytes();
            FileUtil.writeBytes(data, target);
            return true;
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }
    }

    private boolean bucketExists(String bucketName){
        try{
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        }catch (NoSuchBucketException e){
            return false;
        }
    }

    private void createBucket(String bucketName){
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }
}
