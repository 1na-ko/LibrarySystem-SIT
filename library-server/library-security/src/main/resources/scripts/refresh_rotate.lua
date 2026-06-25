-- =============================================================================
-- Refresh Token 原子轮换 + 重放检测
-- 保证并发下仅一个请求轮换成功，其余视为重放。
-- KEYS[1] = auth:refresh:{userId}
-- ARGV[1] = expectedJti（请求携带的 RT 的 jti）
-- ARGV[2] = newJti（即将签发的新 RT 的 jti）
-- ARGV[3] = ttlSeconds（7d）
-- 返回: 0=正常轮换成功 / 1=重放检测命中（key 不存在=已登出/已轮换；jti 不匹配=旧 RT 复用）
-- =============================================================================
local key = KEYS[1]
local expectedJti = ARGV[1]
local newJti = ARGV[2]
local ttl = tonumber(ARGV[3])

local stored = redis.call('GET', key)
if not stored then
    -- key 不存在：该 RT 已被消费（轮换时 jti 已更新）或已登出，现再次出现 → 重放
    return 1
end
if stored ~= expectedJti then
    -- jti 不匹配：旧 RT 在轮换后已失效，现复用 → 重放
    return 1
end

-- 匹配：消费当前 jti，写入新 jti，刷新 TTL（原子，并发安全）
redis.call('SET', key, newJti, 'EX', ttl)
return 0
