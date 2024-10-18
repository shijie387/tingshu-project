package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author: atguigu
 * @create: 2024-10-18 16:21
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties properties;

    /**
     * 文件上传，图片封面、用户头像
     *
     * @param file 文件
     * @return
     */
    @Override
    public String fileUpload(MultipartFile file) {
        try {
            //1.验证图片文件是否合法、超过指定大小
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new GuiguException(500, "文件格式非法");
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            if (width > 900 || height > 900) {
                throw new GuiguException(500, "文件超过900*900");
            }

            //2.将文件上传到MInIO分布式存储服务器
            //2.1 生成全局唯一名称 形式：/年月日路径/文件唯一名称.后缀
            String folderName = DateUtil.today().replace("-", "");
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(file.getOriginalFilename());
            String objectName = "/" + folderName + "/" + fileName + "." + extName;
            //2.2 调用MInIO客户端上传文件方法
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(properties.getBucketName()).object(objectName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            //3.返回文件访问路径
            return properties.getEndpointUrl() + "/" + properties.getBucketName() + objectName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
