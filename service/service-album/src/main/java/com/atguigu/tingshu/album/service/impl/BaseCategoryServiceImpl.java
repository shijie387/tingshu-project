package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	@Override
	public List<JSONObject> getBaseCategoryList() {

		List<JSONObject> list = new ArrayList<>();

		List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);

		if(!CollectionUtils.isEmpty(baseCategoryViewList)){
			Map<Long, List<BaseCategoryView>> categorymap = baseCategoryViewList
					.stream()
					.collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
			for (Map.Entry<Long, List<BaseCategoryView>> entry1 : categorymap.entrySet()) {
				JSONObject jsonObject1 = new JSONObject();
				Long category1Id = entry1.getKey();
				String category1Name = entry1.getValue().get(0).getCategory1Name();
				jsonObject1.put("categoryId", category1Id);
				jsonObject1.put("categoryName", category1Name);

				List<JSONObject> list2 = new ArrayList<>();
				Map<Long, List<BaseCategoryView>> categoryMap2 = entry1.getValue()
						.stream()
						.collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
				for(Map.Entry<Long, List<BaseCategoryView>> entry2 : categoryMap2.entrySet()) {
					JSONObject jsonObject2 = new JSONObject();
					Long category2Id = entry2.getKey();
					String category2Name = entry2.getValue().get(0).getCategory2Name();
					jsonObject2.put("categoryId", category2Id);
					jsonObject2.put("categoryName", category2Name);
					list2.add(jsonObject2);

					List<JSONObject> list3 = new ArrayList<>();
					for(BaseCategoryView baseCategoryView : entry2.getValue()) {
						JSONObject jsonObject3 = new JSONObject();
						jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
						jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
						list3.add(jsonObject3);
					}
					jsonObject2.put("categoryChild", list3);
				}
				jsonObject1.put("categoryChild", list2);
				list.add(jsonObject1);
			}
		}

		return list;
	}

	@Override
	public List<BaseAttribute> getAttributesByCategory1Id(Long category1Id) {
		return baseAttributeMapper.getAttributesByCategory1Id(category1Id);
	}
}
