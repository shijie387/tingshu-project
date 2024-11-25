package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
//@CrossOrigin(origins = "www.baidu.com")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

	/**
	 * TODO 该接口必须登录才能访问
	 * 内容创作者/运营人员保存专辑
	 *
	 * @param albuminfo
	 * @return
	 */
	@Operation(summary = "内容创作者/运营人员保存专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albuminfoVo) {
		//1.获取当前登录用户ID-从ThreadLocal中获取用户ID 暂时固定为1
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑保存专辑
		albumInfoService.saveAlbumInfo(albuminfoVo, userId);
		return Result.ok();
	}

	/**
	 * TODO 该接口必须登录才能访问
	 * 分页查询当前用户专辑列表
	 *
	 * @param page
	 * @param limit
	 * @return
	 */
	@Operation(summary = "分页查询当前用户专辑列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<Page<AlbumListVo>> getUserAlbumPage(
			@PathVariable int page,
			@PathVariable int limit,
			@RequestBody AlbumInfoQuery query
			){
		Long userId = AuthContextHolder.getUserId();
		query.setUserId(userId);
		Page<AlbumListVo> pageInfo = new Page<>(page, limit);
		pageInfo = albumInfoService.getUserAlbumPage(pageInfo, query);
		return Result.ok(pageInfo);
	}

	/**
	 * 根据专辑ID删除专辑
	 *
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据专辑ID删除专辑")
	@DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
	public Result removeAlbumInfo(@PathVariable Long id) {
		albumInfoService.removeAlbumInfo(id);
		return Result.ok();
	}

	/**
	 * 根据专辑ID查询专辑信息（包括专辑标签列表）
	 *
	 * @param id 专辑ID
	 * @return 专辑信息
	 */
	@Operation(summary = "根据专辑ID查询专辑信息（包括专辑标签列表）")
	@GetMapping("/albumInfo/getAlbumInfo/{id}")
	public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
		AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
		return Result.ok(albumInfo);
	}

	/**
	 * 修改专辑信息
	 * @param id 专辑ID
	 * @param albumInfo 专辑修改后信息
	 * @return
	 */
	@Operation(summary = "修改专辑信息")
	@PutMapping("/albumInfo/updateAlbumInfo/{id}")
	public Result updateAlbumInfo(@PathVariable Long id, @RequestBody AlbumInfo albumInfo){
		albumInfoService.updateAlbumInfo(albumInfo);
		return Result.ok(albumInfo);
	}


	/**
	 * TODO 该接口必须登录才能访问
	 * 获取当前用户全部专辑列表
	 * @return
	 */
	@Operation(summary = "获取当前用户全部专辑列表")
	@GetMapping("/albumInfo/findUserAllAlbumList")
	public Result<List<AlbumInfo>> getUserAllAlbumList(){
		//1.从ThreadLocal中获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑获取专辑列表
		List<AlbumInfo> list  = albumInfoService.getUserAllAlbumList(userId);
		return Result.ok(list);
	}

}

