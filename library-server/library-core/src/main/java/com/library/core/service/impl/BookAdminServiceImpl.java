package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.dto.BookCreateDTO;
import com.library.core.dto.BookUpdateDTO;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.event.BookCreatedEvent;
import com.library.core.event.BookDeletedEvent;
import com.library.core.event.BookUpdatedEvent;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.BookAdminService;
import com.library.core.service.BookService;
import com.library.core.vo.BookDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端图书编目服务实现.
 * <p>
 * 内聚 ISBN 唯一校验、活跃借阅检查、CRUD、乐观锁判断与领域事件发布。
 * 所有写方法标注 {@link Transactional}，确保领域事件在事务内发布，
 * 由 {@code @TransactionalEventListener(AFTER_COMMIT)} 在提交后异步消费，
 * 避免"事务未提交即发事件"的时序竞态。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookAdminServiceImpl implements BookAdminService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookService bookService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BookDetailVO createBook(BookCreateDTO dto) {
        long exists = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getIsbn, dto.getIsbn())
        );
        if (exists > 0) {
            throw new BizException(ErrorCode.DUPLICATE_ISBN);
        }

        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPubDate(dto.getPubDate());
        book.setCategoryId(dto.getCategoryId());
        book.setTotalCopies(dto.getTotalCopies());
        book.setAvailCopies(dto.getTotalCopies());
        book.setDescription(dto.getDescription());
        book.setLocation(dto.getLocation());
        book.setKeywords(dto.getKeywords());
        book.setBorrowCount(0);

        bookMapper.insert(book);
        log.info("图书新增成功: id={}, isbn={}, title={}", book.getId(), book.getIsbn(), book.getTitle());

        // 事件在事务内发布，AFTER_COMMIT 监听器将在提交后异步同步 ES
        eventPublisher.publishEvent(new BookCreatedEvent(book.getId()));
        return bookService.getById(book.getId());
    }

    @Override
    @Transactional
    public BookDetailVO updateBook(Long id, BookUpdateDTO dto) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }

        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        if (dto.getPublisher() != null) book.setPublisher(dto.getPublisher());
        if (dto.getPubDate() != null) book.setPubDate(dto.getPubDate());
        if (dto.getCategoryId() != null) book.setCategoryId(dto.getCategoryId());
        if (dto.getTotalCopies() != null) book.setTotalCopies(dto.getTotalCopies());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());
        if (dto.getLocation() != null) book.setLocation(dto.getLocation());
        if (dto.getKeywords() != null) book.setKeywords(dto.getKeywords());

        int rows = bookMapper.updateById(book);
        if (rows == 0) {
            throw new BizException(ErrorCode.CONFLICT);
        }

        log.info("图书修改成功: id={}", id);
        eventPublisher.publishEvent(new BookUpdatedEvent(id));
        return bookService.getById(id);
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }

        long activeBorrows = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getBookId, id)
                        .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
        );
        if (activeBorrows > 0) {
            throw new BizException(ErrorCode.CONFLICT);
        }

        // MyBatis-Plus 全局 logic-delete-field=deleted 已配置，deleteById 自动转为逻辑删除
        bookMapper.deleteById(id);
        log.info("图书删除成功（逻辑删除）: id={}, title={}", id, book.getTitle());

        eventPublisher.publishEvent(new BookDeletedEvent(id));
    }
}
