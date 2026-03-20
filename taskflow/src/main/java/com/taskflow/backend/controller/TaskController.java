package com.taskflow.backend.controller;

// ✨ 重点：我帮你把所需的核心包都补全了
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskflow.backend.entity.Result;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.service.TaskService;
import com.taskflow.backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // 1. 查询列表：只查当前登录人的任务
    @GetMapping("/list")
    public Result<List<Task>> list(@RequestHeader("token") String token) {
        Long userId = JwtUtils.getUserId(token);
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return Result.success(taskService.list(wrapper));
    }

    // 2. 新增任务：自动绑定当前用户
    @PostMapping("/add")
    public Result<String> addTask(@RequestBody Task task, @RequestHeader("token") String token) {
        Long userId = JwtUtils.getUserId(token);
        task.setUserId(userId);
        task.setBoardId(1L);
        task.setStatus(0);
        taskService.save(task);
        return Result.success("新增成功");
    }

    // 3. 修改任务：(比如点击 开始制作、完成 等按钮)
    @PostMapping("/update")
    public Result<String> updateTask(@RequestBody Task task) {
        taskService.updateById(task);
        return Result.success("更新成功");
    }

    // 4. 删除任务
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteTask(@PathVariable Long id) {
        taskService.removeById(id);
        return Result.success("删除成功");
    }
}