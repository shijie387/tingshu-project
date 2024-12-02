package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private VodService vodService;
	@Autowired
	private TrackStatMapper trackStatMapper;
	@Autowired
	private UserFeignClient userFeignClient;



	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
		String content = trackInfoVo.getTrackTitle()+trackInfoVo.getTrackIntro();
		String suggest = vodService.scanText(content);
		if("review".equals(suggest) || "block".equals(suggest)){
			throw new RuntimeException("illegal content, edit please");
		}

		//1.根据专辑ID查询专辑信息-更新专辑信息；复用专辑封面图片
		Long albumId = trackInfoVo.getAlbumId();
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		Assert.notNull(albumInfo,"{} - not exist", albumId);


		//2.保存声音信息
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		//2.1 设置声音额外基本信息：用户ID，声音排序序号，状态，来源
		trackInfo.setUserId(userId);
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
		trackInfo.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);

		//2.2 远程调用腾讯点播平台获取音频详情信息
		TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
		if (trackMediaInfoVo != null) {
			trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
			trackInfo.setMediaSize(trackMediaInfoVo.getSize());
			trackInfo.setMediaType(trackMediaInfoVo.getType());
		}
		trackInfoMapper.insert(trackInfo);

		//2.3 新增声音


		//3.更新专辑信息（包含声音数量）
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
		albumInfoMapper.updateById(albumInfo);

		//4.初始化声音统计信息
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY, 0);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT, 0);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE, 0);
		this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT, 0);

		//5.TODO 调用点播平台发起音频文件审核任务（异步审核）
		String taskId = vodService.startAuditReviewTask(trackInfo.getMediaFileId());
		if (StringUtils.isNotBlank(taskId)) {
			trackInfo.setReviewTaskId(taskId);
			trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEWING);
			trackInfoMapper.updateById(trackInfo);
		}

	}
	/**
	 * 保存声音统计信息
	 * @param trackId 声音ID
	 * @param statType 统计类型
	 * @param statNum 统计数值
	 */
	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		trackStatMapper.insert(trackStat);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page<TrackListVo> getUserTrackPage(TrackInfoQuery trackInfoQuery, Page<TrackListVo> pageInfo) {
		return trackInfoMapper.getUserTrackPage(trackInfoQuery, pageInfo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackInfo(TrackInfo trackInfo) {
		//1.判断音频文件是否变更
		//1.1 根据声音ID查询声音记录得到“旧”的音频文件标识
		TrackInfo oldTrackInfo = trackInfoMapper.selectById(trackInfo.getId());
		//1.2 判断文件是否被更新
		if (!trackInfo.getMediaFileId().equals(oldTrackInfo.getMediaFileId())) {
			//1.3 如果文件被更新，再次获取新音频文件信息更新：时长，大小，类型
			TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
			if (mediaInfoVo != null) {
				trackInfo.setMediaType(mediaInfoVo.getType());
				trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
				trackInfo.setMediaSize(mediaInfoVo.getSize());
				trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

				// 音频文件发生更新后，必须再次进行审核
				//4. 开启音视频任务审核；更新声音表：审核任务ID-后续采用定时任务检查审核结果
				//4.1 启动审核任务得到任务ID
				String reviewTaskId = vodService.startAuditReviewTask(trackInfo.getMediaFileId());
				//4.2 更新声音表：审核任务ID，状态（审核中）

				//4.2 更新声音表：审核任务ID，状态（审核中）
				trackInfo.setReviewTaskId(reviewTaskId);
				trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEWING);
				trackInfoMapper.updateById(trackInfo);
			}
			//1.4 从点播平台删除旧的音频文件
			vodService.deleteMedia(oldTrackInfo.getMediaFileId());
		}
		//2.更新声音信息
		trackInfoMapper.updateById(trackInfo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {
		//1.根据ID查询欲被删除声音记录-得到专辑ID跟声音序号
		TrackInfo deleteTrackInfo = trackInfoMapper.selectById(id);
		Long albumId = deleteTrackInfo.getAlbumId();
		Integer orderNum = deleteTrackInfo.getOrderNum();

		//2.更新声音表序号-确保连续
		LambdaUpdateWrapper<TrackInfo> updateWrapper = new LambdaUpdateWrapper<>();
		//2.1 更新条件
		updateWrapper.eq(TrackInfo::getAlbumId, albumId);
		updateWrapper.gt(TrackInfo::getOrderNum, orderNum);
		//2.2 修改字段值
		updateWrapper.setSql("order_num = order_num - 1");
		trackInfoMapper.update(null, updateWrapper);

		//3.删除声音表记录
		trackInfoMapper.deleteById(id);

		//4.删除声音统计
		LambdaQueryWrapper<TrackStat> trackStatLambdaQueryWrapper = new LambdaQueryWrapper<>();
		trackStatLambdaQueryWrapper.eq(TrackStat::getTrackId, id);
		trackStatMapper.delete(trackStatLambdaQueryWrapper);

		//5.更新专辑包含声音数量
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
		albumInfoMapper.updateById(albumInfo);

		//6.删除点播平台音频文件
		vodService.deleteMedia(deleteTrackInfo.getMediaFileId());
	}

	@Override
	public Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {
		//1.分页查询专辑下声音列表 TODO 暂不考虑声音付费标识 默认Vo对象AlbumTrackListVo付费标识：false
		pageInfo = trackInfoMapper.getAlbumTrackPage(pageInfo, albumId);




		return pageInfo;
	}


}
