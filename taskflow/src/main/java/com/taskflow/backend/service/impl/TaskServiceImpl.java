package com.taskflow.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.mapper.TaskMapper;
import com.taskflow.backend.service.TaskService;
import org.springframework.stereotype.Service;

@Service // 必须加这个注解，否则 Controller 找不到它
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
}