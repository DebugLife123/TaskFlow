package com.taskflow.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 加入下面这行代码，开启 Mapper 扫描雷达（注意包名要和你自己的一致哦）
@MapperScan("com.taskflow.backend.mapper")
public class TaskflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskflowApplication.class, args);
    }

}
