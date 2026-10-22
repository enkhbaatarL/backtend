package com.sk.domain.mi.service.fundamentalmodel.input;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sk.domain.shared.service.ObjectStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 서버가 생성한 private 업로드 원본만 취급한다. 파일명이나 요청 경로를 object key로 쓰지 않는다. */
@Component
public class InputUploadObjects {
    record Reference(String bucket,String key,String location) {}
    private final ObjectStorageService storage;
    private final String endpoint;
    private final String common;
    private final String prefix;
    public InputUploadObjects(ObjectStorageService storage,@Value("${ncp.storage.end-point}") String endpoint,
            @Value("${ncp.storage.bucket-name}") String common,@Value("${ncp.storage.prefix}") String prefix) {
        this.storage=storage;this.endpoint=endpoint;this.common=common;this.prefix=prefix;
    }
    Reference plan(String company,String role,String id) {
        String bucket=role.equals("ADMIN")?common:prefix+company.toLowerCase(Locale.ROOT);
        String key="SCNRCASE/INPUTUPLOAD/"+id+"/original";
        Reference ref=new Reference(bucket,key,endpoint+"/"+bucket+"/"+key);check(ref,company);return ref;
    }
    void put(Reference ref,String company,Path path,String mime)throws Exception {
        check(ref,company);
        try(var input=Files.newInputStream(path)){storage.uploadFile(input,ref.bucket,ref.key,false,mime,Files.size(path));}
    }
    boolean remove(Reference ref,String company)throws Exception {
        check(ref,company);storage.deleteFile(ref.bucket,ref.key);
        try(var ignored=storage.getObject(ref.bucket,ref.key)){return false;}
        catch(AmazonS3Exception ex){if(ex.getStatusCode()==404&&"NoSuchKey".equals(ex.getErrorCode()))return true;throw ex;}
    }
    void check(Reference ref,String company) {
        if(company==null||company.isBlank()||ref==null||!Objects.equals(ref.location,endpoint+"/"+ref.bucket+"/"+ref.key)
            ||!(ref.bucket.equals(common)||ref.bucket.equals(prefix+company.toLowerCase(Locale.ROOT)))
            ||!ref.key.matches("SCNRCASE/INPUTUPLOAD/[0-9a-f-]{36}/original")||ref.location.length()>255)throw InputDraftException.missing();
    }
}
