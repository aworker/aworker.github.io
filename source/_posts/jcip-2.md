---
title: 《java并发编程实战》第二章：线程安全
date: 2017-10-10 11:29:50
tags: java并发
---

第一章主要通过多线程如何重要，多线程将来要应用的越来越多，像是全书的一个引子，就是告诉读者，你选择本书没有错，这本书讲述的就是java中最重要的多线程部分，是程序开发技术中的屠龙刀。诚然本书（英文版本）是在2006年既java诞生10周年时候出版的。距离我2017年看本书已经过十年多的时间，虽然此书仍然是java并发界，乃至程序并发界一本经典书籍，此书中的绝大多数观点依然没有过时，但是计算机技术日新月异，十年前的很多观点到现在可能已经不在适用了，这需要在阅读过程中多加辨析。
第二章在第一章的基础上告诉读者在重要的多线程开发中线程安全的重要性，随着计算机硬件的发展，我们可以利用多核心cpu并发能力来让多个程序同时运行，这是这样计算机的效率就非常高，这是多线程开发的优点。但是要想享受这个优点，就要遵守多线程开发的“规则”。这个规则就是第二章介绍的-线程安全。作者在章首表达了两个观点：
>A program that omits needed synchronization might appear to work, passing its tests and performing well for years, but it is still broken and may fail at any moment.
>It is far easier to design a class to be thread-safe than to retrofit it for thread safety later.
<div align=right><b> Java Concurrency in Practice</b><div>

两个观点分别强调线程安全在多线程环境的必要性：
1. 你本应该应用并发的代码即使已经在生产环境中运行了很久，并且表现良好，但是它们也随时有可能崩溃。
2. 去修复一个类或者一段代码来保证它们是线程安全的远比当初就把它们设计成线程安全的要复杂的多。

对于第一点，我是感触颇深的，在刚工作不久有个同事开发一个功能，需要用到多线程技术，同事也非常注意多线程开发过程中的线程安全问题，没日没夜的忙了快两个月，后来又联合程序，测试，产品等测试将近一个月。可是到了功能上线的时候还是出现了死锁等线程安全问题。此后每周改功能上线的时候都会或多或少的爆出线程安全问题，直到功能正式上线1个月后才算稳定。在严密的测试也不能排查所有线上可能发生的多线程问题。
第二点，可以参考修改老代码...改过别人代码的小伙伴们都懂。修复非线程安全类为线程安全的类和完善别人的代码本质上没有区别。都是抄剩饭。
说了半天线程安全，那么什么是线程安全呢？
<center>![黑人问号脸](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/jcip-2/question.jpg)</center>

看下本书作者给的定义:
>A class is thread‐safe if it behaves correctly when accessed from multiple threads, regardless of the scheduling or
interleaving of the execution of those threads by the runtime environment, and with no additional synchronization or
other coordination on the part of the calling code.
<div align=right><b> Java Concurrency in Practice</b></div>

一个类是线程安全的要满足两个条件：
** 1. 不管java的运行时（runtime environment）怎么安排线程访问此类的顺序都能得到最终你认为结果。** 
比如你和你和你老婆大人公用一张银行卡（当然是你的工资卡了）。你和你老婆大人都可以向这个银行卡里面存钱同时也可以取钱。如果你月薪500元，在你发工资后，银行就不能让你和你老婆大人分别取出500元，不管你先取钱然后你老婆大人再取钱，还是你老婆大人先取钱然后你再取钱，亦或是你们两个协调好了同时取钱。如果你和老婆大人手里一共有500元，那么卡里余额绝对是0元，如果不是0元，而是1000元，那么劝你赶快去银行“自首”，因为任何意外所得都归国有。这里银行卡就相当于一个线程安全的类，而你和你老婆大人就是两个不同的线程。
** 2. 线程安全的类的“安全性”是它自身属性，使用它的类不需要提供额外的安全机制。**
这点就很好解释了，银行保证你卡里面的500元钱不管你怎么折腾都不会多，不管你是去ATM取钱，拿支付宝转账，用微信买瓶饮料。钱数该减少10元时候它绝对不会增加1元，该增加1元的时候它也绝对不会减少1分。

有童鞋可能会说，这两个条件不是天经地义的吗？本来就应该这样啊，但是啊，你认为的天经地义是习惯成自然的结果，就和你丢了钱去找警察，买个东西要交钱都是后天条件反射。对于咿呀学语的小孩纸来说，这些都不那么自然，都要问下为什么。以下面的银行卡类和ATM类，对刚才的两条进行详细说明。
```
public class BankCard {
    private int money;


    public BankCard(String userName,String userPassword){

        /**
         *  用 userName，userPassword查找数据库，初始化实例类BankCard。
         */
    }

    /**
     * 查看银行卡余额
     * @return
     */
    public int getMoney(){
        return money;
    }

    /**
     * 向银行卡中加钱
     * @param addNum 加钱数
     */
    public void addMoney(int addNum){
        money = money + addNum;
    }

    /**
     * 从银行卡中减钱
     * @param subNum  减钱数
     */
    public void subMoney(int subNum){
        if (money - subNum < 0) {
            return;
        }
    }
}```

```
public class ATM {

    private BankCard card;


    public ATM(String userName,String userPassword) {
        this.card = new BankCard(userName, userPassword);
    }

    /**
     * 通过ATM向指定银行卡存钱
     * @param money
     */
    public void  saveMoney(int money){
        card.addMoney(money);
    }

    /**
     * 通过ATM从指定银行卡中取钱
     * @param money
     */
    public void drawMoney(int money){
        card.subMoney(money);
    }
} ```
结合[ <font color=green> 《java并发编程实战》之java内存模型 </font> ](http://baidu.com)和线程安全的类的定义，我们知道BankCard是线程不安全的，当你和你妻子在不同的ATM机上通过输入账号密码来取钱时候（不用插卡的ATM机，没见过吧）。用代码模拟如下：
你自己存钱:
```
ATM atm_you = ATM("10223","****");
atm_you.saveMoney(500); ```

你老婆存钱:
```
ATM atm_wife = ATM("10223","*****");
atm_wife.saveMoney(1);
```
通过ATM的构造方法我们知道，当账号密码相同时候，会拿到同一张银行卡。如果你老婆和你同时存钱（假设开始卡里面没有一分钱），那么存钱结果可能是1，可能是500，当然也可能是你和银行都希望的501。为什么会这样呢？来看下面这张图片：
<center> ![线程_内存模型图](https://github.com/aworker/aworker.github.io/raw/hexo/source/_posts/jcip-2/thread_memory.png)</center>

































