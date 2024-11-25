package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.codec.Base64;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;

import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ims.v20200713.ImsClient;
import com.tencentcloudapi.ims.v20200713.models.ImageModerationRequest;
import com.tencentcloudapi.ims.v20200713.models.ImageModerationResponse;
import com.tencentcloudapi.tms.v20200713.TmsClient;
import com.tencentcloudapi.tms.v20200713.models.TextModerationRequest;
import com.tencentcloudapi.tms.v20200713.models.TextModerationResponse;

import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;

import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient uploadClient;



    @Override
    public Map<String, String> upLoadTrack(MultipartFile file) {
        try {
            //1. 将上传文件保存到临时目录下
            String uploadTempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);

            //2.构造上传请求对象 设置媒体本地上传路径
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(uploadTempPath);

            //3.调用上传，完成文件上传
            VodUploadResponse response = uploadClient.upload(vodConstantProperties.getRegion(), request);
            //4.解析上传响应对象封装响应结果
            if (response != null) {
                String fileId = response.getFileId();
                String mediaUrl = response.getMediaUrl();
                Map<String, String> map = new HashMap<>();
                map.put("mediaFileId", fileId);
                map.put("mediaUrl", mediaUrl);
                return map;
            }
            return null;
        } catch (Exception e) {
            throw new GuiguException(500, "上传文件到云点播服务异常：" + e);
        }
    }

    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);

            //parsing resp
            if(resp != null){
                MediaInfo mediaInfo = resp.getMediaInfoSet()[0];
                if (mediaInfo != null) {
                    MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                    String type = basicInfo.getType();
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    Float duration = metaData.getAudioDuration();
                    Long size = metaData.getSize();
                    TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
                    trackMediaInfoVo.setDuration(duration);
                    trackMediaInfoVo.setSize(size);
                    trackMediaInfoVo.setType(type);
                    return trackMediaInfoVo;
                }
            }

        } catch (Exception e) {
            log.error("tencent cloud, audio exceptions");
        }
        return null;
    }

    @Override
    public String startAuditReviewTask(String mediaFileId) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ReviewAudioVideoRequest req = new ReviewAudioVideoRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个ReviewAudioVideoResponse的实例，与请求对象对应
            ReviewAudioVideoResponse resp = client.ReviewAudioVideo(req);
            // parsing resp
            if(resp != null){
                String taskId = resp.getTaskId();
                return taskId;
            }

        } catch (Exception e) {
//            log.error("tencent cloud, audit exceptions");
        }
        return null;
    }

    @Override
    public void deleteMedia(String mediaFileId) {
        try {
            //1.实例化一个认证对象
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            //4.返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            client.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
//            log.error("[专辑服务]删除点播平台文件异常：{}", e);
        }
    }

    @Override
    public String getReviewTaskResult(String reviewTaskId) {
        try {
            //1.实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            DescribeTaskDetailRequest req = new DescribeTaskDetailRequest();
            req.setTaskId(reviewTaskId);
            //4.查询审核结果 返回的resp是一个DescribeTaskDetailResponse的实例，与请求对象对应
            DescribeTaskDetailResponse resp = client.DescribeTaskDetail(req);
            //5.解析审核结果
            if ("ReviewAudioVideo".equals(resp.getTaskType())) {
                //5.1  判断音视频审核任务是否完成
                if ("FINISH".equals(resp.getStatus())) {
                    //5.2 获取音视频审核结果
                    ReviewAudioVideoTask reviewAudioVideoTask = resp.getReviewAudioVideoTask();
                    if (reviewAudioVideoTask != null) {
                        //5.3 获取审核任务结果
                        ReviewAudioVideoTaskOutput output = reviewAudioVideoTask.getOutput();
                        if (output != null) {
                            //5.4 返回建议结果
                            String suggestion = output.getSuggestion();
                            return suggestion;
                        }
                    }
                }
            }
        } catch (Exception e) {
//            log.error("[点播平台]获取审核结果异常：{}", e);
        }
        return null;
    }

    @Override
    public String scanText(String content) {
        try {
            //1.实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            TmsClient client = new TmsClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            TextModerationRequest req = new TextModerationRequest();
            String encodeContent = Base64.encode(content);
            req.setContent(encodeContent);
            //4.返回的resp是一个TextModerationResponse的实例，与请求对象对应
            TextModerationResponse resp = client.TextModeration(req);
            //5.解析结果
            if (resp != null) {
                String suggestion = resp.getSuggestion();
                return suggestion.toLowerCase();
            }
            // 输出json格式的字符串回包
        } catch (TencentCloudSDKException e) {
            //log.error("[点播平台]内容安全文本检测异常：{}", e);
        }
        return null;
    }

    @Override
    public String scanImage(MultipartFile file) {
        try {
            //1.实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            ImsClient client = new ImsClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            ImageModerationRequest req = new ImageModerationRequest();
            String encodeImage = Base64.encode(file.getInputStream());
            req.setFileContent(encodeImage);
            //4.返回的resp是一个ImageModerationResponse的实例，与请求对象对应
            ImageModerationResponse resp = client.ImageModeration(req);
            //5.解析结果输出json格式的字符串回包
            if (resp != null) {
                String suggestion = resp.getSuggestion();
                return suggestion.toLowerCase();
            }
        } catch (Exception e) {
            //log.error("[点播平台内容安全图片检测异常：{}", e);
        }
        return null;
    }

}
