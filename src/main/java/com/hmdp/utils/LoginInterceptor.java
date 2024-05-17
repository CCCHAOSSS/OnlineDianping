package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author limei
 * @date 2024/5/17 10:39
 * @description 用户拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    // 登录校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取session
        HttpSession session = request.getSession();
        //2.获取session中的用户信息
        Object user = session.getAttribute("user");

        if (user == null) {
            //3.用户未登录(不存在)则拦截
            response.setStatus(401);
            return false;
        }

        //4.存在，保存用户信息到ThreadLocal----->这里UserHolder自己写的，里面会调用ThreadLocal等
        UserHolder.saveUser( (UserDTO) user);
        return true;
    }

    //业务执行结束后销毁用户信息，避免内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
