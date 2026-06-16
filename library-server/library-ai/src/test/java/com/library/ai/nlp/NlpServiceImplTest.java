package com.library.ai.nlp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NlpServiceImpl} 单元测试.
 * <p>
 * 使用真实 HanLP 调用（本地 JAR，无网络依赖），不 Mock 任何依赖。
 * HanLP 首次调用会触发模型懒加载（约 1-2 秒），在 {@link #setUp()} 中预热。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("NlpServiceImpl")
class NlpServiceImplTest {

    private static NlpServiceImpl nlpService;

    @BeforeAll
    static void setUp() {
        nlpService = new NlpServiceImpl();
        // 预热 HanLP：首次调用触发模型懒加载，避免影响后续测试计时
        nlpService.tokenize("预热");
    }

    @Nested
    @DisplayName("tokenize — 中文分词")
    class Tokenize {

        @Test
        @DisplayName("正常中文文本应返回有效分词结果")
        void shouldTokenizeChineseText() {
            List<String> tokens = nlpService.tokenize("深入理解Java虚拟机");

            assertThat(tokens).isNotEmpty();
            // 至少应包含部分词语
            assertThat(tokens).contains("深入", "理解");
        }

        @Test
        @DisplayName("空文本应返回空列表")
        void shouldReturnEmptyListWhenTextIsNull() {
            assertThat(nlpService.tokenize(null)).isEmpty();
        }

        @Test
        @DisplayName("空白文本应返回空列表")
        void shouldReturnEmptyListWhenTextIsBlank() {
            assertThat(nlpService.tokenize("   ")).isEmpty();
        }

        @Test
        @DisplayName("纯英文文本应仍可分词")
        void shouldTokenizeEnglishText() {
            List<String> tokens = nlpService.tokenize("Hello World");

            assertThat(tokens).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("extractKeywords — 关键词提取")
    class ExtractKeywords {

        @Test
        @DisplayName("正常文本应提取到关键词")
        void shouldExtractKeywords() {
            List<String> keywords = nlpService.extractKeywords(
                    "Java是一种广泛使用的计算机编程语言，拥有跨平台、面向对象、泛型编程的特性", 5);

            assertThat(keywords).isNotEmpty();
            assertThat(keywords.size()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("空文本应返回空列表")
        void shouldReturnEmptyKeywordsWhenTextIsBlank() {
            assertThat(nlpService.extractKeywords("", 5)).isEmpty();
        }

        @Test
        @DisplayName("null 文本应返回空列表")
        void shouldReturnEmptyKeywordsWhenTextIsNull() {
            assertThat(nlpService.extractKeywords(null, 5)).isEmpty();
        }

        @Test
        @DisplayName("topK 为零时应返回空列表")
        void shouldReturnEmptyKeywordsWhenTopKIsZero() {
            List<String> keywords = nlpService.extractKeywords("测试文本", 0);

            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("topK 为负数时应返回空列表")
        void shouldReturnEmptyKeywordsWhenTopKIsNegative() {
            List<String> keywords = nlpService.extractKeywords("测试文本", -1);

            assertThat(keywords).isEmpty();
        }
    }
}
