package com.taskflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskflow.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring 这是一个 Mapper 接口
public interface UserMapper extends BaseMapper<User> {
}