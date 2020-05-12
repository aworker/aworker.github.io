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

+ 需要对jdk8的lambda有基础的了解。

# 3、源码分析

## 3.1、源码栈帧
首先我们先看下需要分析的源码的主要栈帧：

![源码栈帧图](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/spring/post1/bean_create_process_of_CD.jpg)

先对上图做简单的说明，上图中的每蓝色小块代表一个方法，里面的数字部分表示方法的执行先后顺序（数字小的先执行）。两个相邻的方法之间大数字方法是程序在执行小数字方法的过程中要调用的方法（和debug时的的栈信息类似）。我们对源码的分析也将按照“创建所有单例Bean”，“创建Chicken对象”，“填充Chicken对象属性”，“创建Egg对象”，“填充Egg对象属性”，“获取Chicken对象”的顺序进行。

## 3.2、 创建所有单例Bean

方法1. 是AnnotationConfigApplicationContext类的构造方法，构造方法引出对Bean的初始化创建操作。其中可以留意下



# 4、 总结

+ spring之所以默认是前置加载的正如其








# 参考资料

[1、Spring Circular Dependencies](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-dependency-resolution)