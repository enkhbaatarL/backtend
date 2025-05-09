package com.sk.domain.mi.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import com.sk.common.utils.PathUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiObjectStorageService {
    private final AmazonS3 objectStorageClient;

    @Value("${ncp.storage.end-point}")
    private String endPoint;

    /**
    * 파일을 다운로드 한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return byte[] 파일 데이타
    */
    public byte[] downloadFile(String bucketName, String fileName) throws IOException {
        S3Object s3Object = objectStorageClient.getObject(bucketName, fileName);

        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

        return IOUtils.toByteArray(s3ObjectInputStream);
    }

    /**
    * 파일의 URL을 생성한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return
    */
    public String getUrl(String bucketName, String fileName) {
        return PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + fileName;
    }
}
