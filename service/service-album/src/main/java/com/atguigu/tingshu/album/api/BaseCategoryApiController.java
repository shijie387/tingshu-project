package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory3;
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


	/**
	 * 根据三级分类ID查询分类视图
	 * @param category3Id
	 * @return
	 */
	@Operation(summary = "根据三级分类ID查询分类视图")
	@GetMapping("/category/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
		BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
		return Result.ok(baseCategoryView);
	}

	/**
	 * 根据1级分类ID查询置顶3级分类列表
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据1级分类ID查询置顶3级分类列表")
	@GetMapping("/category/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> getTopBaseCategory3(@PathVariable Long category1Id){
		List<BaseCategory3> list = baseCategoryService.getTopBaseCategory3(category1Id);
		return Result.ok(list);
	}

    /**
     * 查询当前1级分类下包含子分类（二三级分类）
     * @param category1Id
     * @return {"categoryId":1,"categoryName":"音乐",categoryChild:[{"categoryId":101,"categoryName":"音乐音效",categoryChild:[{..}]}]}
     */
    @Operation(summary = "查询当前1级分类下包含子分类（二三级分类）")
    @GetMapping("/category/getBaseCategoryList/{category1Id}")
    public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id){
        JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
        return Result.ok(jsonObject);
    }
}

