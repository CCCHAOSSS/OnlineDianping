package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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

    private static final DefaultRedisScript<Long> UNLOKC_SCRIPT;
    static {
        UNLOKC_SCRIPT = new DefaultRedisScript<>();
        UNLOKC_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOKC_SCRIPT.setResultType(Long.class);
    }

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


//    public void unLock() {
//        //获取线程标识
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//
//        //获取锁中的表标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//
//        //判断是否一致，一致则释放
//        if (threadId.equals(id)) {
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }

    /**
     * 基于Lua脚本释放锁
     * */
    @Override
    public void unLock(){
        //调用Lua脚本
        stringRedisTemplate.execute(
                UNLOKC_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
