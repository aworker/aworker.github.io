---
title: 《java并发编程实战》之java内存模型
date: 2017-10-18 15:28:49
tags: java并发
---
> “如欲征服java并发，需先征服java内存模型，如欲征服java内存模型，需先征服计算机内存模型” -aworker.

![大佬讲话](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java-memory-model/leader_speak.jpg)
咳！咳！，大家都记好笔记了吧。虽然我不是什么大佬，但是这句话说的还是没有毛病的。不了解java的内存模型，就不会从跟不上理解java并发的一些行为和机制，而java内存模型毕竟是jvm模拟出来的一部分，其底子还是建立在现代计算机的物理内存模型上来的，所以我们就按照现代计算机的物理内存模型、java内存模型的顺序来仔细介绍，为彻底了解java并发机制打下底子。
1. 现代计算机的物理内存模型：
