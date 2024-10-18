package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;


    /**
     * TODO:将来将读多写少数据放入Redis中
     * 查询所有分类（1、2、3级分类）
     *
     * @return [{categoryId:1,categoryName:"音乐",categoryChild:[{categoryId:101,categoryName:"音效",categoryChild:[{}]},{}]},{..其他1级分类}]
     */
    @Operation(summary = "查询所有分类（1、2、3级分类）")
    @GetMapping("/category/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        List<JSONObject> list = baseCategoryService.getBaseCategoryList();
        return Result.ok(list);
    }


    /**
     * TODO:将来将读多写少数据放入Redis中
     * 根据一级分类Id获取分类标签以及标签值
     *
     * @param category1Id
     * @return
     */
    @Operation(summary = "根据一级分类Id获取分类标签以及标签值")
    @GetMapping("/category/findAttribute/{category1Id}")
    public Result<List<BaseAttribute>> getAttributeByCategory1Id(@PathVariable Long category1Id) {
        List<BaseAttribute> list = baseCategoryService.getAttributeByCategory1Id(category1Id);
        return Result.ok(list);
    }


}

