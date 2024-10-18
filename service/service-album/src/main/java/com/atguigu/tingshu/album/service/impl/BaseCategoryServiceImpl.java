package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    /**
     * 查询所有分类（1、2、3级分类）
     *
     * @return
     */
    @Override
    public List<JSONObject> getBaseCategoryList() {
        //1.封装响应集合对象-封装所有1级分类数据
        List<JSONObject> list = new ArrayList<>();
        //2.查询分类视图获取所有分类视图记录
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //3.处理1级分类数据
        if (CollectionUtil.isNotEmpty(baseCategoryViewList)) {
            //3.1 采用Stream根据1级分类ID进行分组 Map中key=1级分类ID Map中Value="1级"分类集合
            Map<Long, List<BaseCategoryView>> categoryMap = baseCategoryViewList
                    .stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
            //3.2 遍历1级分类Map，创建1级分类JSONObject对象
            for (Map.Entry<Long, List<BaseCategoryView>> entry1 : categoryMap.entrySet()) {
                JSONObject jsonObject1 = new JSONObject();
                //3.2.1 封装1级分类ID
                Long category1Id = entry1.getKey();
                //3.2.1 封装1级分类名称
                String category1Name = entry1.getValue().get(0).getCategory1Name();
                jsonObject1.put("categoryId", category1Id);
                jsonObject1.put("categoryName", category1Name);

                //4.处理1级分类下2级分类
                //4.1 将"1级"分类列表进行按2级分类ID进行分组 Map中key= 2级分类ID Map中Value= "2级"分类集合
                Map<Long, List<BaseCategoryView>> baseCategory2List = entry1.getValue()
                        .stream()
                        .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
                //4.2 遍历2级分类Map，创建2级分类JSONObject对象
                List<JSONObject> jsonObject2List = new ArrayList<>();
                for (Map.Entry<Long, List<BaseCategoryView>> entry2 : baseCategory2List.entrySet()) {
                    JSONObject jsonObject2 = new JSONObject();
                    //4.2.1 封装1级分类ID
                    jsonObject2.put("categoryId", entry2.getKey());
                    //4.2.2 封装2级分类名称
                    jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
                    jsonObject2List.add(jsonObject2);
                    //5.处理2级分类下3级分类
                    //5.1 构建3级分类JSON集合对象
                    List<JSONObject> jsonObject3List = new ArrayList<>();
                    //5.2 遍历"2级"分类集合，获取3级分类对象
                    for (BaseCategoryView baseCategoryView : entry2.getValue()) {
                        JSONObject jsonObject3 = new JSONObject();
                        //5.3 封装3级分类ID和名称
                        jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                        jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                        jsonObject3List.add(jsonObject3);
                    }
                    //5.4 将3级分类集合放入2级分类对象中"categoryChild"
                    jsonObject2.put("categoryChild", jsonObject3List);
                }
                //4.3 将2级分类列表放入1分类对象中categoryChild属性中
                jsonObject1.put("categoryChild", jsonObject2List);
                list.add(jsonObject1);
            }
        }
        return list;
    }

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;
    /**
     * 根据一级分类Id获取分类标签以及标签值
     * @param category1Id 1级分类ID
     * @return [{id:1,attributeName:"讲播形式",attributeValueList:[{valueName:"多人"},{}]},{}]
     */
    @Override
    public List<BaseAttribute> getAttributeByCategory1Id(Long category1Id) {
        return baseAttributeMapper.getAttributeByCategory1Id(category1Id);
    }
}
