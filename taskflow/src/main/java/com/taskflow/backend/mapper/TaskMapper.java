package com.taskflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskflow.backend.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    // 继承了 BaseMapper 之后，你就自动拥有了对 Task 表的增删改查方法！一句 SQL 都不用写！
}