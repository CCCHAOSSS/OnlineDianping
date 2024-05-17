package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回错误消息
            return Result.fail("手机号格式错误");
        }
        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);  //长度为6的验证码

        //4.保存验证码到session
        session.setAttribute("code",code);

        //5. 发送验证码---》不具体实现了，一般会有具体服务去做这块
        log.debug("发送验证码成功，验证码为：{}",code);

        //6. 返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回错误消息
            return Result.fail("手机号格式错误");
        }
        //2.校验验证码
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();  //前端提交过来的
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            //3.不一致，返回错误消息
            return Result.fail("验证码错误");
        }

        //3.一致则根据手机号查询用户（手机号是唯一的）
        User user = query().eq("phone", phone).one();//查一个就是one
        //4.判断用户是否存在
        if (user == null){
            // 5.不存在则创建新用户
            user = createUserWithPhone(phone);
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setIcon(user.getIcon());
        userDTO.setNickName(user.getNickName());
        userDTO.setId(user.getId());
    // 6.存在保存用户信息到session
        session.setAttribute("user",userDTO);

//        session.setAttribute("user",user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));

        // 2.保存用户
        save(user);
        return user;
    }
}
