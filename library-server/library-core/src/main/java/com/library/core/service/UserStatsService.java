package com.library.core.service;

import com.library.core.vo.UserStatsVO;

/**
 * 用户借阅统计服务接口.
 * <p>
 * 封装借阅统计相关的聚合查询逻辑，避免 Controller 层直接操作 Mapper。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface UserStatsService {

    /**
     * 获取用户借阅统计数据.
     * <p>
     * 包含：累计总数、当前在借、超期次数、罚款总额、分类分布、近 12 月趋势。
     *
     * @param userId 用户 ID
     * @return 借阅统计 VO
     */
    UserStatsVO getStats(Long userId);
}
