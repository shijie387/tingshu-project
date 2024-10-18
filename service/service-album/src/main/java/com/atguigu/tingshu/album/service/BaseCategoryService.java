package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 查询所有分类（1、2、3级分类）
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据一级分类Id获取分类标签以及标签值
     * @param category1Id
     * @return
     */
    List<BaseAttribute> getAttributeByCategory1Id(Long category1Id);
}
