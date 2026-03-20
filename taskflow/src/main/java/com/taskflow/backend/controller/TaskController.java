package com.taskflow.backend.controller;

import com.taskflow.backend.entity.Result;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/tasks") // 给所有任务接口加一个统一的路径前缀
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/list")
    public Result<List<Task>> list() {
        // 调用 service 层的方法
        return Result.success(taskService.list());
    }

    // 👇 下面是我们需要新增的方法     后端提供接收数据的接口
    @PostMapping("/add")
    public Result<String> addTask(@RequestBody Task task) {
        // 为了简化，我们暂时把新任务都默认挂在 ID 为 1 的看板下
        task.setBoardId(1L);
        // 新增的任务，默认状态肯定是“待办”(0)
        task.setStatus(0);

        // 调用 Service 层的自带方法保存到数据库
        taskService.save(task);

        return Result.success("新增成功");
    }

    // 👇 新增：修改任务接口 (前端把改好状态的 task 传过来，我们覆盖掉数据库里的)
    @PostMapping("/update")
    public Result<String> updateTask(@RequestBody Task task) {
        // updateById 是 MyBatis-Plus 自带的神级方法，自动根据 ID 更新其他变化的字段
        taskService.updateById(task);
        return Result.success("更新成功");
    }

    // 👇 新增：删除任务接口 (前端传一个 ID 过来，我们把它删了)
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteTask(@PathVariable Long id) {
        // removeById 也是 MyBatis-Plus 自带的
        taskService.removeById(id);
        return Result.success("删除成功");
    }
}