---
title: 《java并发编程实战》第5章：构建阻塞
date: 2017-11-16 11:36:00
tags: java并发
---

阻塞（blocks）对于初学者来说可能有些太陌生，但是只要接触过java并发的就肯定接触过阻塞。如果我们对某个方法使用锁，我们就是在运用阻塞。如果线程1持有了锁a，那么直到线程1释放锁a前，线程2一直在等待锁，其一直都处在阻塞状态。这就是阻塞（现象），而锁就是阻塞类。在java并发中，阻塞机制是十分有必要的。
<!-- more  -->
***
阻塞是线程间通信的重要机制，它用来控制线程间的执行顺序。而java中用各种阻塞类来实现阻塞机制，在java中凡是能让用来控制线间程执行顺序的类，就是阻塞类。比如ReentantLock、Latch、FutureTask、Semaphores等，也包含各种并发容器类，如ConcurrentHashMap、CopyOnWriteList等。因为线程在调用它们的某些方法后，就可能进入阻塞状态。比如线程1调用ReentantLock.lock方法，当有其它线程已经调用了同一个ReentantLock对象的lock方法，那么直到其调用unklock方法前，线程1一直处在阻塞状态；比如线程1调用ConcurrentHashMap的get方法时，如果有其它线程已经调用了此同一个ConcurrentHashMap对象的get方法并且没有获得返回值前，线程1就**可能**处在阻塞状态。注意这里是“可能”不是一定，因为ConcurrentHashMap使用的是分段锁，具体可以google一下。
阻塞机制是十分有必要的。生产者-消费者模式就是阻塞最常见的一种应用，假设有n个厨师生产面包，m个客人吃面包，每个厨师面包的生产速度是随机的，客人吃完一个面包的速度也是随机的。在java中BlockingQueue可以很好的实现这种需求，当我们构建一个容量有限的BlockingQueue时候，如果queue中的面包数量为0，那么客人线程就会被阻塞；而如果queue中的面包达到其最大容量的时候厨师线程就会被阻塞。本文我们将设计一个应用java阻塞类实现的高效缓存系统，来加深对阻塞的理解。
***
**多阶段设计线程安全的缓存系统**
公司有一个“time-honored”的类叫ExpensiveCompution，其通过一个方法可以传入一个字符串，返回一个段数字，其代码如下：
```
public class ExpensiveCompution {

    public Long compute(String string) {
        Long along = new Long(0);
        /**
         * 利用string计算出一个长整型的数复制给along
    	 * 方法比较耗时间	
         */
        return  along;
    }
}
```
现在公司要求你设计个缓存系统来改善下性能，要求缓存系统线程安全。

***
第一版
```
public class Cache1 {
    ExpensiveCompution computer;
    private Map<String,Long> map = new HashMap<String,Long>();

    public Cache1(ExpensiveCompution c) {
        this.computer = c;
    }

    public synchronized Long compute(String string) {
        Long aLong = map.get(string);
        if (aLong == null) {
            aLong = computer.compute(string);
            map.put(string, aLong);
        }

        return aLong;
    }
}
```
大家都可以不加思索的设计出这这个版本，但是这个版本在并发效率上是非常低的，在多线程环境下，有时候Cache1类反而可能成为累赘。具体如下图所示。
