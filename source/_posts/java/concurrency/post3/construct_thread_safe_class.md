---
title: 03 构建线程安全的类：安全发布一个类
date: 2020-05-20 20:06:13
categories:
    - java并发
    - java并发原理及实践
tags:
---

<!-- 正确性问题 -->

编写多线程的java程序时，我们最最基础的要求是保证程序的正确性：多线程环境下程序能够获得正确的执行结果。而保证程序在多线程环境的正确性离不开构成java程序的类在多线程环境下的正确性，也就是我们说的**线程安全**。那么什么样的类是线程安全的呢？

> 线程安全的类在被多个线程访问执行时能保持设计者想要的行为，这种正确的行为的保持不需要借助此类以外的其它手段的帮助。

这里有三个要点，类必须是被多个线程调用、类能保持设计者想要的行为、这种行为的保持不需要别的辅助手段来完成，说起来挺抽象的，栗子给大家举个例子，比如有如下类：

```
/
/**
 *  ThreadAlarm类用来统计线程调用次数
 */
public class ThreadAlarm {
    private int quota;

    public ThreadAlarm(int quota){
        this.quota = quota;
    }

    public void invokeByPerThread(){
        quota--;
    }

    public int getRemained(){
        return quota;
    }
}

```

如果ThreadAlarm类是线程安全的，那么它需要保证多个线程调用它的invokeByPerThread方法时（被多个线程访问执行）quota能正确减少相应的数值（保持设计者要求的行为），同时线程在调用invokeByPerThread方法前不用加锁或者其他方式来保证行为的正确性（不需要借助此类以外的其它手段的帮助）。按照这个要求来说，ThreadAlarm类并不是一个线程安全的类。

考虑如下对ThreadAlarm类的一种使用情况，main方法中定义一个配额为10000的ThreadAlarm对象，同时创建10000个线程分别获取一个配额，然后循环判断threadAlarm对象剩余的配额是否为0，为0就抛出一个运行时异常。如果ThreadAlarm类是线程安全的，那么main方法中一定会抛出一个RuntimeException，但是大家可以运行下这个程序，不需要尝试太多次，很有可能会出现main方法执行死循环，不抛异常的情况。

```
public class Main{
    public static void main(String[] args){
        ThreadAlarm threadAlarm = new ThreadAlarm(10000);  //1
        System.out.println(threadAlarm.getRemained()); //2
        for(int i=0; i<10000; i++){   
            new Thread(() -> threadAlarm.invokeByPerThread()).start();  //3
        }

        for (;;) {
            if (threadAlarm.getRemained() == 0) {
                throw new RuntimeException("达到配额了");
            }
        }
    }


}
```

为什么ThreadAlarm类不是一个线程安全的类呢？这里栗子带大家先看下 //1 处的代码，在main线程执行创建TheadAlarm类时实际上是执行了如下三个步骤：

1. 创建一个ThreadAlarm对象
2. 把ThreadAlarm对象的quota变量赋值为10000
3. 把ThreadAlarm对象赋值给一个对象引用（本例中是threadAlarm）

<!-- todo exchange url-->
前面的文章说过，多线程的正确性问题主要是因为操作的原子性，操作的有序性，和操作的可见性没能得到保证导致的。这里在main线程中执行创建ThreadAlarm对象的三个步骤的顺序很有可能是有变化的，比如按照 1. 3. 2.的顺序执行，前文<!-- todo exchange url --> 也说过，操作系统、java虚拟机、java编译器不会无脑改变程序的执行顺序，最起码在main线程中这种改变是不会影响 //2 处代码的打印结果：不管怎么指令重排序，打印的数字永远为10000。但是对于 //3 处创建并执行的线程来说，其通过threadAlarm对象的invokeByPerThread方法看到的quota变量很有可能是quota变量的默认值:0。这里引出一个重要的概念：

> 在某个线程中创建的对象其引用对其它线程可见时，并不意味着此对象中的成员变量对其它线程也是可见的。

那么如何才能保证对象和其成员变量对其它线程的可见性呢？这里可以用我们的final关键字了。如下例子：

```
public class Person{
    public  int age;
    public final String name;

    public Person(int age,String name){
        this.age = age;
        this.name = name;
    }

    public static void main(String[] args){
        Person person = new Person(18,"栗子");
        new Thread(() -> System.out.println(person.name +":"+person.age ),"reader");
    }

}
```
对于这个程序来说，//1 处的线程reader一定可以看到person.name的值:“栗子”，但是不一定能看到person.age的值:18。final关键字可以保证其它线程（非对象的创建线程）在能看到某个对象（此对象的索引不为null）时，那么这个线程也能看到此对象用final关键字修饰的成员变量的初始化值。除此以外，其它线程也能看到通过final关键字修饰的变量获取的其它对象的正确初始化值，举个栗子：

```
public class OuterClassV1 {
    private final InnerClass innerClass;


    private static class InnerClass{
        private int state;

        public InnerClass(int state) {
            this.state = state;
        }
        
    }

    public OuterClassV1() {

        innerClass = new InnerClass(3);
    }

    public  int getState(){
        return innerClass.state;
    }

    
//    public void setState(int newState){
//        innerClass.state = newState;
//    }


    public static void main(String[] args) {
        OuterClassV1 outerClass = new OuterClassV1();
        
//        outerClass.setState(33);
        new Thread(() -> System.out.println(outerClass.getState()),"reader");
    }
}
```
代码比较简单，main线程创建一个OuterClassV1对象，同时对InnerClass对象的state变量赋值为3，reader线程一定能正确获取到InnerClass对象的state变量的值:打印结果一定是3，因为reader是通过final修饰的变量(innerClass)获取到state值的。

不仅如此，我们的OuterClassV1类还是线程安全的类，它能保持线程安全是因为它是一个不可变的类：

1. 变量必须都用final关键字修饰。
2. 变量所在的对象在构造完成前不能暴露给其它线程。
3. 通过final变量获取到的值在变量所在的对象构造完成后，不能再发生改变。

用OutterClassV1来解释，其innerClass变量被final关键字修饰满足 1. ；在OuterClassV1的构造方法中OuterClassV1的对象（this）没有暴露给别的线程满足 2. ；OuterClassV1对象构造完后innerClass变量被final修饰不可能变化且没有方法可以再修改其state的值，OutterClassV1对象创建完成后，其满足 3. 。

简单的逻辑理解就是final关键字保证了innerClass变量和state变量对别的线程的可见性，同时这两个变量被发布后不能再被改变，那么当然是线程安全的了。

> 不可变类永远都是线程安全的。

























