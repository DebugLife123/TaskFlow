# 🚀 TaskFlow Kanban (敏捷任务看板系统)

![Vue3](https://img.shields.io/badge/Vue.js-3.0-4FC08D?logo=vue.js)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-6DB33F?logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)
![License](https://img.shields.io/badge/License-MIT-blue)

## 📖 项目简介
TaskFlow 是一款基于前后端分离架构开发的轻量级敏捷任务管理工具。它旨在提供高效的个人/团队任务状态追踪与流转功能，具有多租户数据隔离、安全鉴权及直观的优先级视觉交互体验。

本项目为全栈独立开发实战项目，打通了从数据库设计、后端 API 搭建、鉴权拦截到前端响应式渲染与跨域处理的完整闭环。

## ✨ 核心功能亮点
- **🔐 完整安全闭环 (JWT)**：
  - 后端基于拦截器（Interceptor）实现统一 Token 校验，拦截非法请求。
  - 前端基于 Vue Router 路由守卫与 Axios 请求拦截器，实现无感携牌与无权拦截。
- **🛡️ 多用户数据隔离**：
  - 动态解析 JWT Token 获取用户身份，在底层 SQL 层面实现“我的任务仅我可见”，具备 SaaS 系统基础的多租户架构雏形。
- **🎨 敏捷流转与视觉增强**：
  - 支持任务在 TODO (待办)、DOING (进行中)、DONE (已完成) 三种状态间无缝流转。
  - 引入**任务优先级**机制（普通/中等/紧急），前端根据优先级动态渲染 Element Plus 彩色标签，视觉主次分明。
- **⚡ 丝滑的 CRUD 体验**：
  - 前后端数据交互采用统一的 `Result` 泛型封装格式，状态码及提示信息规范清晰。

## 🛠️ 技术栈
- **前端**：Vue 3 (Composition API)、Vite、Element Plus、Axios、Vue Router
- **后端**：Spring Boot 3、MyBatis-Plus、Java JWT、MySQL
- **架构**：RESTful API 设计规范、前后端分离、CORS 跨域处理

## 📸 项目截图
*(💡 提示：将你的登录页、带彩色标签的看板页截图放在项目的 `docs/images/` 目录下，并在这里引入链接)*
- [登录页面截图](./docs/images/login.png)
- [敏捷看板截图](./docs/images/board.png)

## 🚀 快速开始 (本地运行)

### 1. 环境准备
- JDK 17+
- Node.js 16+
- MySQL 8.0+

### 2. 数据库初始化
在 MySQL 中创建数据库 `taskflow`，并执行以下 SQL 脚本建立表结构：
```sql
-- 1. 创建用户表
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建任务表
CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text,
  `status` int DEFAULT '0' COMMENT '0-待办, 1-进行中, 2-已完成',
  `board_id` bigint DEFAULT '1',
  `user_id` bigint COMMENT '所属用户ID',
  `priority` int DEFAULT '1' COMMENT '1-普通, 2-中等, 3-紧急',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 插入测试数据
INSERT INTO `user` (username, password, nickname) VALUES ('admin', '123456', '管理员');