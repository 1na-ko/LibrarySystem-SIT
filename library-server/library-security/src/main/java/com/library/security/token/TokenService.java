package com.library.security.token;

/**
 * Token 存储与撤销服务.
 * <p>
 * 管理两类 Token 的撤销机制：
 * <ul>
 *   <li><b>Refresh Token</b>：每用户当前有效 jti 存 Redis（{@code auth:refresh:{userId}}），
 *       支持登录覆盖、刷新轮换（防重放）、登出撤销</li>
 *   <li><b>Access Token</b>：无状态不落库，但通过 user 维度 logout 时间戳
 *       （{@code auth:logout:{userId}}）实现登出后立即失效——AT 的 {@code iat} 早于
 *       登出时间戳即视为已撤销，由 {@code JwtAuthenticationFilter} 校验</li>
 * </ul>
 * Redis 操作全部收敛至此接口，便于单元测试 mock、未来替换为真实集成测试。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface TokenService {

    /**
     * 登录/注册成功后存储当前 RT 的 jti（覆盖式，旧 RT 立即失效）.
     *
     * @param userId 用户 ID
     * @param jti    新签发 RT 的 jti
     */
    void storeRefresh(long userId, String jti);

    /**
     * 消费旧 RT 并预占新 RT 的 jti（原子操作）.
     *
     * @param userId      用户 ID
     * @param expectedJti 请求携带的旧 RT 的 jti
     * @param newJti      即将签发的新 RT 的 jti
     * @return 0=正常轮换成功；1=重放检测命中（旧 RT 已失效或被复用）
     */
    int rotate(long userId, String expectedJti, String newJti);

    /**
     * 登出：删除当前 RT 记录（旧 RT 立即失效）+ 记录登出时间戳（旧 AT 立即失效）.
     *
     * @param userId 用户 ID
     */
    void revoke(long userId);

    /**
     * 校验指定 AccessToken 的签发时间是否早于该用户最近一次登出（即 AT 是否已失效）.
     * <p>
     * 项目无状态 JWT 设计下 AT 不带 jti，故用 user 维度的 logout 时间戳兜底——只要 AT 的
     * {@code iat} ≤ 登出时间戳，即视为已撤销。粒度为秒（与 JWT iat 精度一致）。
     *
     * @param userId         用户 ID（来自 AT subject）
     * @param iatEpochSeconds AT 的 issued-at（epoch 秒）
     * @return true=已被登出失效；false=仍有效（或该用户从未登出）
     */
    boolean isAccessTokenLoggedOut(long userId, long iatEpochSeconds);
}
