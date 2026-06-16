package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.enums.GapPriorityEnum;
import com.library.acquisition.service.GapAnalysisService;
import com.library.acquisition.vo.GapAnalysisResultVO;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.GapCoreBookPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GapAnalysisServiceImpl implements GapAnalysisService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final AcquisitionProperties props;
    private final ObjectProvider<GapCoreBookPort> portProvider;

    public GapAnalysisServiceImpl(BookMapper bookMapper, CategoryMapper categoryMapper,
                                  AcquisitionProperties props,
                                  ObjectProvider<GapCoreBookPort> portProvider) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.props = props;
        this.portProvider = portProvider;
    }

    @Override
    public GapAnalysisResultVO analyze(Long subjectId) {
        Category category = categoryMapper.selectById(subjectId);
        String subjectName = category != null ? category.getName() : "学科#" + subjectId;

        // 1. 从 KG 获取核心书目 ID 列表
        GapCoreBookPort port = portProvider.getIfAvailable();
        List<Long> coreIds;
        if (port != null) {
            coreIds = port.getCoreBookIds(subjectId, props.getGapCoreBookTopN());
        } else {
            log.info("GapCoreBookPort 不可用（KG 模块未就绪），按借阅热度查询馆藏书目");
            List<Book> books = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getCategoryId, subjectId)
                            .eq(Book::getDeleted, 0)
                            .orderByDesc(Book::getBorrowCount)
                            .last("LIMIT " + props.getGapCoreBookTopN()));
            coreIds = books.stream().map(Book::getId).toList();
        }
        if (coreIds.isEmpty()) {
            return GapAnalysisResultVO.builder()
                    .subjectId(subjectId).subjectName(subjectName)
                    .totalCoreBooks(0).ownedBooks(0).coverage(1.0)
                    .gapBooks(List.of()).build();
        }

        // 2. 批量加载核心书目，消除 N+1
        // selectBatchIds 受全局 logic-delete 过滤，返回结果均为未下架馆藏
        List<Book> coreBooks = bookMapper.selectBatchIds(coreIds);
        List<GapAnalysisResultVO.GapBook> gapBooks = new ArrayList<>();
        for (Book book : coreBooks) {

            int total = book.getTotalCopies() != null ? book.getTotalCopies() : 1;
            int avail = book.getAvailCopies() != null ? book.getAvailCopies() : 1;
            long borrowCount = book.getBorrowCount() != null ? book.getBorrowCount() : 0L;
            double heatRatio = total > 0 ? (double) borrowCount / (total * 12.0) : 0;
            double turnoverRate = total > 0 ? (double) borrowCount / total : 0;
            long rawSuggested = (long) Math.ceil(borrowCount / (double) props.getPredictionCopiesDivisor());
            int suggested = (int) Math.min(rawSuggested, Integer.MAX_VALUE);

            boolean isGap = heatRatio > props.getGapHeatThreshold()
                    && (double) avail / total < 0.2;

            if (isGap) {
                GapPriorityEnum priority;
                if (heatRatio > 0.9) priority = GapPriorityEnum.CRITICAL;
                else if (heatRatio > 0.8) priority = GapPriorityEnum.HIGH;
                else if (heatRatio > 0.7) priority = GapPriorityEnum.MEDIUM;
                else priority = GapPriorityEnum.LOW;

                gapBooks.add(GapAnalysisResultVO.GapBook.builder()
                        .bookId(book.getId())
                        .isbn(book.getIsbn())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .currentCopies(total)
                        .suggestedCopies(suggested)
                        .borrowCount(borrowCount)
                        .heatRatio(heatRatio)
                        .turnoverRate(turnoverRate)
                        .priority(priority)
                        .reason(priority == GapPriorityEnum.CRITICAL ? "热度极高且复本严重不足" : "借阅热需补购复本")
                        .build());
            }
        }

        // coreBooks 已过滤下架书；核心书目中缺失或已下架的计入未拥有
        int ownedCount = coreBooks.size();
        double coverage = (double) ownedCount / coreIds.size();
        if (coverage < props.getGapCoverageThreshold()) {
            log.warn("学科馆藏覆盖率低于阈值: subjectId={}, coverage={}, threshold={}",
                    subjectId, coverage, props.getGapCoverageThreshold());
        }
        return GapAnalysisResultVO.builder()
                .subjectId(subjectId).subjectName(subjectName)
                .totalCoreBooks(coreIds.size()).ownedBooks(ownedCount).coverage(coverage)
                .gapBooks(gapBooks).build();
    }
}
