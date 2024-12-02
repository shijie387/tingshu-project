package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.aspectj.apache.bcel.util.Repository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
