package com.hmdp.utils;

/**
 * @author limei
 * @date 2024/5/18 12:42
 * @description 分布式锁接口
 */
public interface ILock {

    /**
     * 尝试获取锁
     * */
    boolean tryLock(Long timeoutSec);


    /**
     * 释放锁
     * */
    void unLock();
}
