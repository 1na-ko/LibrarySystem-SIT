package com.library.kg.service.impl;

import com.library.ai.nlp.NlpService;
import com.library.core.entity.Book;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.kg.config.KnowledgeGraphProperties;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.TopicNetworkBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GraphBuildServiceImpl} 单元测试.
 * <p>
 * 重点验证 P0-1 引入的 CITES 引用边构建逻辑：
 * <ul>
 *     <li>有关键词时调用 Neo4jRepository.query 写入 CITES 关系</li>
 *     <li>无关键词时跳过 CITES 构建</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GraphBuildServiceImpl")
class GraphBuildServiceImplTest {

    @Mock
    private Neo4jRepository neo4jRepository;
    @Mock
    private NlpService nlpService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ObjectProvider<TopicNetworkBuilder> topicNetworkBuilderProvider;

    private KnowledgeGraphProperties kgProperties;
    private GraphBuildServiceImpl service;

    @BeforeEach
    void setUp() {
        kgProperties = new KnowledgeGraphProperties();
        // llmService 注入为 null，强制走 fallbackNer 路径
        service = new GraphBuildServiceImpl(neo4jRepository, nlpService, bookMapper,
                categoryMapper, kgProperties, null, topicNetworkBuilderProvider);
    }

    @Nested
    @DisplayName("buildCitationsBySharedKeywords")
    class BuildCitations {

        @Test
        @DisplayName("有关键词时应执行 CITES MERGE Cypher")
        void shouldExecuteCitesCypherWhenKeywordsPresent() {
            Book book = newBook(1L, "深入理解Java虚拟机", "java,jvm,gc");
            when(bookMapper.selectById(1L)).thenReturn(book);
            // fallbackNer 中 extractKeywords 被调用；返回空列表，后续 keywords 字段补充逻辑会添加 3 个关键词
            when(nlpService.extractKeywords(anyString(), anyInt())).thenReturn(Collections.emptyList());
            // CITES Cypher 通过 neo4jRepository.query 执行；返回空列表代表 0 条创建
            when(neo4jRepository.query(anyString(), any(), any()))
                    .thenReturn(Collections.emptyList());

            service.buildGraph(1L);

            // 捕获所有 query 调用，找到包含 CITES 的那一次
            ArgumentCaptor<String> cypherCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor =
                    (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
            verify(neo4jRepository, times(1)).query(cypherCaptor.capture(), paramsCaptor.capture(), any());

            String cypher = cypherCaptor.getValue();
            assertThat(cypher).contains("CITES");
            assertThat(cypher).contains("MERGE");
            assertThat(cypher).contains("HAS_KEYWORD");

            Map<String, Object> params = paramsCaptor.getValue();
            assertThat(params).containsKey("bookId");
            assertThat(params).containsKey("totalKeywords");
            assertThat(params.get("bookId")).isEqualTo(1L);
            // 3 个关键词 = "java","jvm","gc"
            assertThat((Integer) params.get("totalKeywords")).isEqualTo(3);
        }

        @Test
        @DisplayName("无关键词时应跳过 CITES 构建")
        void shouldSkipCitesWhenNoKeywords() {
            // title/author/keywords 均无 — buildExtractionText 返回空串 → 不调用 nlpService.extractKeywords
            // → fallbackNer 返回空 keywords 列表 → totalKeywords=0 早返回
            Book book = newBook(2L, null, null);
            book.setAuthor(null);
            when(bookMapper.selectById(2L)).thenReturn(book);

            service.buildGraph(2L);

            // 不应该有任何 query 调用（CITES 是唯一通过 query 路径的写入）
            verify(neo4jRepository, never()).query(anyString(), any(), any());
        }

        @Test
        @DisplayName("CITES 写入异常时不影响主流程")
        void shouldSwallowExceptionInCites() {
            Book book = newBook(3L, "Spring", "spring,ioc");
            when(bookMapper.selectById(3L)).thenReturn(book);
            when(nlpService.extractKeywords(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(neo4jRepository.query(anyString(), any(), any()))
                    .thenThrow(new RuntimeException("Neo4j down"));

            // 不应抛异常
            service.buildGraph(3L);
            verify(neo4jRepository, times(1)).query(anyString(), any(), any());
        }
    }

    private Book newBook(Long id, String title, String keywords) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setKeywords(keywords);
        b.setBorrowCount(0);
        b.setDeleted(0);
        b.setCreateTime(LocalDateTime.now());
        return b;
    }
}
