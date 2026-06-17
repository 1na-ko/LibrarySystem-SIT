package com.library.core.service.impl;

import com.library.core.entity.OperationLogEntity;
import com.library.core.mapper.OperationLogMapper;
import com.library.core.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void insert(OperationLogEntity record) {
        operationLogMapper.insert(record);
    }
}
