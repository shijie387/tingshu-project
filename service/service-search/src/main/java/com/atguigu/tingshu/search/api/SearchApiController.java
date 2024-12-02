package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;


    /**
     * 将指定专辑上架到索引库
     * @param albumId 专辑ID
     * @return
     */
    @Operation(summary = "将指定专辑上架到索引库")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    /**
     * 将指定专辑下架，从索引库删除文档
     * @param albumId
     * @return
     */
    @Operation(summary = "将指定专辑下架")
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result albumRemoveOff(@PathVariable Long albumId){
        searchService.albumRemoveOff(albumId);
        return Result.ok();
    }
    /**
     * 站内条件检索专辑接口
     *
     * @param albumIndexQuery
     * @return
     */
    @Operation(summary = "站内条件检索专辑接口")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo vo = searchService.search(albumIndexQuery);
        return Result.ok(vo);
    }

    @Operation(summary = "查询置顶3级分类下热度TOP6专辑列表")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> searchTopCategoryHotAlbum(@PathVariable Long category1Id){
        List<Map<String, Object>> list = searchService.searchTopCategoryHotAlbum(category1Id);
        return Result.ok(list);
    }

    /**
     * 根据用户已录入字符查询提词索引库进行自动补全关键字
     * @param keyword
     * @return
     */
    @Operation(summary = "根据用户已录入字符查询提词索引库进行自动补全关键字")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword){
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }
}

