package com.library.core.service;

import com.library.core.vo.DashboardVO;

/**
 * 流通统计 Dashboard Service.
 * <p>
 * 提供管理员 Dashboard 所需的全局统计指标。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface StatsDashboardService {

    /**
     * 获取 Dashboard 数据.
     *
     * @return DashboardVO（今日统计 + 月趋势 + 热门分类 + 在馆人数）
     */
    DashboardVO getDashboard();
}
