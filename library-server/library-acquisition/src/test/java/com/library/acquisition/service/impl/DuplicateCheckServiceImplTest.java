package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.enums.MatchStrategyEnum;
import com.library.ai.nlp.NlpService;
import com.library.core.entity.Book;
import com.library.core.mapper.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateCheckServiceImpl")
class DuplicateCheckServiceImplTest {

    @Mock
    private BookMapper bookMapper;
    @Mock
    private NlpService nlpService;

    @InjectMocks
    private DuplicateCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        AcquisitionProperties props = new AcquisitionProperties();
        props.setDuplicateTitleThreshold(0.8);
        props.setDuplicateAuthorTitleThreshold(0.7);
    }

    @Nested
    @DisplayName("checkDuplicate")
    class CheckDuplicate {

        @Test
        @DisplayName("ISBN 精确匹配时应返回 score=1.0 + ISBN_EXACT 策略")
        void shouldReturnExactMatchForIsbn() {
            Book existed = new Book();
            existed.setId(1L); existed.setIsbn("978-7-111-58680-7");
            existed.setTitle("深入理解Java虚拟机"); existed.setAuthor("周志明");
            existed.setDeleted(0);
            when(bookMapper.selectList(any())).thenReturn(List.of());
            when(bookMapper.selectOne(any())).thenReturn(existed);

            var result = service.checkDuplicate("978-7-111-58680-7", "test", "author");
            assertThat(result.isDuplicate()).isTrue();
            assertThat(result.getDuplicates().get(0).getScore()).isEqualTo(1.0);
            assertThat(result.getDuplicates().get(0).getMatchStrategy()).isEqualTo(MatchStrategyEnum.ISBN_EXACT);
        }

        @Test
        @DisplayName("无匹配时应返回 isDuplicate=false")
        void shouldReturnNonDuplicateWhenNoMatch() {
            when(bookMapper.selectOne(any())).thenReturn(null);
            when(bookMapper.selectList(any())).thenReturn(List.of());

            var result = service.checkDuplicate("978-0-000-00000-0", "Unique", "UniqueAuthor");
            assertThat(result.isDuplicate()).isFalse();
            assertThat(result.getDuplicates()).isEmpty();
        }
    }
}
