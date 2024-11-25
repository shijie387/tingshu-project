package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategoryView;
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
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	/**
	 * 查询所有分类（1、2、3级分类）
	 *
	 * @return 业务数据：[{"categoryId":1,"categoryName":"分类",categoryChild:[..]},{其他1级分类}]
	 */
	@Autowired
	private BaseCategoryService baseCategoryService;

	@Operation(summary = "getBaseCategoryList: 1st, 2nd, 3rd")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);
	}

	/**
	 * 根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表
	 *
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取分类属性以及属性值（标签名，标签值）列表")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> getAttributesByCategory1Id(@PathVariable Long category1Id) {
		List<BaseAttribute> list = baseCategoryService.getAttributesByCategory1Id(category1Id);
		return Result.ok(list);
	}


}

