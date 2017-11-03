---
title: 《java并发编程实战》第三章：发布对象
date: 2017-11-03 14:39:32
tags:
---

第二章主要介绍了什么是线程安全，以及怎么检测一个类到底是不是线程安全的，从一个实例引出线程不安全的情况，并且怎么用synchronized关键字来保证线程安全。通读本章全文，发现其围绕一个核心主题就是“发布对象”（sharing object）。怎么在多线程的环境下，正确的发布一个对象，来达到想要的目的。下面来详细介绍这方面的问题。
<!-- more -->
***
**可见性**
通过[ <font color=green> 《java并发编程实战》之java内存模型 </font> ](http://aworker.cn/2017/10/18/java-memory-model/)这篇文章的分析，我们知道多线程开发中，我们面对的主要挑战就是“可见性”、“原子性”、“有序性”，三个方面。而其中“可见性”往往是最让新手感觉“理所应当”但其实是“来之不易”的一种特性。我们再来看看书中引出可见性问题的例子：
```
public class NoVisibility{
	private static boolean ready; 
	private static int number;
	private static class ReaderThread extends Thread{
		public void run(){
			while(!ready)
				Thread.yield();
				System.out.println(number);
		}
	}

	public static void main(String[] args){
		new ReaderThread().start();
		number = 42;
		ready = true;
	}
}
```
观察NoVisibility的代码，这段代码有两个线程，有一个调用main方法的线程，我们暂叫它main线程，还有一个在main方法里面启动的read线程，代码表达的逻辑也很简单，mian线程先修改number的值，然后把ready变量赋值为true告诉read线程number的值可以使用了，然后再read线程中打印出来，number的值，这个number的值，我们期望是main线程赋值以后的最新值42。读过[ <font color=green> 《java并发编程实战》之java内存模型 </font> ](http://aworker.cn/2017/10/18/java-memory-model/)这篇文章的人知道，这样写代码是问题的，read线程可能一直停在while语句；也可能打印出的结果是0不是42。
这两种情况都不是我们想要的结果，一直停在while语句处是因为read线程没有看到main线程对ready的修改，所以一直不能去执行“System.out.println(number)”这句，这是内存可见性问题。打印结果是0而不是42是因为发生了指令重排序导致导致main线程先执行了对number的赋值操作然后执行了对ready的赋值操作，read线程看到赋值后的ready变量后打印number变量，但是这时候对read线程并没有看到number的最新值42而去打印了number的默认值0。
我们可以通过添加锁来解决此问题：
```
public class LockVisibility  {
    private static boolean ready;
    private static int number;
    private static class ReadThread extends Thread{
        @Override
        public void run() {
            synchronized (NoVisibility.class) { //lock
                while(!ready){
                    Thread.yield();
                }
                System.out.println(number);
            }
        }
    }

    public static void main(String[] args){
        new ReadThread().start();
        synchronized (NoVisibility.class) {  //lock
            number = 42;
            ready = true;
        }
    }
}
```
注意观察上述代码，其与NoVisibility类的区别就是在线程main和线程read操作两个共享变量ready和number时，都加上了同一把锁。这样就可以保证最后read线程的输出结果是42，这样也就引出了锁的除了可以保障操作“原子性”外另外一个特性，锁可以保障操作的“可见性”。这也就是[ <font color=green> java 8大happen-before原则超全面详解 </font> ](http://aworker.cn/2017/10/26/8-happen-before-principle/)中介绍的“锁的happen-before”原则。详细介绍可以移步到上述这篇文章观看。
如果单纯的想要保证“可见性”，我们还可以通过java提供的另外一个关键字volatile，来保证read线程打印的结果是42，如下面的代码：
```
public class VolatileVisibility {
    private static volatile boolean ready; //use volatile variable
    private static int number;

    private static class ReadThread extends Thread {
        @Override
        public void run() {
            while (!ready) {
                Thread.yield();
            }
            System.out.println(number);
        }
    }



    public static void main(String[] args) {
        new ReadThread().start();
        number = 42;
        ready = true;
    }
}
```
VolatileVisibility实现可见性的方式是通过将其中的ready变量声明为volatile的变量,通过[ <font color=green> java 8大happen-before原则超全面详解 </font> ](http://aworker.cn/2017/10/26/8-happen-before-principle/)这篇文章介绍的“volatile的happen-before原则”和“可传递的happen-before原则”可知道。read线程最终打印的结果只能是42一种情况。这种可见性实现的方式量级比较轻，相较于用锁的方式吃的资源更少。但是需要注意的是volatile变量不能保证“操作的原子性”，而且过多的使用volatile变量来保证“操作的可见性”会在并发代码的实现上和后期维护上带来不小的问题。所以尽量少用volatile变量。
***
**对象泄露**
来看下面一段代码：
```
class UnsafeStates {
	private final String[] states = { "active","sleep","dead"};

	public String[] getStates(){
		return states;
	}
}
```
任何可以调用到getStates方法的线程，都可以改变states变量中的值，这就违背了把states定义成private变量的初衷了。还有一种情况是在类的构造方法中启用一个线程，如果在构造方法启动的线程中用到了此对象的任何成员变量或者方法，都可能导致异常情况。因为:
>An object is in a predictable, consistent state only after its constructor returns.

如果没有等构造方法返回一个实例对象就用此对象的成员变量和成员方法，很有可能看到的成员变量处在一个非稳定的状态。不要在构造方法中启动一个线程，这在发布对象的时候尤其要注意。
***
**对象的线程限制**
对象的线程限制讨论的是如何把某个对象的所有权限制为特定的线程。比如变量var只属于线程1，其它线程不能看到或者得到此变量。java中有三种方式来实现这种要求。分别是“Ad-hoc thread confinement”、“stack confinement”和“ThreadLocal 类的使用”。
* “Ad-hoc thread confinement”：说白就是利用各种java以后的方式来限制特定对象的访问权限，来达到只对特定线程可用的目的。java的GUI就是用这种方式设计实现的。这种方式非常脆弱，需要考虑的非常周详，才能实现要求。
* “stack confinement” ： 栈限制，这种方式是java自带的特性，比如下面的代码：
```
public void method1(int var){
	int a = var;
	Object obj = new Object();
}
```
如果线程1调用了method1方法，其中的变量a和obj指向的对象永远不会被其他线程看到或者获得，如果又有新的线程2调用了method1方法，那么线程2对a变量的复制不会影响到线程1对a变量的复制。
* “ThreadLocal的使用” ：ThreadLocal 类可以被理解成是一种特殊的Map，其key是线程实例自身，value是其想要独有的变量。具体使用方法大家可以去自行google。

