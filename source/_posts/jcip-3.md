---
title: 《java并发编程实战》第三章：发布对象
date: 2017-11-03 14:39:32
tags: java并发
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
***
**不变性**
不可变的对象永远是线程安全的。如果一个对象自打创建以后其状态不可以再被改变，那么不管是哪个线程使用这个对象，此对象都是线程安全的。那么什么是不可变的对象呢？不可变对象要满足如下三个条件：
1. 不可变对象的成员变量必须被声明成final类型的。
2. 不可变对象的成员变量在初始化以后就不会再改变。
3. 不可变对象在初始化的时候不会泄露this变量。

下面看一个例子：
```
public class Immutable {
	private final Set<String> set = new HashSet<String>();

	public Immutable(){
		set.add("started");
		set.add("sleep");
		set.add("suspend");
	}

	public Set<String> getStates(){
		Set<String> newSet = new HashSet<String>();
		newSet.addAll(set);
		return newSet;
	}
}
```
我们来看下Immutable对象是一个不可变对象，首先它满足第一个条件，其成员变量都用final来修饰了；第二个条件，我们可以发现虽然set是一个Set类型的容器，但是其变量被声明成private类型的，而且Immutable类的getStates方法拿到的是set的深度拷贝，返回的那个Set类型的容器的变化不会对原来的set中的内容造成任何影响，set变量只是在Immutable的构造法中传入了三个值，在Immutable构造方法完成后，set中的内容不会被改变，所以其满足第二条；第三个条件，我们发现在构造方法中this变量没有被泄露，所以此条件也满足。所以Immutable对象是不可变对象，其也就是线程安全的。
我们可以利用不可变对象是线程安全的这一特点来在多线程环境下实现不用锁的编程。如多线程环境下的一个简单的缓存系统设计：
```
class OneValue {
    private final Integer number;
    private final BigInteger[] result;

    public OneValue(Integer number, BigInteger[] result) {
        this.number = number;
        this.result = result;
    }

    public BigInteger[] getResult(int number){
        if ( this.number == null ||  number != this.number) {
            return  null;
        }

        return Arrays.copyOf(result, result.length); //1
    }
}

public class Cache {

    private volatile OneValue oneValue = new OneValue(null, null); //2
    public BigInteger[] getCache(Integer integer) {
        BigInteger[] result = oneValue.getResult(integer);
        if (result == null) {
            /**
             * result 值从硬盘中读取
             */
            oneValue = new OneValue(integer, result); //3
        }

        return  result;
    }

}
```
我们先看OneValue类，其实例化的对象都是不可变的对象，注意//1处的写法，没有直接返回result变量，而是对其进行深度拷贝，这样就保证getResult方法不会泄露result变量，从而避免其他线程通过getResult方法改变result变量的内容。在来看下Cache类，其实现的功能是只有一个缓存值的缓存系统，通过getCache方法传入一个整数，如果能命中那么就直接从内存返回结果，不然需要从硬盘中耗费较多的时间获取结果。由于OneValue是线程安全的，其在 //3处的代码操作的原子性，即integer和result的值要么同时改变，要么同时不变。而在//2出的oneValue变量被声明成volatile保证了可见性，如果有线程对oneValue变量进行修改，其它线程可以马上获得最新的oneValue值。整个系统没有用任何锁，但是却在volatile和不可变对象的支持下，完成了一个简单的多线程环境下线程安全的缓存系统。
