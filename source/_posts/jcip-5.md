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
大家都可以不加思索的设计出这这个版本，但是这个版本在并发效率上是非常低的，在多线程环境下，有时候Cache1类反而可能成为累赘。具体如下图所示：
![低并发的Cache1](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/jcip-5/Cache1.png)
当有线程1，线程2，线程3分别同时执行计算字符串"1"、"2"、"3"返回的值时，因为Cache1为了保证线程安全性，其用了synchrnozied关键字。这使得同一时间只能由一个线程调用Cache1.compute方法，如果把cache不能命中时Cache1.compute方法的执行时间设为一个单位时间。那么三个线程平均用时为2个单位时间（（1+2+3）/3 = 2),Cache1缓存的引用在某些情况下甚至起到了负作用，因为即使不用缓存直接使用ExpensiveCompution.compute方法，其线程的平均用时也只有一个单位时间。这肯定需要改善。
***
第二版
分析第一版，之所以会在某些情况下让线程平均等待时间更长，是因为Cache1.compute方法把耗时很长的ExpensiveCompute.compution方法放在锁的里面，错误的锁的范围扩大了。启发下设计Cache2类如下：
```
public class Cache2 {
    ExpensiveCompution computer;
    private Map<String,Long> map = new HashMap<String,Long>();

    public Cache2(ExpensiveCompution c) {
        this.computer = c;
    }

    public Long compute(String string) {
        Long aLong;
        synchronized(this){                    //1
           aLong = map.get(string);            //1
        }                                      //1
        if (aLong == null) {
            aLong = computer.compute(string);
            synchronized (this) {              //2
                map.put(string, aLong);        //2
            }                                  //2
        }

        return aLong;
    }
}
```
这样如果有线程1、线程2、线程3同时调用Cache2.compute方法分别计算"1"、"2"、"3"对应的返回值时会有如下情况：
![Cache2较Cache1的优势](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/jcip-5/Cache2.png)
图中的"//1","//2"分别对应CaChe2类中对应的"//1","//2"部分。线程2、线程3也会阻塞，但其时间不会像用Cache1时那么长，如果假设"//1"处代码的执行时间为0.1个单位时间，那么三个线程的平均用时为1.1个单位时间((1+1.1+1.2)/3=1.1)。实时上代码"//1"处的用时远远不能达到0.1个单位时间这么长，三个线程的平均用时随着ExpensiveCompution.compute的执行时间和"//1"的执行时间的比值的增大而减少，即平均用时将近1个单位时间,这就是非常大的进步。但是其代码还有如下图的缺点存在：
![Cache2任然存在的缺点](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/jcip-5/Cache2.png)

