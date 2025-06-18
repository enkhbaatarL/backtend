-- KEYS[1] = chatbot:token-usage:company:%s(Integer):%s(yyyyMM)
-- ARGV[1] = delta  (증가량)
-- ARGV[2] = limit  (월 한도)

local delta = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

local new = redis.call('INCRBY', KEYS[1], delta)

return new
