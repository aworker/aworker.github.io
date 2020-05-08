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

![代码结构图](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/spring/post1/spring_post1_code_struct.png)

前前后后一共使用了四个类，其中两个Bean类如下：

```aidl
@Component
public class Chicken {
    @Autowired
    Egg egg;
}

```

```aidl
@Component
public class Egg {
    @Autowired
    Chicken chicken;
}
```

一个配置类：

```aidl
@ComponentScan("spring.post1.beans")
public class Config {

}
```

一个简单的main方法启动类：

```aidl
public class DemoSpringCircularDependencies {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Config.class);
        Chicken chicken = ac.getBean("chicken", Chicken.class);
        System.out.println(chicken);
    }
}
```

通过代码可以看出，本章主要讨论下spring怎么解决基于@AutoWired注解的Bean的循环依赖问题。

# 2、前置知识需求

+ 学习本文前需要对spring的基于注解的bean管理配置方式有基本的了解，不然看不懂上述4个类的作用，那么就无从谈及学习spring源码了，本系列的文章也不是基本的spring配置学习文章，这部分知识自行google。






# 参考资料

[1、Spring Circular Dependencies](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-dependency-resolution)