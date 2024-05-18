package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author limei
 * @date 2024/5/18 12:43
 * @description
 */
public class SimpleRedisLock implements ILock{

    private String name;    // 业务名称，也是锁的名称，每个锁都不一样
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean tryLock(Long timeoutSec) {
        //获取线程标示，用于释放锁时候判断
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //获取锁,这里已经之间返回是否设置成功了，不需要再次判断
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);    //拆箱要注意空指针
    }

    @Override
    public void unLock() {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //获取锁中的表标识
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        //判断是否一致，一致则释放
        if (threadId.equals(id)) {
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
