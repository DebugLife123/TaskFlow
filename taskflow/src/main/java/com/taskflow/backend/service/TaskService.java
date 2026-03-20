package com.taskflow.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taskflow.backend.entity.Task;

// 继承 IService，MyBatis-Plus 会自动注入 30 多个常用的 CRUD 方法
public interface TaskService extends IService<Task> {
}