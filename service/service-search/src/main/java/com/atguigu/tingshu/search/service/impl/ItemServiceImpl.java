package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Map<String, Object> getItemData(Long albumId) {
        //1.TODO 根据专辑ID查询布隆过滤器判断专辑是否存在-解决缓存传统问题
        //注意：引入多线程异步任务优化后，存在多线程并发写hashmap（线程不安全）改为线程安全：ConcurrentHashMap
        Map<String, Object> map = new ConcurrentHashMap<>();
        //2.远程调用专辑服务获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑:{}不存在！", albumId);
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolTaskExecutor);


        //3.远程调用专辑服务获取分类信息
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "分类:{}不存在！", albumInfo.getCategory3Id());
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolTaskExecutor);

        //4.远程调用用户服务获取主播信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "主播:{}不存在！", albumInfo.getUserId());
            map.put("announcer", userInfoVo);
        }, threadPoolTaskExecutor);

        //4.远程调用专辑服务获取统计信息
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑统计:{}不存在！", albumId);
            map.put("albumStatVo", albumStatVo);
        }, threadPoolTaskExecutor);

        //5.汇总异步任务
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatVoCompletableFuture,
                baseCategoryViewCompletableFuture,
                announcerCompletableFuture
        ).join();

        return map;
    }
}
