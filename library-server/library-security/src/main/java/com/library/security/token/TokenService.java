package com.library.security.token;

/**
 * Refresh Token 存储服务.
 * <p>
 * 将每个用户当前有效的 Refresh Token jti 存入 Redis，支持登录覆盖、刷新轮换（防重放）、登出撤销。
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
     * 登出：删除当前 RT 记录（旧 RT 立即失效）.
     *
     * @param userId 用户 ID
     */
    void revoke(long userId);
}
