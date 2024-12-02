package com.atguigu.tingshu.album.service;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;
import netscape.javascript.JSObject;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    List<JSONObject> getBaseCategoryList();

    List<BaseAttribute> getAttributesByCategory1Id(Long category1Id);

    BaseCategoryView getCategoryView(Long category3Id);

    List<BaseCategory3> getTopBaseCategory3(Long category1Id);

    JSONObject getBaseCategoryListByCategory1Id(Long category1Id);
}
