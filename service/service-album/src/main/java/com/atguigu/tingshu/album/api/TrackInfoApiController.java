package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.GGLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	@Autowired
	private VodService vodService;

	/**
	 * 音视频文件上传点播平台
	 *
	 * @param file 文件
	 * @return {mediaFileId:"文件唯一标识",mediaUrl:"在线播放地址"}
	 */
	@Operation(summary = "音视频文件上传点播平台")
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String, String>> upLoadTrack(@RequestParam("file") MultipartFile file) {
		Map<String, String> map = vodService.upLoadTrack(file);
		return Result.ok(map);
	}

	/**
	 * TODO 该接口必须登录才能访问
	 * 保存声音
	 *
	 * @param trackInfo
	 * @return
	 */
	@Operation(summary = "保存声音")
	@PostMapping("/trackInfo/saveTrackInfo")
	public Result saveTrackInfo(@Validated @RequestBody TrackInfoVo trackInfoVo) {
		//1.获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务层保存声音-发起审核任务
		trackInfoService.saveTrackInfo(userId, trackInfoVo);
		return Result.ok();
	}

	/**
	 * 条件分页查询当前用户声音列表（包含声音统计信息）
	 *
	 * @param page           页码
	 * @param limit          页大小
	 * @param trackInfoQuery 查询条件
	 * @return
	 */
	@Operation(summary = "条件分页查询当前用户声音列表（包含声音统计信息）")
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	public Result<Page<TrackListVo>> findUserTrackPage(
			@PathVariable Integer page,
			@PathVariable Integer limit,
			@RequestBody TrackInfoQuery trackInfoQuery) {
		Long userId = AuthContextHolder.getUserId();
		trackInfoQuery.setUserId(userId);

		Page<TrackListVo> pageInfo = new Page<>(page, limit);
		pageInfo = trackInfoService.getUserTrackPage(trackInfoQuery,pageInfo);
		return Result.ok(pageInfo);
	}

	/**
	 * 根据声音ID查询声音信息
	 *
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据声音ID查询声音信息")
	@GetMapping("/trackInfo/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
		TrackInfo trackInfo = trackInfoService.getById(id);
		return Result.ok(trackInfo);
	}

	/**
	 * 修改声音信息
	 * @param id
	 * @param trackInfo
	 * @return
	 */
	@Operation(summary = "修改声音信息")
	@PutMapping("/trackInfo/updateTrackInfo/{id}")
	public Result updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfo trackInfo){
		trackInfoService.updateTrackInfo(trackInfo);
		return Result.ok();
	}

	/**
	 * 删除声音记录
	 * @param id
	 * @return
	 */
	@Operation(summary = "删除声音记录")
	@DeleteMapping("/trackInfo/removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id){
		trackInfoService.removeTrackInfo(id);
		return Result.ok();
	}

	/**
	 * TODO 该方法不登录可以访问，但用户登录状态就可以从ThreadLocal获取用户ID
	 * 分页查询当前用户可见声音列表-动态判断声音付费标识
	 * @param albumId 专辑ID
	 * @param page 页码
	 * @param limit 页大小
	 * @return
	 */
	@GGLogin(required = false)
	@Operation(summary = "分页查询当前用户可见声音列表-动态判断声音付费标识")
	@GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
	public Result<Page<AlbumTrackListVo>> getAlbumTrackPage(
			@PathVariable Long albumId,
			@PathVariable int page,
			@PathVariable int limit
	){
		//1.获取当前登录用户信息
		Long userId = AuthContextHolder.getUserId();
		//2.构建分页所需Page对象
		Page<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
		//3.调用业务层获取业务数据
		pageInfo = trackInfoService.getAlbumTrackPage(pageInfo, albumId, userId);
		return Result.ok(pageInfo);
	}

}

