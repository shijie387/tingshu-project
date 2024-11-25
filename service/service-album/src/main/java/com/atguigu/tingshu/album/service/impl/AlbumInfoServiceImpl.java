package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;
	@Autowired
	private AlbumStatMapper albumStatMapper;
	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private VodService vodService;

	/**
	 * 内容创作者/运营人员保存专辑
	 * * TODO 业务校验-验证内容安全
	 * * 1.封装专辑相关信息，保存一条记录到专辑信息表
	 * * 2.封装专辑标签关系集合，保存若干条记录到专辑标签关系表
	 * * 3.封装专辑统计信息，保存4条记录到专辑统计表
	 *
	 * @param albuminfo 新增专辑信息
	 * @param userId    用户ID
	 */
	@Override
	@Transactional(rollbackFor = Exception.class) //默认RuntimeException跟Error回滚事务
	public void saveAlbumInfo(AlbumInfoVo albuminfoVo, Long userId) {
		//audit
		String content = albuminfoVo.getAlbumTitle()+albuminfoVo.getAlbumIntro();
		String suggest = vodService.scanText(content);
		if("block".equals(suggest) || "review".equals(suggest)){
			throw new RuntimeException("illegal content, edit please");
		}

		AlbumInfo albumInfo = BeanUtil.copyProperties(albuminfoVo, AlbumInfo.class);
		albumInfo.setUserId(userId);
		albumInfo.setTracksForFree(5);
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
		albumInfoMapper.insert(albumInfo);
		Long albumId = albumInfo.getId();

		List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
		if(!CollectionUtil.isEmpty(albumAttributeValueVoList)){

			for (AlbumAttributeValue albumAttributeValue : albumAttributeValueVoList) {
				albumAttributeValue.setAlbumId(albumInfo.getId());
				albumAttributeValueMapper.insert(albumAttributeValue);
			}
		}

		this.saveAlbumInfoStat(albumId, SystemConstant.ALBUM_STAT_PLAY, 0);
		this.saveAlbumInfoStat(albumId, SystemConstant.ALBUM_STAT_SUBSCRIBE, 0);
		this.saveAlbumInfoStat(albumId, SystemConstant.ALBUM_STAT_BUY, 0);
		this.saveAlbumInfoStat(albumId, SystemConstant.ALBUM_STAT_COMMENT, 0);



	}

	public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(statType);
		albumStat.setStatNum(statNum);
		albumStatMapper.insert(albumStat);
	}

	@Override
	public Page<AlbumListVo> getUserAlbumPage(Page<AlbumListVo> pageInfo, AlbumInfoQuery query) {
		return albumInfoMapper.getUserAlbumPage(pageInfo,query);
	}

	/**
	 * 根据专辑ID删除专辑
	 * 1.判断该专辑是否关联声音
	 * 2.删除专辑记录
	 * 3.删除统计记录
	 * 4.删除专辑标签记录
	 *
	 * @param id
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAlbumInfo(Long id) {
		//1.判断该专辑是否关联声音-根据专辑ID查询声音表数量进行判断
		LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackInfo::getAlbumId, id);
		Long count = trackInfoMapper.selectCount(queryWrapper);
		if (count > 0) {
			throw new GuiguException(500, "该专辑下存在声音！");
		}
		//2.删除专辑记录
		albumInfoMapper.deleteById(id);
		//3.删除统计记录
		LambdaQueryWrapper<AlbumStat> statLambdaQueryWrapper = new LambdaQueryWrapper<>();
		statLambdaQueryWrapper.eq(AlbumStat::getAlbumId, id);
		albumStatMapper.delete(statLambdaQueryWrapper);
		//4.删除专辑标签记录
		LambdaQueryWrapper<AlbumAttributeValue> attributeValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
		attributeValueLambdaQueryWrapper.eq(AlbumAttributeValue::getAlbumId, id);
		albumAttributeValueMapper.delete(attributeValueLambdaQueryWrapper);
	}

	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);

		LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AlbumAttributeValue::getAlbumId, id);
		List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(queryWrapper);
		if(!CollectionUtil.isEmpty(albumAttributeValues)){
			albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
		}

		return albumInfo;
	}

	@Override
	public void updateAlbumInfo(AlbumInfo albumInfo) {
		//1.修改专辑相关信息
		albumInfoMapper.updateById(albumInfo);
		Long albumId = albumInfo.getId();
		//2.可能需要-修改专辑标签关系
		//3.1 根据专辑ID删除专辑标签关系记录
		LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
		albumAttributeValueMapper.delete(queryWrapper);

		//3.2 重新保存专辑标签关系
		List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
		if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
			for (AlbumAttributeValue albumAttributeValue : albumAttributeValueVoList) {
				albumAttributeValue.setAlbumId(albumId);
				albumAttributeValueMapper.insert(albumAttributeValue);
			}
		}
		//TODO 对修改后内容进行再次内容审核
	}

	/**
	 * 获取当前用户全部专辑列表
	 * @param userId
	 * @return
	 */
	@Override
	public List<AlbumInfo> getUserAllAlbumList(Long userId) {
		//1.构建查询条件QueryWrapper对象
		LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<>();
		//1.1 查询条件
		queryWrapper.eq(AlbumInfo::getUserId, userId);
		//1.2 排序
		queryWrapper.orderByDesc(AlbumInfo::getId);
		//1.3 限制记录数
		queryWrapper.last("LIMIT 200");
		//1.4 指定查询列
		queryWrapper.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle);
		//2.执行列表查询
		return albumInfoMapper.selectList(queryWrapper);
	}
}
