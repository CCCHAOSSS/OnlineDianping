---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by CHAOS.
--- DateTime: 2024/5/19 15:21
---

-- 1.参数列表
--1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2.获取用户id
local userId = ARGV[2]
-- 1.3.获取订单id
local orderId = ARGV[3]


--2.数据key
--2.1库存key
local stockKey = 'seckill:stock:' .. voucherId
--2.2.秒杀订单key
local orderKey = 'seckill:order:' .. voucherId

--3.脚本业务
--3.1判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
   --库存不足，返回1
    return 1
end

--3.2判断用户是否重复下单
if (redis.call('sismember', orderKey, userId) == 1) then
    --重复下单，返回2
    return 2
end

--3.3库存充足，且用户未下单，库存-1，下单
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)

--`3.4 发送消息到队列中 XADD STREAM.orders * K1 V1 K2 V2
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
--orderId直接用id，对应实体类中订单id（是直接用id）

return 0