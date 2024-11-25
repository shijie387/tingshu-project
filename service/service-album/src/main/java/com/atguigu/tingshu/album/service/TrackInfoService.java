package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {


    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    void saveTrackStat(Long trackId, String statType, int statNum);


    Page<TrackListVo> getUserTrackPage(TrackInfoQuery trackInfoQuery, Page<TrackListVo> pageInfo);

    void updateTrackInfo(TrackInfo trackInfo);

    void removeTrackInfo(Long id);
}
