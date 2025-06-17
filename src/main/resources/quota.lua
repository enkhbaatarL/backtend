-- KEYS[1] = chatbot:token-usage:company:%s(Integer):%s(yyyyMM)
-- ARGV[1] = delta  (증가량)
-- ARGV[2] = limit  (월 한도)

local delta = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

local new = redis.call('INCRBY', KEYS[1], delta)

-- -- 현재 누적량이 한도를 초과하는지 확인
-- if new > limit then
--     return {0, new} -- 0 = 거부, 현재 누적량
-- end
--     return {1, new} -- 1 = 허용, 현재 누적량

return new

-- todo 반환값이 이상하다...
