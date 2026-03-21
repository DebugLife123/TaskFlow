package com.taskflow.backend.controller;
import org.springframework.beans.factory.annotation.Autowired;
import com.taskflow.backend.mapper.UserMapper;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.utils.JwtUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import com.taskflow.backend.entity.Result;

@RestController

@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserMapper userMapper;
    // ✨ 1. 头像上传接口 (完整持久化版)
    @PostMapping("/upload-avatar")
    // 💡 注意这里：参数里必须加上 @RequestHeader("token") String token，才能知道是谁在上传！
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestHeader("token") String token) {
        if (file.isEmpty()) {
            return error(400,"文件不能为空");
        }

        try {
            // 1. 获取原始文件名并提取后缀名 (例如 .png)
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 2. 使用 UUID 生成一个不重复的新文件名，防止别人上传同名文件覆盖你的
            String newFileName = UUID.randomUUID().toString() + ext;

            // 3. 确定保存的物理路径 (存在当前项目根目录下的 uploads/avatars 文件夹)
            String dirPath = System.getProperty("user.dir") + "/uploads/avatars/";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs(); // 如果文件夹不存在，就自动创建
            }

            // 4. 将前端传来的文件真正保存到这台电脑的硬盘上
            file.transferTo(new File(dir, newFileName));

            // 5. 拼接出网络图片 URL
            String avatarUrl = "http://localhost:8080/uploads/avatars/" + newFileName;

            // ✨ ✨ ✨ 核心更新：把头像持久化到数据库 ✨ ✨ ✨
            Long userId = JwtUtils.getUserId(token);
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    user.setAvatar(avatarUrl); // 把 URL 存入实体类
                    userMapper.updateById(user); // 真正执行数据库更新！
                }
            } else {
                return error(401, "登录已过期，请重新登录");
            }

            // 6. 把 URL 返回给前端
            return Result.success(avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return error(500,"文件上传失败");
        }
    }
    // 内部快捷方法，用于返回错误信息
    private Result<String> error(int code, String msg) {
        Result<String> r = new Result<>();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }
    // ✨ 2. 修改密码接口
    @PostMapping("/update-password")
    public Result<String> updatePassword(@RequestHeader("token") String token,
                                         @RequestBody java.util.Map<String, String> params) {

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 1. 通过 Token 解析出当前用户的 ID
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return error(401, "登录已过期，请重新登录");
        }

        // 2. 去数据库查出这个用户的真实信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return error(404, "找不到该用户");
        }

        // 3. 核心校验：比对旧密码是否正确 (根据你之前的截图，这里是明文比对)
        if (!user.getPassword().equals(oldPassword)) {
            return error(400, "原密码输入错误，请重试");
        }

        // 4. 旧密码正确，设置新密码并更新到数据库
        user.setPassword(newPassword);
        userMapper.updateById(user);

        // 5. 返回成功信息
        return Result.success("密码修改成功");
    }
    // ✨ 3. 修改基本信息接口 (修改昵称)
    @PostMapping("/update-info")
    public Result<String> updateInfo(@RequestHeader("token") String token,
                                     @RequestBody java.util.Map<String, String> params) {
        // 从前端传来的数据中拿到新昵称
        String newNickname = params.get("nickname");

        if (newNickname == null || newNickname.trim().isEmpty()) {
            return error(400, "昵称不能为空");
        }

        // 1. 解析 Token 获取当前用户 ID
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return error(401, "登录已过期，请重新登录");
        }

        // 2. 查出数据库里的该用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return error(404, "找不到该用户");
        }

        // 3. 更新昵称并保存到数据库
        user.setNickname(newNickname);
        userMapper.updateById(user);

        // 4. 返回成功
        return Result.success("信息修改成功");
    }
}