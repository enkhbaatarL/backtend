package com.sk.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class NCPStorageConfig {

  @Value("${ncp.storage.regionName}")
  private String regionName;

  @Value("${ncp.storage.endPoint}")
  private String endPoint;

  @Value("${ncp.storage.accessKey}")
  private String accessKey;

  @Value("${ncp.storage.secretKey}")
  private String secretKey;

  @Bean
  public AmazonS3 objectStorageClient() {
    return (AmazonS3Client) AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
        .build();
  }

}
