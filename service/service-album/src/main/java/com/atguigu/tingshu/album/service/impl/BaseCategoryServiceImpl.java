package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}

	@Override
	public List<BaseCategory3> getTopBaseCategory3(Long category1Id) {
		LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(queryWrapper);

		if(!CollectionUtils.isEmpty(baseCategory2List)){
			List<Long> idList2 = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());

			LambdaQueryWrapper<BaseCategory3> queryWrapper3 = new LambdaQueryWrapper<>();
			queryWrapper3.eq(BaseCategory3::getIsTop, 1);
			queryWrapper3.orderByAsc(BaseCategory3::getOrderNum);
			queryWrapper3.in(BaseCategory3::getCategory2Id, idList2);
			queryWrapper3.last("limit 7");
			queryWrapper3.select(BaseCategory3::getId, BaseCategory3::getName, BaseCategory3::getCategory2Id);
			List<BaseCategory3> list = baseCategory3Mapper.selectList(queryWrapper3);
			return list;
		}
		return null;
	}

	@Override
	public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
		//1.根据1级分类ID查询分类视图得到“1级”分类列表  封装1级分类对象
		LambdaQueryWrapper<BaseCategoryView> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(BaseCategoryView::getCategory1Id, category1Id);
		List<BaseCategoryView> category1List = baseCategoryViewMapper.selectList(queryWrapper);
		if (CollectionUtil.isNotEmpty(category1List)) {
			String category1Name = category1List.get(0).getCategory1Name();
			//1.1 封装1级分类对象
			JSONObject jsonObject1 = new JSONObject();
			jsonObject1.put("categoryId", category1Id);
			jsonObject1.put("categoryName", category1Name);
			//2.处理二级分类
			List<JSONObject> jsonObject2List = new ArrayList<>();
			//2.1 对"1级"分类列表进行按2级分类ID分组
			Map<Long, List<BaseCategoryView>> map2 = category1List
					.stream()
					.collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			//2.2 遍历"2级"分类Map
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
				Long category2Id = entry2.getKey();
				String category2Name = entry2.getValue().get(0).getCategory2Name();
				//2.3 封装2级分类JSON对象
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.put("categoryId", category2Id);
				jsonObject2.put("categoryName", category2Name);
				//3.处理三级分类
				List<JSONObject> jsonObject3List = new ArrayList<>();
				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					JSONObject jsonObject3 = new JSONObject();
					jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
					jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
					jsonObject3List.add(jsonObject3);
				}
				jsonObject2.put("categoryChild", jsonObject3List);
				jsonObject2List.add(jsonObject2);
			}
			//2.4 将2级分类集合存入1级分类categoryChild中
			jsonObject1.put("categoryChild", jsonObject2List);
			return jsonObject1;
		}
		return null;
	}
}
