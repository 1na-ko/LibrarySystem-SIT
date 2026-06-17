package com.library.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Book;
import com.library.core.event.ESSyncListener;
import com.library.core.mapper.BookMapper;
import com.library.core.repository.BookDocument;
import com.library.core.repository.BookESRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ES 测试种子数据导入器（10.18）.
 * <p>
 * 测试启动前将 V100 种子图书（id 10001-10020）批量同步至 Elasticsearch，
 * 绕过 MQ 异步时序，保证集成测试开始时 ES 数据确定就绪。
 * <p>
 * 复用 {@link ESSyncListener#buildDocument} 构建文档逻辑（DRY），避免重复。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsDataLoader {

    private final ESSyncListener esSyncListener;
    private final BookESRepository bookESRepository;
    private final BookMapper bookMapper;

    /**
     * 将 V100 种子图书（10001-10020）批量同步至 ES.
     */
    @Transactional(readOnly = true)
    public void bulkSyncSeedBooks() {
        List<Book> books = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().between(Book::getId, 10001L, 10020L));
        int count = 0;
        for (Book book : books) {
            BookDocument doc = esSyncListener.buildDocument(book.getId());
            if (doc != null) {
                bookESRepository.save(doc);
                count++;
            }
        }
        log.info("ES 测试种子数据导入完成: {} 本图书", count);
    }
}
