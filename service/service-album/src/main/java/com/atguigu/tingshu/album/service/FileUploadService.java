package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 文件上传，图片封面、用户头像
     * @param file 文件
     * @return
     */
    String fileUpload(MultipartFile file);
}
