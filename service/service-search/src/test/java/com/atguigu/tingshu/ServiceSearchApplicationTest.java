package com.atguigu.tingshu;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ServiceSearchApplicationTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private SearchService searchService;

    @Test
    public void test1() throws Exception{
        Result<AlbumInfo> albumInfo = albumFeignClient.getAlbumInfo(1l);
        System.out.println(albumInfo);
    }

    @Test
    public void test2() throws Exception{
        Result<UserInfoVo> userInfoVo = userFeignClient.getUserInfoVo(1l);
        System.out.println(userInfoVo);
    }
    
    @Test
    public void test3() throws Exception{
        for (long i = 1; i <=1615 ; i++) {
            try {
                searchService.upperAlbum(i);
            } catch (Exception e) {
                continue;
            }
        }
    }

}