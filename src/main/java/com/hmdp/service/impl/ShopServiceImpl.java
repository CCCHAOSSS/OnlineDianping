package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>

 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透（存空值）
//        Shop shop = queryWithPassThrough(id);

        //通过redis SETNX 互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if(shop == null){
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }


    /**
     * 解决缓存穿透
     * */
    public Shop queryWithPassThrough(Long id){
        // 1.从redis查询店铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在则直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中是否空值
        if(shopJson != null){   // 空值是空字符串"",不等于null
            return null;
        }

        Shop shop = getById(id);
        //4.不存在则根据id查数据库
        if (shop == null){
            //【解决缓存穿透】将空值写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //5.数据库不存在则返回错误
            return null;
        }

        //6.数据库存在则存入redis,shop信息转为json格式
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;

    }

    /**
     * 获取锁
     * */
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);

    }

    /**
     * 释放锁
     * */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 解决缓存击穿
     * */
    public Shop queryWithMutex(Long id){
        // 1.从redis查询店铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在则直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中是否空值
        if(shopJson != null){   // 空值是空字符串"",不等于null
            return null;
        }

        //4.实现缓存重建
        //4.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否获取成功
            if(!isLock){
                //4.3失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功
            shop = getById(id);

            //5.不存在则根据id查数据库
            if (shop == null){
                //【解决缓存穿透】将空值写入redis
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                //5.数据库不存在则返回错误
                return null;
            }

            //6.数据库存在则存入redis,shop信息转为json格式
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            //7.释放锁
            unLock(lockKey);
        }
        return shop;

    }


    /**
     * 更新店铺数据
     * */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();

        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

        return Result.ok();
    }






}
