package com.bzpet.petuser.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.bzpet.common.exception.BadException;
import com.bzpet.petuser.config.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OssProperties props;

    /**
     * 上传文件到 OSS，返回可访问 URL
     *
     * @param file 文件
     * @param dir  OSS 上的目录，如 avatar
     */
    public String upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BadException("文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        // 用 UUID 命名，避免覆盖与路径穿越
        String objectName = dir + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        OSS ossClient = new OSSClientBuilder().build(
                props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(props.getBucketName(), objectName, in);
        } catch (Exception e) {
            log.error("OSS 上传失败：{}", e.getMessage(), e);
            throw new BadException("文件上传失败");
        } finally {
            ossClient.shutdown();
        }
        return props.getDomain() + "/" + objectName;
    }
}
