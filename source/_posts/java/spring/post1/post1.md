---
title: 01 spring解决循环源码分析
date: 2020-05-08 16:46:13
categories:
    - spring
    - spring源码剖析系列
tags:
---

# 1、什么是循环依赖

循环依赖在spring框架中有一个专有名词叫 *Circular dependencies*，其具体是指受spring管理的两个bean对象 Bean1和Bean2，Bean1中有成员变量Bean2；Bean2中有成员变量Bean1。具体代码case如下：

代码结构如图：






# 参考资料

[1、Spring Circular Dependencies](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-dependency-resolution)