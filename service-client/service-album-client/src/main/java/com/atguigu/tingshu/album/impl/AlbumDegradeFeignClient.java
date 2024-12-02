package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("error and invoke degradeFeignClient getAlbumInfo");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑服务]提供远程调用方法getCategoryView执行服务降级");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> getTopBaseCategory3(Long category1Id) {
        log.error("[专辑服务]远程调用getTopBaseCategory3执行服务降级");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑服务]远程调用getAlbumStatVo执行服务降级");
        return null;
    }
}
