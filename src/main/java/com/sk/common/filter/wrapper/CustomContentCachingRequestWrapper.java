package com.sk.common.filter.wrapper;


import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CustomContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    public CustomContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        if (ObjectUtils.isNotEmpty(request.getContentType()) && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(MediaType.parseMediaType(request.getContentType()))) {
            Map<String, String[]> parameterMap = super.getParameterMap();
            String formData = parameterMap.entrySet().stream()
                .flatMap(entry -> {
                    String key = entry.getKey();
                    return java.util.Arrays.stream(entry.getValue()).map(value -> key + "=" + value);
                }).collect(Collectors.joining("&"));
            byteArrayOutputStream.write(formData.getBytes(StandardCharsets.UTF_8));
        } else {
            IOUtils.copy(super.getInputStream(), byteArrayOutputStream);
        }

    }

    @NonNull
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ServletInputStream servletInputStream = super.getInputStream();

        return new ServletInputStream() {
            final ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            @Override
            public int read() {
                return input.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return input.read(b, off, len);
            }

            @Override
            public int read(byte[] b) throws IOException {
                return input.read(b);
            }

            @Override
            public boolean isFinished() {
                return servletInputStream.isFinished();
            }

            @Override
            public boolean isReady() {
                return servletInputStream.isReady();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                servletInputStream.setReadListener(readListener);
            }

            @Override
            public void close() throws IOException {
                super.close();
            }

        };
    }

    /**
     * Converting Http Header to Map Type
     *
     * @return Map
     */
    public Map<String, String> getHeaderMap() {
        return Collections.list(getHeaderNames())
                .stream()
                .collect(Collectors.toMap(name -> name, this::getHeader));
    }

    /**
     * Returns the request body data as a byte array
     *
     * @return byte[]
     */
    public byte[] getBody() {
        return byteArrayOutputStream.toByteArray();
    }

}
