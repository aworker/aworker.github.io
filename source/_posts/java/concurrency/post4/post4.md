---
title: 04 构建线程安全的类：安全发布一个类
date: 2020-06-07 10:26:30
categories:
    - java并发
    - java并发原理及实践
tags:
---


但是我们创建的对象变量不是一成不变的，其可能需要改变初始值，如果将OuterClassV1类中被注释的代码放开，那么reader线程可能获取不到main线程对state变量赋的新值33，因为它破坏了final变量保持可见性的条件 3. 。那么有没有办法让我们可以在对象创建完后修改变量的值，同时保持可见性呢？这里就可以用我们的volatile关键字了:

```
public class OuterClassV2 {
    private final InnerClass innerClass;


    private static class InnerClass{
        private volatile int state;

        public InnerClass(int state) {
            this.state = state;
        }

    }

    public OuterClassV2() {

        innerClass = new InnerClass(3);
    }

    public  int getState(){
        return innerClass.state;
    }


    public void setState(int newState){
        innerClass.state = newState;
    }


    public static void main(String[] args) {
        OuterClassV2 outerClass = new OuterClassV2();

        outerClass.setState(33);
        new Thread(() -> System.out.println(outerClass.getState()),"reader");
    }
}
```
OuterClassV2和OuterClassV1的区别是V2的InnerClass的state变量被定义为volatile的了，<!--todo -->前文我们已经介绍了，volatile关键字修饰的变量满足happen-before原则：main线程volatile变量state的写操作结果33，对reader线程的volatile变量state的读操作一定是可见的，即reader线程的打印结果一定是33。使用volatile关键字一定要谨慎。
先做个总结
> final关键字修饰的变量在其对象的构造方法中没有暴露给其它线程且在对象发布后通过此变量获取的值都不在变化，那么其它通过final变量获取的值满足可见性。

> volatile关键字修饰的变量值发生变化