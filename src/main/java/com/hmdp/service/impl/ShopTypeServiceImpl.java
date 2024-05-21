package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopTypeMapper shopTypeMapper;


    public List<ShopType> getAllShopTypes() {
        QueryWrapper<ShopType> queryWrapper = new QueryWrapper<>();
        return shopTypeMapper.selectList(queryWrapper);
    }

    @Override
    public List<ShopType> queryTypeList() {
        //TODO： 使用redis存储List的方式进行缓存

        // 1.从redis查询店铺类型
        String key = "cache:shop:type";
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        //2.1 存在则直接返回
        if (StrUtil.isNotBlank(shopTypeJson)) {
//            List<ShopType> shopTypes = stringRedisTemplate.opsForList().range(key, 0, -1).stream().map(type -> {
//                return BeanUtil.toBean(type, ShopType.class);
//            }).collect(Collectors.toList());
            List<ShopType> shopTypes = JSONUtil.toList(shopTypeJson, ShopType.class);
//            return toList(shopTypeJson, ShopType.class);
            return shopTypes;
        }

        if (shopTypeJson != null) {
            // 说明不是null，是空值
            return null;
        }

        //2.2 不存在，查询数据库
        List<ShopType> shopTypes = getAllShopTypes();

        //3.将数据保存到redis[同时考虑缓存穿透问题]
        if (shopTypes == null) {
            stringRedisTemplate.opsForValue().set(key, "");
            return null;
        }
        //4.将结果存入redis，要先序列化
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypes), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //4.返回结果
        return shopTypes;
    }

//    public List<ShopType> toList(String shopTypeJson, Class<ShopType> shopTypeClass) {
//        // 分割字符串，得到JSON对象数组
//        String[] jsonObjects = shopTypeJson.split(",");
//
//        List<ShopType> shopTypes = new ArrayList<>();
//        for(String json : jsonObjects){
//            ShopType shopType = JSONUtil.toBean(json, shopTypeClass);
//            shopTypes.add(shopType);
//        }
//        return shopTypes;
//    }
}


