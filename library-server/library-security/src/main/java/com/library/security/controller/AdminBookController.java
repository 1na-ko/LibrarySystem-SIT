package com.library.security.controller;

import com.library.common.result.Result;
import com.library.core.dto.BookCreateDTO;
import com.library.core.dto.BookUpdateDTO;
import com.library.core.service.BookAdminService;
import com.library.core.vo.BookDetailVO;
import com.library.security.aspect.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 提供图书新增、修改、删除三个端点。仅 LIBRARIAN（{@code book:catalog} 权限）及以上角色可访问。
 * Controller 仅负责参数绑定与鉴权，业务逻辑（ISBN 唯一校验、活跃借阅检查、乐观锁、事件发布）
 * 全部委托给 {@link BookAdminService}，遵循分层规范——不直接依赖 Mapper。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookAdminService bookAdminService;

    @PostMapping
    @RequirePermission("book:catalog")
    public Result<BookDetailVO> create(@Valid @RequestBody BookCreateDTO dto) {
        return Result.success(bookAdminService.createBook(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("book:catalog")
    public Result<BookDetailVO> update(@PathVariable Long id, @Valid @RequestBody BookUpdateDTO dto) {
        return Result.success(bookAdminService.updateBook(id, dto));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("book:catalog")
    public Result<Void> delete(@PathVariable Long id) {
        bookAdminService.deleteBook(id);
        return Result.success();
    }
}
