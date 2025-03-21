package com.sk.domain.admin.service;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// todo: 멀티파트 업로드 필요시
// https://velog.io/@sangwoo-sean/spring-AWS-S3-Multipart-Upload

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminObjectStorageService {
    // https://guide.ncloud-docs.com/docs/storage-storage-8-1
    private final AmazonS3 objectStorageClient;

    /**
    * 파일을 업로드 한다.
    *
    * @param MultipartFile 단일 파일 업로드
    * @param prefix 파일 이름 접두어
    * @return String 실제 저장 된 파일명
    */
    public String uploadFile(String bucketName, String prefix, MultipartFile file) throws IOException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        String fileName = createFileName(prefix, file.getOriginalFilename());

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), objectMetadata);

        objectStorageClient.putObject(putObjectRequest);

        return fileName;
    }

    /**
    * 파일을 다운로드 한다.
    *
    * @param bucketName 버킷명
    * @param fileName 파일이름
    * @return byte[] 파일 데이타
    */
    public byte[] downloadFile(String bucketName, String fileName) throws IOException {
        S3Object s3Object = objectStorageClient.getObject(bucketName, fileName);

        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

        return IOUtils.toByteArray(s3ObjectInputStream);
    }

    /**
    * 파일을 삭제 한다.
    *
    * @param bucketName 버킷명
    * @param fileName 파일이름
    * @return 
    */
    public void deleteFile(String bucketName, String fileName) {
        objectStorageClient.deleteObject(new DeleteObjectRequest(bucketName, fileName));
    }

    /**
    * 파일의 URL을 생성한다.
    *
    * @param bucketName 버킷명
    * @param fileName 파일이름
    * @return 
    */
    public String getPresignedUrl(String bucketName, String fileName) {
        // presigned URL 만료 시간 설정 (예: 1시간 후 만료)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + (1000 * 60 * 60);
        expiration.setTime(expTimeMillis);

        // presigned URL 생성 요청
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        generatePresignedUrlRequest.addRequestParameter(Headers.S3_CANNED_ACL,
                CannedAccessControlList.PublicRead.toString());

        URL url = objectStorageClient.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public String createFileName(String prefix, String fileName) {
        if (prefix.isEmpty()) {
            return UUID.randomUUID().toString().concat(getFileExtension(fileName));
        } else {
            return prefix.toLowerCase() + "-" + UUID.randomUUID().toString().concat(getFileExtension(fileName));
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
