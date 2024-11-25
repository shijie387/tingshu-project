package com.atguigu.tingshu.album.job;


import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewResultTask {
    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private VodService vodService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void getAudioReviewTaskResult() {
//        log.info("[定时任务]查询审核中任务ID获取审核结果，根据审核结果更新审核状态");
        //1.根据审核状态（审核中）声音列表
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getStatus, SystemConstant.TRACK_STATUS_REVIEWING);
        queryWrapper.select(TrackInfo::getId, TrackInfo::getReviewTaskId);
        queryWrapper.last("limit 10");
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(queryWrapper);

        //2.调用腾讯点播平台获取审核任务ID对应审核结果
        if (CollectionUtil.isNotEmpty(trackInfoList)) {
            for (TrackInfo trackInfo : trackInfoList) {
                //2.1 根据审核任务ID查询审核结果
                String suggest = vodService.getReviewTaskResult(trackInfo.getReviewTaskId());
                if (StringUtils.isNotBlank(suggest)) {
                    if ("pass".equals(suggest)) {
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
                    } else if ("block".equals(suggest)) {
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
                    } else if ("review".equals(suggest)) {
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_ARTIFICIAL);
                    }
                    //3.根据审核结果更新审核状态
                    trackInfoMapper.updateById(trackInfo);
                }
            }
        }
    }
}
