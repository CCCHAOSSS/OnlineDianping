package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author limei
 * @date 2024/5/17 10:39
 * @description 用户拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {


    // 登录校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.判断是否需要拦截
        if (UserHolder.getUser() == null) {
            //2.没有，需要拦截，返回401
            response.setStatus(401);
            //拦截
            return false;
        }
        //有用户
        return true;
    }
}
