package com.library.acquisition.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智能采编配置属性.
 * <p>
 * 对应 {@code application.yml} 中 {@code acquisition.*} 配置块。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "acquisition")
public class AcquisitionProperties {

    /** 预测最少数据月数（默认 6） */
    private int predictionMinMonths = 6;
    /** 预测历史月数（默认 12） */
    private int predictionHistoryMonths = 12;
    /** 查重标题阈值（默认 0.6，WP-0：原 0.8 对短标题 NLP 分词偏严，已配合 LIKE 前置降低门槛） */
    private double duplicateTitleThreshold = 0.6;
    /** 查重作者+标题阈值（默认 0.7） */
    private double duplicateAuthorTitleThreshold = 0.7;
    /** 缺口核心书 Top-N（默认 50） */
    private int gapCoreBookTopN = 50;
    /** 缺口热度阈值（默认 0.4，WP-0：原 0.7 过苛仅"几乎全借出"算缺口，已放宽并配合 avail/total<0.4） */
    private double gapHeatThreshold = 0.4;
    /** 缺口覆盖率阈值（默认 0.8） */
    private double gapCoverageThreshold = 0.8;
    /** 谈判历史月数（默认 24） */
    private int negotiationHistoryMonths = 24;
    /** 开学季因子（默认 1.3） */
    private double seasonExamFactor = 1.3;
    /** 假期因子（默认 0.6） */
    private double seasonVacationFactor = 0.6;
    /** 预测置信度基础衰减（默认 0.05） */
    private double predictionConfidenceDecay = 0.05;
    /** 数据不足时置信度惩罚（默认 0.2） */
    private double predictionDataScarcityPenalty = 0.2;
    /** 预约热度影响系数（默认 0.01） */
    private double predictionReservationHeatCoefficient = 0.01;
    /** 预测调整基础乘数（默认 1.1） */
    private double predictionBaseMultiplier = 1.1;
    /** 周转率建议除数为 50 */
    private int predictionCopiesDivisor = 50;
}
