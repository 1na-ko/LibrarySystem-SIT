package com.library.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
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
import com.library.core.service.BookService;
import com.library.core.vo.BookDetailVO;
import com.library.security.aspect.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-图书编目控制器.
 * <p>
 * 提供图书新增、修改、删除三个端点。仅 LIBRARIAN（book:catalog 权限）及以上角色可访问。
 * 遵循 {@link AuthController} 的模式，位于 {@code library-security} 模块以便使用 {@code @RequirePermission}。
 * <p>
 * 修改操作使用 {@code updateById} + 乐观锁 {@code version} 字段（MyBatis-Plus 插件自动管理）。
 * 删除操作由 MyBatis-Plus 全局 {@code logic-delete-field: deleted} 配置自动转为逻辑删除。
 * <p>
 * 注：本 Controller 直接注入 Mapper 用于编目操作（ISBN 唯一校验、活跃借阅检查），
 * 这是阶段 3 的架构取舍——Admin CRUD 的 Service 层将在后续阶段提取为 {@code BookAdminService}。
 * VO 构建委托给 {@link BookService#getById(Long)} 以避免与 {@code BookServiceImpl.toDetailVO()} 重复。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookService bookService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping
    @RequirePermission("book:catalog")
    public Result<BookDetailVO> create(@Valid @RequestBody BookCreateDTO dto) {
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

        eventPublisher.publishEvent(new BookCreatedEvent(book.getId()));
        return Result.success(bookService.getById(book.getId()));
    }

    @PutMapping("/{id}")
    @RequirePermission("book:catalog")
    public Result<BookDetailVO> update(@PathVariable Long id, @Valid @RequestBody BookUpdateDTO dto) {
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
        return Result.success(bookService.getById(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("book:catalog")
    public Result<Void> delete(@PathVariable Long id) {
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
        return Result.success();
    }
}
