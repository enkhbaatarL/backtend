package com.sk.domain.admin.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.sk.common.utils.PathUtil;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// todo: 멀티파트 업로드 필요시
// https://velog.io/@sangwoo-sean/spring-AWS-S3-Multipart-Upload

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminObjectStorageService {
    private final CustomerCompanyRepository customerCompanyRepository;

    // https://guide.ncloud-docs.com/docs/storage-storage-8-1
    private final AmazonS3 objectStorageClient;

    @Value("${ncp.storage.endPoint}")
    private String endPoint;

    @Value("${ncp.storage.bucketName}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

    @PostConstruct
    public void init() {

        // 공통 버킷 생성
        if (!objectStorageClient.doesBucketExistV2(commBucketName)) {
            objectStorageClient.createBucket(commBucketName);
        }

        // 고객사 버킷 생성
        customerCompanyRepository.findByIsDeletedFalseOrderByCustomerCompanyNmAsc().stream().forEach(
                company -> {
                    String customerCompanyCd = company.getCustomerCompanyCd().toLowerCase();
                    String bucketName = prefix + customerCompanyCd;

                    // create bucket
                    if (!objectStorageClient.doesBucketExistV2(bucketName)) {
                        objectStorageClient.createBucket(bucketName);
                    }
                });

        getBucketList();
    }

    public void getBucketList() {
        List<Bucket> buckets = objectStorageClient.listBuckets();
        System.out.println("Bucket List: ");
        for (Bucket bucket : buckets) {
            System.out.println("    name=" + bucket.getName() + ", creation_date=" + bucket.getCreationDate() + ", owner="
                    + bucket.getOwner().getId());
        }
    }

    /**
    * 파일을 업로드 한다.
    *
    * @param MultipartFile 단일 파일 업로드
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @param isPublic 공개 여부
    * @return String 접근할 수 있는 URL 리턴
    */
    public String uploadFile(MultipartFile file, String bucketName, String fileName, boolean isPublic) throws IOException {

        Path path = Paths.get(fileName.replace("\\", "/"));
        String directoryPath = PathUtil.IncludeTrailingPathDelimiter(path.getParent().toString());

        // create directory
        if (!objectStorageClient.doesObjectExist(bucketName, directoryPath)) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(0L);
            objectMetadata.setContentType("application/x-directory");

            objectStorageClient.putObject(bucketName, directoryPath, new ByteArrayInputStream(new byte[0]), objectMetadata);
        }

        // create file
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), objectMetadata);
        objectStorageClient.putObject(putObjectRequest);

        if (isPublic) {
            this.setACL(bucketName, fileName);
            return PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + fileName;
        }

        return "";// this.getPresignedUrl(bucketName, fileName);
    }

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
    * 파일을 삭제 한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return
    */
    public void deleteFile(String bucketName, String fileName) {
        objectStorageClient.deleteObject(new DeleteObjectRequest(bucketName, fileName));
    }

    /**
    * 파일을 복사 한다.
    *
    * @param bucketName 버킷 이름
    * @param oldPath 기존 위치
    * @param newPath 새 위치
    * @return
    */
    public void copyFile(String bucketName, String oldPath, String newPath) {
        objectStorageClient.copyObject(bucketName, oldPath, bucketName, newPath);
    }

    /**
    * 파일을 이동 한다.
    *
    * @param bucketName 버킷 이름
    * @param oldPath 기존 위치
    * @param newPath 새 위치
    * @return
    */
    public void moveFile(String bucketName, String oldPath, String newPath) {
        this.copyFile(bucketName, oldPath, newPath);
        this.deleteFile(bucketName, oldPath);
    }

    /**
    * 파일의 URL을 생성한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return
    */
    // public String getPresignedUrl(String bucketName, String fileName) {
    // // presigned URL 만료 시간 설정 (예: 1시간 후 만료)
    // Date expiration = new Date();
    // long expTimeMillis = expiration.getTime() + (1000 * 60 * 60);
    // expiration.setTime(expTimeMillis);

    // // presigned URL 생성 요청
    // GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
    // .withMethod(HttpMethod.GET)
    // .withExpiration(expiration);

    // generatePresignedUrlRequest.addRequestParameter(Headers.S3_CANNED_ACL,
    // CannedAccessControlList.PublicRead.toString());

    // URL url = objectStorageClient.generatePresignedUrl(generatePresignedUrlRequest);
    // return url.toString();
    // }
    public String getUrl(String bucketName, String fileName) {
        return PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + fileName;
    }

    /**
    * 파일의 ACL을 설정한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return
    */
    public void setACL(String bucketName, String fileName) {
        // get the current ACL
        AccessControlList accessControlList = objectStorageClient.getObjectAcl(bucketName, fileName);

        // add read permission to user by ID
        accessControlList.grantPermission(GroupGrantee.AllUsers, Permission.Read);

        objectStorageClient.setObjectAcl(bucketName, fileName, accessControlList);
    }

    // 테스트 코드
    public void uploadImageTemp(String imageName, int length, ByteArrayInputStream imageInputStream) throws IOException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("image/png");
        objectMetadata.setContentLength(length);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, imageName, imageInputStream, objectMetadata);
        objectStorageClient.putObject(putObjectRequest);
    }

    public void getAll() {
        List<Bucket> buckets = objectStorageClient.listBuckets();
        for (Bucket bucket : buckets) {
            String bucketName = bucket.getName();
            System.out.println("");
            System.out.println("bucketName:" + bucketName);

            if (bucketName.equals("mi-airflow") || bucketName.equals("mi-etl") || bucketName.equals("mi-unstructure-data")
                    || bucketName.equals("mi-unstructure-data")) {
                continue;
            }
            // list all in the bucket
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withMaxKeys(300);

                ObjectListing objectListing = objectStorageClient.listObjects(listObjectsRequest);

                System.out.println("Object List:");
                while (true) {
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        if (objectSummary.getKey().contains("airflow") || objectSummary.getKey().contains("docker")) {
                            continue;
                        }

                        System.out.println(" name=" + objectSummary.getKey() + ", size=" + objectSummary.getSize() + ", owner="
                                + objectSummary.getOwner().getId());
                    }

                    if (objectListing.isTruncated()) {
                        objectListing = objectStorageClient.listNextBatchOfObjects(objectListing);
                    } else {
                        break;
                    }
                }
            } catch (AmazonS3Exception e) {
                System.err.println(e.getErrorMessage());
                System.exit(1);
            }

            // top level folders and files in the bucket
            try {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withDelimiter("/")
                        .withMaxKeys(300);

                ObjectListing objectListing = objectStorageClient.listObjects(listObjectsRequest);

                System.out.println("Folder List:");
                for (String commonPrefixes : objectListing.getCommonPrefixes()) {
                    System.out.println(" name=" + commonPrefixes);
                }

                System.out.println("File List:");
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    System.out.println(
                            " name=" + objectSummary.getKey() + ", size=" + objectSummary.getSize() + ", owner="
                                    + objectSummary.getOwner().getId());
                }
            } catch (AmazonS3Exception e) {
                e.printStackTrace();
            } catch (SdkClientException e) {
                e.printStackTrace();
            }
        }
    }
}
