-- =============================================================================
-- 令牌桶限流（Redis 单线程执行保证原子性）
-- KEYS[1] = tokens key  (rl:{bucket}:tokens)
-- KEYS[2] = ts key      (rl:{bucket}:ts)
-- ARGV[1] = capacity         桶容量
-- ARGV[2] = refillRatePerSec 每秒补充令牌数（浮点）
-- ARGV[3] = nowMillis        Java 传入的当前毫秒时间戳（Lua 内禁用 os.time 以保纯函数）
-- ARGV[4] = requested        本次请求消耗令牌数（固定 1）
-- ARGV[5] = ttlSeconds       key TTL（窗口过期回收，默认 120s）
-- 返回: {allowed(0/1), remaining(int), resetEpochSecond(long)}
-- =============================================================================
local tokensKey = KEYS[1]
local tsKey = KEYS[2]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local nowMillis = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttlSeconds = tonumber(ARGV[5])

local nowSec = math.floor(nowMillis / 1000)

-- 读取上次状态（首次访问视为满桶）
local lastTokensStr = redis.call('GET', tokensKey)
local lastTsStr = redis.call('GET', tsKey)
local lastTokens = capacity
local lastTs = nowSec
if lastTokensStr then lastTokens = tonumber(lastTokensStr) end
if lastTsStr then lastTs = tonumber(lastTsStr) end

-- 按经过秒数线性补充令牌，上限为桶容量
local elapsed = math.max(0, nowSec - lastTs)
local currentTokens = math.min(capacity, lastTokens + elapsed * refillRate)

-- 计算到桶满（或下一令牌）的重置时间，用于 X-RateLimit-Reset
local resetSec
local deficit = capacity - currentTokens
if refillRate <= 0 then
    -- 无补充速率（纯定频桶）：reset 指向较远未来，避免除零
    resetSec = nowSec + ttlSeconds
elseif deficit <= 0 then
    resetSec = nowSec + math.ceil(1 / refillRate)
else
    resetSec = nowSec + math.ceil(deficit / refillRate)
end

-- 判定与扣减
local allowed = 0
local remaining = math.floor(currentTokens)
if currentTokens >= requested then
    allowed = 1
    remaining = math.floor(currentTokens - requested)
end

-- 回写状态（无论放行与否都记录最新消费时间点）
redis.call('SET', tokensKey, remaining)
redis.call('SET', tsKey, nowSec)
redis.call('EXPIRE', tokensKey, ttlSeconds)
redis.call('EXPIRE', tsKey, ttlSeconds)

return { allowed, remaining, resetSec }
