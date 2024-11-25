package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.management.ObjectName;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioConstantProperties minioConstantProperties;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private VodService vodService;

    @Override
    public String fileUpload(MultipartFile multipartFile) {
        String ObjectName = null;
        int height = 0;
        int width = 0;
        try {
            ObjectName = null;

            BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
            if (bufferedImage == null) {
                throw new GuiguException(500, "illegal file");
            }

            height = bufferedImage.getHeight();
            width = bufferedImage.getWidth();
            if (height > 900 || width > 900) {
                throw new GuiguException(500, "file is too big (larger than 900*900)");
            }


            String suggest = vodService.scanImage(multipartFile);
            if("review".equals(suggest) || "block".equals(suggest)){
                throw new RuntimeException("illegal image, edit please");
            }

        } catch (IOException e) {
            throw new GuiguException(400, "文件大小有误！");
        }


        try {
            String folderName = "/" + DateUtil.today().replace("-", "");
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(multipartFile.getOriginalFilename());
            ObjectName = folderName + "/" + fileName + "." + extName;

            String bucketName = minioConstantProperties.getBucketName();
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName)
                            .object(ObjectName)
                            .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                            .contentType(multipartFile.getContentType())
                            .build());
            return minioConstantProperties.getEndpointUrl()
                    + "/" + bucketName
                    + "/" + ObjectName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
