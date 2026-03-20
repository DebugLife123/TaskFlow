package com.taskflow.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskflow.backend.entity.Result;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.mapper.UserMapper;
import com.taskflow.backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result<String> login(@RequestBody User loginUser) {
        // 1. 在数据库中查找该用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginUser.getUsername());
        User user = userMapper.selectOne(wrapper);

        // 2. 校验逻辑
        if (user == null) {
            return error(401, "用户不存在");
        }

        // 3. 校验密码（目前比对明文，这是最基础的写法）
        if (!user.getPassword().equals(loginUser.getPassword())) {
            return error(401, "密码错误");
        }

        // 4. 登录成功，利用 JwtUtils 生成 Token
        String token = JwtUtils.createToken(user.getId());
        return Result.success(token);
    }
    // ✨ 新增：用户注册接口
    @PostMapping("/register")
    public Result<String> register(@RequestBody User registerUser) {
        // 1. 检查用户名是否已经被别人注册了
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", registerUser.getUsername());
        User existUser = userMapper.selectOne(wrapper);

        if (existUser != null) {
            return error(400, "该用户名已被注册，请换一个");
        }

        // 2. 注册新用户 (为了简单，昵称默认等于用户名)
        if (registerUser.getNickname() == null || registerUser.getNickname().isEmpty()) {
            registerUser.setNickname(registerUser.getUsername());
        }

        // 3. 插入数据库
        userMapper.insert(registerUser);

        return Result.success("注册成功");
    }

    // 内部快捷方法，用于返回错误信息
    private Result<String> error(int code, String msg) {
        Result<String> r = new Result<>();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("token") String token) {
        Long userId = JwtUtils.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null); // 🛡️ 安全第一：绝对不能把密码发给前端
            return Result.success(user);
        }
        return Result.success(null);
    }
}