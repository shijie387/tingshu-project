package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(AlbumInfoVo albuminfoVo, Long userId);

    void saveAlbumInfoStat(Long albumId, String statType, int statNum);

    Page<AlbumListVo> getUserAlbumPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery query);

    void removeAlbumInfo(Long id);

    AlbumInfo getAlbumInfo(Long id);

    void updateAlbumInfo(AlbumInfo albumInfo);

    List<AlbumInfo> getUserAllAlbumList(Long userId);

    AlbumStatVo getAlbumStatVo(Long albumId);
}
