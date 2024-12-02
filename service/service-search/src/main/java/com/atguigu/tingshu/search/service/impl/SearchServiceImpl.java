package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Case;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {


    @Autowired
    private AlbumInfoIndexRepository aiIndexRepository;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private Executor threadPoolTaskExecutor;
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    private static final String INDEX_NAME = "albuminfo";
    private static final String SUGGEST_INDEX_NAME = "suggestinfo";



    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();


        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //2.1 远程调用专辑服务获取专辑信息及标签列表
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "album not exist", albumId);
            BeanUtils.copyProperties(albumInfo, albumInfoIndex);
            //2.3 封装专辑标签列表到索引库文档对象中
            // List<AttributeValueIndex> attributeValueIndexList
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtils.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList
                        .stream()
                        .map(albumAttributeValue -> BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class))
                        .collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        }, threadPoolTaskExecutor);


        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //3.封装文档对象中分类相关信息
            //3.1 远程调用专辑服务-根据专辑所属3级分类ID查询分类信息
            BaseCategoryView categoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(categoryView, "category not exist", albumInfo.getCategory3Id());

            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
            albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
        }, threadPoolTaskExecutor);


        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //4.封装文档对象中主播相关信息
            //4.1 远程调用用户服务-根据专辑所属用户ID查询主播信息
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "用户：{}信息为空", albumInfo.getUserId());
            //4.2 封装主播名称到索引库文档对象中
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolTaskExecutor);


        //5.封装文档对象中统计相关信息 TODO 采用随机生成方式
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            //5.1 封装播放量数值
            int playStatNum = RandomUtil.randomInt(1000, 2000);
            albumInfoIndex.setPlayStatNum(playStatNum);
            //5.2 封装订阅量数值
            int subscribeStatNum = RandomUtil.randomInt(800, 1000);
            albumInfoIndex.setSubscribeStatNum(subscribeStatNum);
            //5.3 封装购买量数值
            int buyStatNum = RandomUtil.randomInt(100, 500);
            albumInfoIndex.setBuyStatNum(buyStatNum);
            //5.4 封装评论量数值
            int commentStatNum = RandomUtil.randomInt(500, 1000);
            albumInfoIndex.setCommentStatNum(commentStatNum);
            //5.5 基于以上生成统计数值计算出当前文档热度分值  热度=累加（不同统计数值*权重）
            BigDecimal bigDecimal1 = new BigDecimal("0.1").multiply(BigDecimal.valueOf(playStatNum));
            BigDecimal bigDecimal2 = new BigDecimal("0.2").multiply(BigDecimal.valueOf(subscribeStatNum));
            BigDecimal bigDecimal3 = new BigDecimal("0.3").multiply(BigDecimal.valueOf(buyStatNum));
            BigDecimal bigDecimal4 = new BigDecimal("0.4").multiply(BigDecimal.valueOf(commentStatNum));
            BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
            albumInfoIndex.setHotScore(hotScore.doubleValue());
        }, threadPoolTaskExecutor);

        CompletableFuture.allOf(categoryCompletableFuture,
                albumInfoCompletableFuture,
                statCompletableFuture,
                userCompletableFuture).join();
        aiIndexRepository.save(albumInfoIndex);

        this.saveSuggestDoc(albumInfoIndex);
    }

    @Override
    public void albumRemoveOff(Long albumId) {
        aiIndexRepository.deleteById(albumId);

        suggestIndexRepository.deleteById(albumId.toString());

    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        try {
            //based on query, search    query -> DSL
            SearchRequest searchRequest = this.buildDSL(albumIndexQuery);

            //use method in elasticearch client to search
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

            //parse result   es -> vo
            return this.parseSearchResult(searchResponse, albumIndexQuery);
        } catch (Exception e) {
            log.error("[检索服务]站内搜索异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        //1.创建检索请求构建器对象
        SearchRequest.Builder builder = new SearchRequest.Builder();

        //设置请求路径中检索：索引库名称
        builder.index(INDEX_NAME);

        BoolQuery.Builder allConditionQueryBuilder = new BoolQuery.Builder();
        //keyword
        String keyword = albumIndexQuery.getKeyword();
        if(StringUtils.isNotBlank(keyword)){
            allConditionQueryBuilder.must(m->m.bool(b->b.should(s->s.match(m1->m1.field("albumTitle").query(keyword)))
                                                        .should(s->s.match(m1->m1.field("albumIntro").query(keyword)))
                                                        .should(s->s.term(m1->m1.field("albumTitle").value(keyword)))
            ));
        }
        //category, filter
        Long category1Id = albumIndexQuery.getCategory1Id();
        if(category1Id != null){
            allConditionQueryBuilder.filter(t->t.term(m->m.field("category1Id").value(category1Id)));
        }
        Long category2Id = albumIndexQuery.getCategory2Id();
        if(category2Id != null){
            allConditionQueryBuilder.filter(t->t.term(m->m.field("category2Id").value(category2Id)));
        }
        Long category3Id = albumIndexQuery.getCategory3Id();
        if(category3Id != null){
            allConditionQueryBuilder.filter(t->t.term(m->m.field("category3Id").value(category3Id)));
        }
        //attributes nested
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if(CollectionUtils.isNotEmpty(attributeList)){
            for (String attr : attributeList) {
                String[] split = attr.split(":");

                if (split != null && split.length == 2) {
                    allConditionQueryBuilder.filter(f -> f.nested(
                            n -> n.path("attributeValueIndexList")
                                    .query(q -> q.bool(
                                            b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                                    ))
                    ));
                }
            }
        };

        builder.query(allConditionQueryBuilder.build()._toQuery());


        //from, size
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        builder.from((pageNo - 1) * pageSize).size(pageSize);
        //hightlight
        if(!StringUtils.isEmpty(keyword)){
            builder.highlight(h -> h.fields("albumeTitle", t -> t.preTags("<font color='red'>").postTags("</font>")));
        }
        //sort
        String order = albumIndexQuery.getOrder();
        if(StringUtils.isNotBlank(order)){
            String[] split = order.split(":"); //split[0]: 1, 2...   split[1]: fileds
            if(split != null && split.length == 2){
                String orderFeild = "";
                switch (split[0]){
                    case "1":
                        orderFeild = "hotScore";
                        break;
                    case "2":
                        orderFeild = "playStatNum";
                        break;
                    case "3":
                        orderFeild ="createTime";
                        break;
                }
                SortOrder sortOrder = split[1].equals("esc") ? SortOrder.Asc : SortOrder.Desc;
                String finalFeild = orderFeild;
                builder.sort(s -> s.field(f->f.field(finalFeild).order(sortOrder)));
            }
        }

        //source
        builder.source(p -> p.filter(t->t.includes("id","albumTitle","albumIntro","coverUrl","includeTrackCount", "playStatNum", "createTime", "payType")));


        return builder.build();
    }

    @Override
    public AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();

        //page
        Integer pageSize = albumIndexQuery.getPageSize();
        Integer pageNo = albumIndexQuery.getPageNo();
        long total = searchResponse.hits().total().value();
        long totalPages = total % pageSize == 0? total / pageSize : total / pageSize + 1;

        vo.setPageSize(pageSize);
        vo.setPageNo(pageNo);
        vo.setTotal(total);
        vo.setTotalPages(totalPages);

        //hightlight, album
        List<Hit<AlbumInfoIndex>> hitList = searchResponse.hits().hits();
        if(CollectionUtils.isNotEmpty(hitList)){
            List<AlbumInfoIndexVo> infoIndexVoList = hitList.stream()
                    .map(hit -> {
                        AlbumInfoIndex albumInfoIndex = hit.source();
                        Map<String, List<String>> highlight = hit.highlight();
                        if (CollectionUtils.isNotEmpty(highlight)) {
                            String albumTitleHL = highlight.get("albumTitle").get(0);
                            albumInfoIndex.setAlbumTitle(albumTitleHL);
                        }
                        return BeanUtil.copyProperties(albumInfoIndex, AlbumInfoIndexVo.class);
                    }).collect(Collectors.toList());
            vo.setList(infoIndexVoList);
        }


        return vo;
    }

    @Override
    public List<Map<String, Object>> searchTopCategoryHotAlbum(Long category1Id) {
        try {
            //get the list of category1id in table2 and search all catgory3id
            // aggregate based on category2id
            List<BaseCategory3> baseCategory3List = albumFeignClient.getTopBaseCategory3(category1Id).getData();
            Assert.notNull(baseCategory3List, "failed", category1Id);

            List<Long> baseCategory3IdList = baseCategory3List.stream()
                    .map(BaseCategory3::getId).collect(Collectors.toList());

            Map<Long, BaseCategory3> category3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, c3 -> c3));

            List<FieldValue> fieldValueList = baseCategory3IdList.stream().map(id -> FieldValue.of(id)).collect(Collectors.toList());

            //elasticsearch
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                            .query(t -> t.terms(f -> f.field("category3Id").terms(t1 -> t1.value(fieldValueList))))
                            .size(0)
                            .aggregations("category3Agg", t -> t.terms(f -> f.field("category3Id").size(10))
                                    .aggregations("top6Agg", a -> a.topHits(s1 -> s1.sort(f -> f.field(o -> o.field("hotScore").order(SortOrder.Desc)))
                                            .size(6)
                                            .source(f1 -> f1.filter(i1 -> i1.includes("id", "albumTitle", "albumIntro", "coverUrl", "includeTrackCount", "playStatNum", "createTime", "payType","category3Id","category2Id","category1Id")))
                                    )))
                    , AlbumInfoIndex.class);
            System.out.println(searchResponse);

            //parse result
            LongTermsAggregate category3Agg = searchResponse.aggregations().get("category3Agg").lterms();
            List<LongTermsBucket> array = category3Agg.buckets().array();
            if(CollectionUtils.isNotEmpty(array)){
                List<Map<String, Object>> mapList = array.stream().map(categoryBucket ->
                {
                    long category3Id = categoryBucket.key();
                    List<Hit<JsonData>> hitList = categoryBucket.aggregations().get("top6Agg").topHits().hits().hits();
                    if (CollectionUtils.isNotEmpty(hitList)) {
                        List<AlbumInfoIndex> top6List = hitList.stream().map(hit -> {
                            String hitSourceStr = hit.source().toString();
                            return JSON.parseObject(hitSourceStr, AlbumInfoIndex.class);
                        }).collect(Collectors.toList());

                        Map<String, Object> map = new HashMap<>();
                        map.put("baseCategory3", category3Map.get(category3Id));
                        map.put("list", top6List);
                        return map;
                    }
                    return null;
                }).collect(Collectors.toList());
                return mapList;
            }

        } catch (Exception e) {
            log.error("[检索服务]检索置顶分类热门专辑异常：{}", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void saveSuggestDoc(AlbumInfoIndex albumInfoIndex) {
        SuggestIndex suggestIndex = new SuggestIndex();

        suggestIndex.setId(albumInfoIndex.getId().toString());

        String albumTitle = albumInfoIndex.getAlbumTitle();
        suggestIndex.setTitle(albumTitle);

        suggestIndex.setKeyword(new Completion(new String[]{albumTitle}));

        String pinyin = PinyinUtil.getPinyin(albumTitle, "");
        suggestIndex.setKeywordPinyin(new Completion(new String[]{pinyin}));

        String firstLetter = PinyinUtil.getFirstLetter(albumTitle, "");
        suggestIndex.setKeywordSequence(new Completion(new String[]{firstLetter}));

        suggestIndexRepository.save(suggestIndex);
    }

    @Override
    public List<String> completeSuggest(String keyword) {
        try {
            //search
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(s -> s.index(SUGGEST_INDEX_NAME).suggest(
                            s1 -> s1.suggesters("letter-suggest", fs -> fs.prefix(keyword).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true)))
                                    .suggesters("pinyin-suggest", s2 -> s2.prefix(keyword).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true)))
                                    .suggesters("keyword-suggest", s2 -> s2.prefix(keyword).completion(c -> c.field("keyword").size(10).skipDuplicates(true)))
                    )
                    , SuggestIndex.class);

            //parse
            HashSet<String> hashSet = new HashSet<>();
            hashSet.addAll(this.parseSuggestResult(searchResponse, "letter-suggest"));
            hashSet.addAll(this.parseSuggestResult(searchResponse, "pinyin-suggest"));
            hashSet.addAll(this.parseSuggestResult(searchResponse, "keyword-suggest"));

            //2.2 如果解析建议提示词列表长度小于10，采用全文查询尝试补全到10个
            if (hashSet.size() < 10) {
                //2.2.1 根据用户录入字符进行全文检索
                SearchResponse<AlbumInfoIndex> matchSearchResponse = elasticsearchClient.search(
                        s -> s.index(INDEX_NAME)
                                .query(q -> q.match(m -> m.field("albumTitle").query(keyword)))
                                .size(10)
                        , AlbumInfoIndex.class
                );
                HitsMetadata<AlbumInfoIndex> hits = matchSearchResponse.hits();
                List<Hit<AlbumInfoIndex>> hitList = hits.hits();
                if (CollectionUtil.isNotEmpty(hitList)) {
                    for (Hit<AlbumInfoIndex> hit : hitList) {
                        AlbumInfoIndex source = hit.source();
                        //2.2.2 将检索到专辑标题内容加入到提词结果列表中
                        hashSet.add(source.getAlbumTitle());
                        if (hashSet.size() >= 10) {
                            break;
                        }
                    }
                }
            }
            if (hashSet.size() >= 10) {
                //如果提词结果列表中大于10截取前10个
                return new ArrayList<>(hashSet).subList(0, 10);
            } else {
                return new ArrayList<>(hashSet);
            }
        } catch (Exception e) {
            log.error("[搜索服务]关键字自动补全异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> parseSuggestResult(SearchResponse<SuggestIndex> searchResponse, String suggestName) {
        //根据自定义建议词参数名称获取结果列表
        List<String> list = new ArrayList<>();
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(suggestName);
        if (CollectionUtil.isNotEmpty(suggestionList)) {
            //遍历得到建议对象
            for (Suggestion<SuggestIndex> suggestIndexSuggestion : suggestionList) {
                for (CompletionSuggestOption<SuggestIndex> option : suggestIndexSuggestion.completion().options()) {
                    SuggestIndex suggestIndex = option.source();
                    list.add(suggestIndex.getTitle());
                }
            }
        }
        return list;
    }
}
