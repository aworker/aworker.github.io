---
title: 01最全面的jvm运行时数据区讲解
date: 2019-11-09 16:36:13
categories:
    - jvm
    - java虚拟机，说点不一样的
tags:
---

# 最全面的jvm运行时数据区讲解


记得有位大佬曾经说过这样一句话：
> 如欲征服java，必须征服java虚拟机，如欲征服java虚拟机，需先征服java虚拟机内存模型。

java虚拟机内存，是java虚拟机进行对象内存空间分配、垃圾回收的活动室，只有先了解java虚拟机内存才能在此基础上进一步了解对象内存分配、垃圾回收等活动。有别于真实物理机硬盘、主存、缓存、寄存器的存储模型，java虚拟机内存模型按照其存储模块负责的数据类型将其划分为如下图所示的模型：

![java虚拟机内存模型](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/jvm/post1/jvm_memory_model.png)
                    

## 堆
堆是各个线程共享的内存区域，是java对象内存分配和垃圾回收的主战场，几乎所有的对象都是在堆中创建的。根据*Java虚拟机规范（Java Virtual Machine Specification）* 的规则，Java堆可以处于物理上不连续的内存空间中，只要逻辑上是连续的即可。如果在堆中没有内存空间完成Java对象的内存分配时，将会抛出OutOfMemoryError（一下简称OOM）。

关于堆的最常见虚拟机参数：
+ -Xms ：表示虚拟机堆的最小值，如 -Xms10M 表示堆的最小值为10MB
+ -Xmx ：表示虚拟机堆的最大值，如果 -Xmx100M 表示堆的最大值为100MB

```
/**
 * 设置虚拟机参数为：-Xms5M -Xmx5M
 */
public class HeapOOM {
    public static void main(String[] args) {
        ArrayList<Byte[]> bytes = new ArrayList<>();
        for (; ; ) {
            Byte[] _1M = new Byte[1024 * 1024];
            bytes.add(_1M);
        }

    }
}
```
执行结果:
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at jvm.post1.HeapOOM.main(HeapOOM.java:15)
```
“Java heap space”类型的OOM表示堆中没有可用的内存空间，具体到本例子中就是在大小为5M的堆中没有可用空间分配给大小为1M的数组对象。再来看一个例子：
```aidl
/**
 * @createtime 2019/11/2
 * 虚拟机参数 -Xms5M -Xmx5M 
*/ 
public class HeapOOM1 {
    public static void main(String[] args) {
        ArrayList<Object> heapOOM1s = new ArrayList<>();
        for (; ; ) {
            heapOOM1s.add(new Object());
        }
    }
}
```
执行结果:
```aidl
Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded
	at jvm.post1.HeapOOM1.main(HeapOOM1.java:14)
```
“GC overhead limit exceeded” 类型的OOM是在jdk6后引入的一种新的错误类型。发生错误的原因是虚拟机用了大量的时间进行GC但是只释放了较小的空间，这是虚拟机的一种保护机制。具体到本例子中就是虚拟机在GC时没有能回收内存空间，浪费了时间却没有收获，所以就抛出了这个错误。可以用 *-XX:-UseGCOverheadLimit*参数禁用这个检查，但解决不了内存问题，只是把错误的信息延后，替换成 java.lang.OutOfMemoryError: Java heap space错误。

## 方法区
方法区和堆一样，也是各个线程共享的内存区域，它用来存储已经被虚拟机加载的类信息、常量池、静态变量等。方法区是jdk5到jdk8变化较大的java虚拟机内存区域。在jdk5和jdk6时，常量池是存在方法区的：

![jdk5和jdk6](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/jvm/post1/jvm_memory_model_1.5.png)


而从jdk7及其以后的版本，常量池被放到了堆里面：

![jdk7](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/jvm/post1/jvm_memory_model_1.7.png)

常量池就是java语言系统级别的缓存，目的是让程序在运行过程中速度更快，更节省内存空间，java的8种基本数据类型外加String类型，共9种类型都有对应的常量池。这些类型的对象不可能全都放到常量池中存储，因此不同的类型有不同的存储策略，具体到String类型的对象来说，有如下三条规则：

+ 用双引号创建的对象放在常量池中，如 "Hello"，"Jvm"这种。
+ 用双引号创建的对象相加产生的对象放在常量池，如 *String s = "Hello" + "Jvm";*，这里的s对象就是放在常量池中的。
+ 调用String对象的intern方法会返回一个存放在常量池中的String对象,且两个对象内容相同。

再回到本篇的主题上，因为常量池位置的变化，在不同的jdk版本下，下面代码的执行结果是不一样的：
```aidl
/**
 * @description java常量池
 */
public class ConstantsPool {
    public static void main(String[] args) {

        String s = new String("Hello") + new String("Jvm"); //1
        String s1 = s.intern();  //2
        System.out.println(s == s1); //jdk5和jdk6中返回false，jdk7及其以上版本返回true。
    }
}
``` 

在jdk7之前，程序在执行//2处代码之前常量池中没有"HelloJvm"这个字符串常量，//2处代码执行时，程序会在常量池中创建一个"HelloJvm"的字符串对象s1并返回，而常量池是在方法区的。那一个在堆中的s对象和方法区中的s1对象比较地址是否相同，当然会得到false。
在jdk7及其以后的版本，程序在执行//2出代码时，发现常量池中同样没有"HelloJvm"这个对象，但因为常量池已经迁移到堆中，常量池不需要存储一个对象了，程序只是简单的把s这个对象的引用在常量池中存储了，此时s和s1指向的是同一个对象，结果当然是true。

上面简单介绍了jdk7中常量池的变化，而在jdk8中方法整个方法区被放到了物理机的本地内存,同时也更名为元空间（MetaSpace）：

![jdk8](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/jvm/post1/jvm_memory_model_1.8.png)


jdk8及其以后的版本，元空间直接使用物理机的本地内存，在不加限制的情况下其最大值为本地内存的最大可用值。考虑到物理机上可能部署其它的应用服务，通常会给元空间加一个大小限制。

关于元空间最常见的虚拟机参数是：
+ -XX:MetaspaceSize : 表示虚拟机元空间发生MetadataGC时的初始阈值,如 -XX:MetaspaceSize=10M 表示元空间在第一次到大10M时，会发生一次MetadataGC。
+ -XX:MaxMetaspaceSize ： 表示虚拟机元空间的最大值为MaxMetaspaceSize，如 -XX:MaxMetaspaceSize=15M 表示元空间的最大值为15M，再大就会发生OOM异常。

关于元空间的的内存溢出模拟，我们需要借助CGLib来动态的创建类，先引入如下maven依赖：
```aidl
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib-nodep</artifactId>
    <version>3.3.0</version>
</dependency>
```

具体代码如下：
```aidl
/**
 * 虚拟机参数 -XX:MaxMetaspaceSize=10M 
 * @description 元空间内存溢出
 */
public class MetaSpaceOOM {
    public static void main(String[] args) {
        BeanGenerator beanGenerator = new BeanGenerator();
        List<Class> classes = new ArrayList<>();
        for (int i=0; i<1000000000L;i++ ) {

            beanGenerator.addProperty("id"+i, Integer.class);
            Object aClass = beanGenerator.createClass();
            classes.add((Class) aClass);

        }
    }
}
```

执行结果为：
```aidl
Exception in thread "main" java.lang.IllegalStateException: Unable to load cache item
	at net.sf.cglib.core.internal.LoadingCache.createEntry(LoadingCache.java:79)
	at net.sf.cglib.core.internal.LoadingCache.get(LoadingCache.java:34)
	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData.get(AbstractClassGenerator.java:119)
	at net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:294)
	at net.sf.cglib.beans.BeanGenerator.createHelper(BeanGenerator.java:94)
	at net.sf.cglib.beans.BeanGenerator.createClass(BeanGenerator.java:85)
	at jvm.post1.MetaSpaceOOM.main(MetaSpaceOOM.java:19)
Caused by: java.lang.OutOfMemoryError: Metaspace
	at net.sf.cglib.core.AbstractClassGenerator.generate(AbstractClassGenerator.java:348)
	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:96)
	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:94)
	at net.sf.cglib.core.internal.LoadingCache$2.call(LoadingCache.java:54)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at net.sf.cglib.core.internal.LoadingCache.createEntry(LoadingCache.java:61)
	... 6 more
```

可以看到，引起IllegalStateException异常的正是因为"Metaspace"类型的OOM错误。具体原因为BeanGenerator对象通过createClass方法不断创建新的类，导致最大内存为10MB的元空间没办法存储类的信息而抛出异常。

## 虚拟机栈和本地方法栈

虚拟机栈和本地方法栈，都是线程私有的，主要用来存储在线程运行过程中的局部变量、操作数栈、方法出入口等信息，这些信息是以栈帧的形式存储的，虚拟机栈和本地方法栈的区别就是一个存储java方法运行时的栈帧数据一个存储本地方法（native 关键字修饰的方法）运行时的栈帧数据。由于都是存储栈帧数据，两种栈的区别不是很大，甚至在HotSpot虚拟机中，直接把这两个合二为一，所以本小节把这两种栈合起来说。java程序在运行时的栈数据结构如下图：

![运行时栈结构](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/java/jvm/post1/stack-model.jpg)  


在介绍堆时，我们曾说过几乎所有的对象都是在堆中创建的，这几乎中的特例就来自于栈，对象是可以在栈上创建，我们称为栈上分配。

```aidl
/**
 * 执行栈上分配的虚拟机参数  -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -Xmx10M
 * 不执行栈上分配的虚拟机参数  -XX:-DoEscapeAnalysis -XX:+EliminateAllocations -Xmx10M
 * 
 * 参数说明：
 * DoEscapeAnalysis  ： 逃逸分析，对于本例来说逃逸分析可以判断出//1处创建的对象是否会被本方法外的方法获取到。
 * EliminateAllocations ： 标量替换，对于本例来说，在逃逸分析的帮助下发现//1出的User对象不会逃逸出方法allo，那么消除User对象的堆内存分配，把它的字段改为一个个独立的局部变量（本例中是int类型的标量）存储在线程的栈中。
 * 要模拟栈上分配，需要逃逸分析和标量替换两个功能都是开启的。
 * @description 栈上分配
 */
public class StackAllocation {
    static class User{
        int i;
    }

    public static void allo() {
        User user = new User(); //1
        user.i = 4;
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000000L; i++) {
            allo();
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
    }
}
```

用不同的虚拟机参数执行上面的代码时，会发现同样执行1亿次方法调用，栈上分配的执行时间明显比非栈上分配的执行时间短。简单的解释就是1亿个的User对象不是被分配在堆上，这样就避免了频繁的GC，对性能自然有很大提升。

与栈相关的虚拟机参数主要有：
+ -Xss : 设置java线程栈的大小，如 -Xss100k 表示每个java线程栈的大小为100k。

线程栈是用来存方法的栈帧的。线程栈越大其能调用的方法深度越大，运行如下代码可以印证此观点：

```aidl
/**
 * 虚拟机参数 -Xss1000K
 * @description 模拟栈内存溢出
 */
public class StackOverFlowOOM {
    private static int num = 0;

    public static void loop(){
        num++;
        loop();
    }

    public static void main(String[] args) {
        try {
            loop();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(num);
        }
    }
}
```

当Xss的值越大时，程序中的num变量在栈溢出异常时的值越大。jdk8中如果不指定Xss参数的大小，那么其默认值为1MB，这也从内存角度印证线程是一种昂贵的资源，即使简单的创建一个线程而不分配给其处理任务，其也要占用一些内存空间。


## 程序计数器

程序计数器是一块较小的内存空间，它可以看作是当前线程所执行的字节码的行号指示器，因为操作系统会分配给各个线程一些时间片来运行，当时间片用完后，就需要有程序计数器记录线程执行的位置，用来在线程重新获得时间片时能恢复到原来的执行位置。从程序计数器的用途得知，程序程序计数器也是线程私有的，而且也是唯一一个不会有OOM异常的虚拟机内存区域。



#篇尾小节

本篇主要简绍了java虚拟机在运行时的各个内存区域，简单介绍了它们的作用和内存溢出的方式。




> 有任何不懂或者质疑的地方，都欢迎大家积极留言讨论，留言必回，一起学习进步。



<!--
参考文献 ：
http://lovestblog.cn/blog/2016/10/29/metaspace/ 
-->