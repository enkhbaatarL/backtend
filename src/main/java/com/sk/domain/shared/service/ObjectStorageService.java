package com.sk.domain.shared.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.sk.common.utils.PathUtil;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// todo: 멀티파트 업로드 필요시
// https://velog.io/@sangwoo-sean/spring-AWS-S3-Multipart-Upload

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {
    // https://guide.ncloud-docs.com/docs/storage-storage-8-1
    private final AmazonS3 objectStorageClient;

    @Value("${ncp.storage.end-point}")
    private String endPoint;

    @Value("${ncp.storage.bucket-name}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

    // @PostConstruct
    // public void init() {
    // // getBucketList();
    // }

    // public void getBucketList() {
    // List<Bucket> buckets = objectStorageClient.listBuckets();
    // System.out.println("Bucket List: ");
    // for (Bucket bucket : buckets) {
    // System.out.println(" name=" + bucket.getName() + ", creation_date=" + bucket.getCreationDate() + ", owner="
    // + bucket.getOwner().getId());
    // }
    // }

    public S3Object getObject(String bucketName, String fileName) {
        return objectStorageClient.getObject(bucketName, fileName);
    }

    /**
    * 버킷 이름이 비어 있으면 설정된 기본 버킷 이름을 반환한다.
    *
    * @param bucketName 버킷 이름
    * @return 결정된 버킷 이름
    */
    public String resolveBucketName(String bucketName) {
        return (bucketName == null || bucketName.isBlank()) ? commBucketName : bucketName;
    }

    /**
    * 특정 폴더의 하위 폴더 목록과 파일명 목록을 조회한다.
    * 버킷 이름이 비어 있으면 설정된 기본 버킷을 사용한다.
    * recursive가 true이면 폴더 구분 없이 해당 경로 아래 전체 파일을 평탄화하여 조회한다.
    *
    * @param bucketName 버킷 이름
    * @param folderPath 폴더 경로
    * @param recursive 하위 폴더 전체 조회 여부
    * @return folders(하위 폴더 목록), files(파일 목록)
    */
    public Map<String, List<String>> listFolder(String bucketName, String folderPath, boolean recursive) {
        String targetBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = (folderPath == null || folderPath.isBlank()) ? "" : PathUtil.IncludeTrailingPathDelimiter(folderPath.replace("\\", "/"));

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(targetBucketName)
                .withPrefix(normalizedPrefix)
                .withMaxKeys(1000);

        if (!recursive) {
            request.withDelimiter("/");
        }

        List<String> folders = new ArrayList<>();
        List<String> files = new ArrayList<>();

        ListObjectsV2Result result;
        do {
            result = objectStorageClient.listObjectsV2(request);

            folders.addAll(result.getCommonPrefixes());

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                if (key.equals(normalizedPrefix)) {
                    continue;
                }
                files.add(key);
            }

            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        log.info("List folder completed. bucket={}, prefix={}, recursive={}, folders={}, files={}", targetBucketName, normalizedPrefix, recursive, folders.size(), files.size());

        Map<String, List<String>> listing = new LinkedHashMap<>();
        listing.put("folders", folders);
        listing.put("files", files);
        return listing;
    }

    public S3ObjectInputStream getObjectContent(String bucketName, String fileName) {
        log.info("Get Object Content : BucketName={}, filename={}", bucketName, fileName);
        S3Object s3Object = objectStorageClient.getObject(bucketName, fileName);
        return s3Object != null ? s3Object.getObjectContent() : null;
    }

    /**
    * 파일을 업로드 한다.
    *
    * @param file 단일 파일 업로드
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @param isPublic 공개 여부
    * @return String 접근할 수 있는 URL 리턴
    */
    public String uploadFile(MultipartFile file, String bucketName, String fileName, boolean isPublic) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return uploadFile(input, bucketName, fileName, isPublic, file.getContentType(), file.getSize());
        }
    }

    /**
    * 파일을 업로드 한다.
    *
    * @param InputStream 단일 파일 업로드
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @param isPublic 공개 여부
    * @param contentType 타입
    * @param contentLength 크기
    * @return String 접근할 수 있는 URL 리턴
    */
    public String uploadFile(InputStream file, String bucketName, String fileName, boolean isPublic, String contentType, long contentLength)
            throws IOException {
        try (var diagnostics = ObjectStorageUploadDiagnostics.begin(bucketName, fileName, contentLength)) {
            String result = uploadFileInternal(file, bucketName, fileName, isPublic, contentType, contentLength);
            diagnostics.succeeded();
            return result;
        }
    }

    private String uploadFileInternal(InputStream file, String bucketName, String fileName,
            boolean isPublic, String contentType, long contentLength) throws IOException {

        Path path = Paths.get(fileName);
        String directoryPath = PathUtil.IncludeTrailingPathDelimiter(path.getParent().toString().replace("\\", "/"));

        // create bucket
        if (!objectStorageClient.doesBucketExistV2(bucketName)) {
            objectStorageClient.createBucket(bucketName);
        }

        // create directory
        if (!objectStorageClient.doesObjectExist(bucketName, directoryPath)) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(0L);
            objectMetadata.setContentType("application/x-directory");

            objectStorageClient.putObject(bucketName, directoryPath, new ByteArrayInputStream(new byte[0]), objectMetadata);
        }

        // create file
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(contentLength);

        putRepeatableObject(file, bucketName, fileName, objectMetadata, contentLength);

        if (isPublic) {
            this.setACL(bucketName, fileName);
            return PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + fileName;
        }

        return "";// this.getPresignedUrl(bucketName, fileName);
    }

    /** SDK가 이미 전송한 양과 관계없이 다시 읽을 수 있는 입력만 PUT에 전달한다. */
    private void putRepeatableObject(InputStream input, String bucketName, String fileName,
            ObjectMetadata metadata, long contentLength) throws IOException {
        // FileInputStream은 SDK가 ResettableInputStream으로 감싼다. 바이트 배열은 전체 reset이 가능하다.
        if (input instanceof FileInputStream || input instanceof ByteArrayInputStream) {
            objectStorageClient.putObject(new PutObjectRequest(bucketName, fileName, input, metadata));
            return;
        }
        if (contentLength < 0) throw new IllegalArgumentException("upload content length must not be negative");
        Path directory = Path.of(System.getProperty("java.io.tmpdir"));
        // Windows 등 비POSIX 파일 시스템에서는 관리된 임시 디렉터리의 기본 ACL을 상속한다.
        FileAttribute<?>[] attributes = Files.getFileStore(directory).supportsFileAttributeView("posix")
                ? new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------"))}
                : new FileAttribute<?>[0];
        Path temporary = Files.createTempFile(directory, "mi-storage-upload-", ".tmp", attributes);
        try {
            // 임의 스트림은 선언된 크기까지만 디스크에 저장한다. 원본 스트림은 호출자가 닫는다.
            try (OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[65536];
                long remaining = contentLength;
                while (remaining > 0) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("upload copy interrupted");
                    int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) throw new IOException("upload content shorter than declared length");
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
                if (input.read() != -1) throw new IOException("upload content exceeds declared length");
            }
            try (FileInputStream repeatable = new FileInputStream(temporary.toFile())) {
                objectStorageClient.putObject(new PutObjectRequest(bucketName, fileName, repeatable, metadata));
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
    * 파일을 다운로드 한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return byte[] 파일 데이타
    */
    public byte[] downloadFile(String bucketName, String fileName) throws IOException {
        long totalStartedAt = System.nanoTime();

        try {
            long getObjectStartedAt = System.nanoTime();
            S3Object s3Object = objectStorageClient.getObject(bucketName, fileName);
            long storageGetMs = elapsedMs(getObjectStartedAt);

            try (s3Object; S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
                long readStartedAt = System.nanoTime();
                byte[] data = IOUtils.toByteArray(inputStream);
                long storageReadMs = elapsedMs(readStartedAt);

                log.info(
                        "Object storage download completed. bytes={}, storageGetMs={}, storageReadMs={}, totalMs={}",
                        data.length,
                        storageGetMs,
                        storageReadMs,
                        elapsedMs(totalStartedAt));
                return data;
            }
        } catch (IOException | RuntimeException e) {
            log.warn(
                    "Object storage download failed. totalMs={}, exception={}",
                    elapsedMs(totalStartedAt),
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    /**
    * 파일을 삭제 한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
    * @return
    */
    public void deleteFile(String bucketName, String fileName) {
        if (objectStorageClient.doesObjectExist(bucketName, fileName)) {
            objectStorageClient.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        }
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
        if (objectStorageClient.doesObjectExist(bucketName, oldPath)) {
            objectStorageClient.copyObject(bucketName, oldPath, bucketName, newPath);
            copyACL(bucketName, oldPath, newPath);
        }
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
    public String getUrl(String bucketName, String fileName) {
        return PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + fileName;
    }

    /**
    * 파일의 URL을 생성한다.
    *
    * @param bucketName 버킷 이름
    * @param fileName 파일 Full Path
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

    public void copyACL(String bucketName, String oldPath, String newPath) {
        AccessControlList accessControlList = objectStorageClient.getObjectAcl(bucketName, oldPath);

        objectStorageClient.setObjectAcl(bucketName, newPath, accessControlList);
    }

    // debug
    // public void getAll() {
    // List<Bucket> buckets = objectStorageClient.listBuckets();
    // for (Bucket bucket : buckets) {
    // String bucketName = bucket.getName();
    // System.out.println("");
    // System.out.println("bucketName:" + bucketName);

    // if (bucketName.equals("mi-airflow") || bucketName.equals("mi-etl") || bucketName.equals("mi-unstructure-data")
    // || bucketName.equals("mi-unstructure-data")) {
    // continue;
    // }
    // // list all in the bucket
    // try {
    // ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
    // .withBucketName(bucketName)
    // .withMaxKeys(300);

    // ObjectListing objectListing = objectStorageClient.listObjects(listObjectsRequest);

    // System.out.println("Object List:");
    // while (true) {
    // for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
    // if (objectSummary.getKey().contains("airflow") || objectSummary.getKey().contains("docker")) {
    // continue;
    // }

    // System.out.println(" name=" + objectSummary.getKey() + ", size=" + objectSummary.getSize() + ", owner="
    // + objectSummary.getOwner().getId());
    // }

    // if (objectListing.isTruncated()) {
    // objectListing = objectStorageClient.listNextBatchOfObjects(objectListing);
    // } else {
    // break;
    // }
    // }
    // } catch (AmazonS3Exception e) {
    // System.err.println(e.getErrorMessage());
    // System.exit(1);
    // }

    // // top level folders and files in the bucket
    // try {
    // ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
    // .withBucketName(bucketName)
    // .withDelimiter("/")
    // .withMaxKeys(300);

    // ObjectListing objectListing = objectStorageClient.listObjects(listObjectsRequest);

    // System.out.println("Folder List:");
    // for (String commonPrefixes : objectListing.getCommonPrefixes()) {
    // System.out.println(" name=" + commonPrefixes);
    // }

    // System.out.println("File List:");
    // for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
    // System.out.println(
    // " name=" + objectSummary.getKey() + ", size=" + objectSummary.getSize() + ", owner="
    // + objectSummary.getOwner().getId());
    // }
    // } catch (AmazonS3Exception e) {
    // e.printStackTrace();
    // } catch (SdkClientException e) {
    // e.printStackTrace();
    // }
    // }
    // }
}
