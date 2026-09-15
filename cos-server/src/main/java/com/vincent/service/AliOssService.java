package com.vincent.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.vincent.config.AliOssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件操作服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliOssService {

    private final OSS ossClient;
    private final AliOssConfig aliOssConfig;

    /**
     * 上传文件到 OSS，返回可公开访问的 URL
     *
     * @param file     上传的文件
     * @param dir      目录前缀，如 "admin/product"
     * @return 文件的完整 URL
     */
    public String upload(MultipartFile file, String dir) {
        // 1. 生成唯一文件名：目录/日期/UUID.后缀
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = dir + "/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;

        // 2. 上传
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(aliOssConfig.getBucketName(), objectName, inputStream);
            ossClient.putObject(request);
        } catch (IOException e) {
            log.error("OSS 文件上传失败：{}", originalFilename, e);
            throw new RuntimeException("文件上传失败", e);
        }

        // 3. 返回完整访问 URL
        return "https://" + aliOssConfig.getBucketName() + "." + aliOssConfig.getEndpoint() + "/" + objectName;
    }

    /**
     * 从 URL 中提取 objectName 并删除
     *
     * @param url 文件的完整 OSS URL
     */
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String prefix = "https://" + aliOssConfig.getBucketName() + "." + aliOssConfig.getEndpoint() + "/";
        if (!url.startsWith(prefix)) {
            log.warn("非 OSS 图片，跳过删除：{}", url);
            return;
        }
        String objectName = url.substring(prefix.length());
        ossClient.deleteObject(aliOssConfig.getBucketName(), objectName);
        log.info("OSS 文件已删除：{}", objectName);
    }
}