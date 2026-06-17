package com.library.core.service;

import com.library.core.entity.OperationLogEntity;

/**
 * 操作日志服务.
 * <p>
 * 封装 {@link com.library.core.mapper.OperationLogMapper} 的 insert 操作，
 * 供跨模块（如 {@code library-security} 的 AOP 切面）通过 Service 层安全调用，
 * 保持分层架构合规：Controller → Service → Mapper。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface OperationLogService {

    /**
     * 写入操作日志.
     * <p>
     * 调用方应在异步上下文中执行，避免阻塞主业务流程。
     *
     * @param record 操作日志实体
     */
    void insert(OperationLogEntity record);
}
