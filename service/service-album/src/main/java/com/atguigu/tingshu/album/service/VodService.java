package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> upLoadTrack(MultipartFile file);

    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    String startAuditReviewTask(String mediaFileId);

    void deleteMedia(String mediaFileId);

    String getReviewTaskResult(String reviewTaskId);

    /**
     * 文本审核
     * @param content
     * @return
     */
    String scanText(String content);

    /**
     * 图片审核
     * @param file 图片文件
     * @return
     */
    String scanImage(MultipartFile file);
}
