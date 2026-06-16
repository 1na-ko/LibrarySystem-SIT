package com.library.core.service;

import com.library.core.config.RecommendationProperties;
import com.library.core.entity.BorrowRecord;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.impl.CollaborativeFilteringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * CollaborativeFilteringService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("CollaborativeFilteringService")
@ExtendWith(MockitoExtension.class)
class CollaborativeFilteringServiceTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private CollaborativeFilteringServiceImpl cfService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getUserCfTopK()).thenReturn(20);
        lenient().when(properties.getItemCfTopK()).thenReturn(10);
    }

    @Nested
    @DisplayName("recommend")
    class Recommend {

        @Test
        @DisplayName("应返回基于相似用户的推荐候选")
        void shouldReturnCandidatesFromSimilarUsers() {
            // 用户 1 借阅 {1,2,3}，用户 2 借阅 {2,3,4}
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(1L, 2L));
            records.add(buildRecord(1L, 3L));
            records.add(buildRecord(2L, 2L));
            records.add(buildRecord(2L, 3L));
            records.add(buildRecord(2L, 4L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            // 用户 1 与用户 2 相似（交集 {2,3} / sqrt(3*3) = 2/3），推荐书 4
            assertThat(result).containsKey(4L);
        }

        @Test
        @DisplayName("无借阅记录时应返回空")
        void shouldReturnEmptyWhenNoBorrowHistory() {
            // 目标用户无借阅记录
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(2L, 1L));
            records.add(buildRecord(3L, 2L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("无其他用户时应返回空")
        void shouldReturnEmptyWhenNoOtherUsers() {
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(1L, 2L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应排除用户已借阅的图书")
        void shouldExcludeAlreadyBorrowedBooks() {
            // 用户 1 借阅 {1}，用户 2 借阅 {1,2}（共同借了书 1）
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(2L, 1L));
            records.add(buildRecord(2L, 2L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            // 书 1 已被用户 1 借阅，不应出现
            assertThat(result).doesNotContainKey(1L);
        }

        @Test
        @DisplayName("应通过 Jaccard 找到相似图书")
        void shouldFindSimilarBooksViaJaccard() {
            // 书 1 被用户 {1,2,3} 借过，书 2 被用户 {2,3} 借过
            // Jaccard(1,2) = |{2,3}| / |{1,2,3}| = 2/3
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(2L, 1L));
            records.add(buildRecord(3L, 1L));
            records.add(buildRecord(2L, 2L));
            records.add(buildRecord(3L, 2L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            // 用户 1 借了书 1 → Item-CF 应推荐书 2（Jaccard > 0）
            assertThat(result).containsKey(2L);
        }

        @Test
        @DisplayName("无共同借阅者时应返回空 Item-CF")
        void shouldReturnEmptyForItemCFWhenNoCoBorrow() {
            // 书 1 被用户 {1} 借过，书 2 被用户 {2} 借过，无交集
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(2L, 2L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = cfService.recommend(1L);

            // 书 2 与书 1 无共同借阅者 → Item-CF 无候选；且无其他用户 → User-CF 也无
            assertThat(result).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("全库无记录时应返回空")
        void shouldReturnEmptyWhenDatabaseEmpty() {
            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(List.of());

            Map<Long, Double> result = cfService.recommend(1L);

            assertThat(result).isEmpty();
        }
    }

    private BorrowRecord buildRecord(Long userId, Long bookId) {
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        return record;
    }
}
