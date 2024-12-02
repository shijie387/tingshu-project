package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 专辑模块远程调用Feign接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album", path = "api/album", fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {

    //baseUrl=http://service-album = http://192.168.31.21:8501
    // 客户端从Nacos拉取到目标服务可用实例列表进行负载均衡（默认策略轮询）

    /**
     * 根据专辑ID查询专辑信息（包含标签列表）
     *
     * @param id
     * @return
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);

    /**
     * 根据三级分类ID查询分类视图
     *
     * @param category3Id
     * @return
     */
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据1级分类ID查询置顶3级分类列表
     * @param category1Id
     * @return
     */
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> getTopBaseCategory3(@PathVariable Long category1Id);

    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

}
