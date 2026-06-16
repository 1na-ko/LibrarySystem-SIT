package com.library.core.service;

import com.library.core.dto.BookCreateDTO;
import com.library.core.dto.BookUpdateDTO;
import com.library.core.vo.BookDetailVO;

/**
 * 管理端图书编目服务接口.
 * <p>
 * 封装图书新增、修改、删除的业务逻辑（ISBN 唯一校验、活跃借阅检查、乐观锁、领域事件发布），
 * 供管理端 Controller 调用，避免 Controller 直接依赖 Mapper。所有写操作在事务内完成，
 * 领域事件于事务提交后由 {@code @TransactionalEventListener(AFTER_COMMIT)} 异步消费。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface BookAdminService {

    /**
     * 新增图书.
     *
     * @param dto 图书创建参数
     * @return 新建图书详情
     * @throws com.library.common.exception.BizException ISBN 已存在时抛出 DUPLICATE_ISBN
     */
    BookDetailVO createBook(BookCreateDTO dto);

    /**
     * 修改图书（乐观锁 version 校验）.
     *
     * @param id  图书 ID
     * @param dto 图书更新参数（仅非 null 字段生效）
     * @return 更新后图书详情
     * @throws com.library.common.exception.BizException 图书不存在抛 BOOK_NOT_FOUND；乐观锁冲突抛 CONFLICT
     */
    BookDetailVO updateBook(Long id, BookUpdateDTO dto);

    /**
     * 删除图书（逻辑删除）.
     *
     * @param id 图书 ID
     * @throws com.library.common.exception.BizException 图书不存在抛 BOOK_NOT_FOUND；存在活跃借阅抛 CONFLICT
     */
    void deleteBook(Long id);
}
