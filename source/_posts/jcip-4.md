---
title: 《java并发编程实战》第四章：设计线程安全的类
date: 2017-11-07 16:23:33
tags: java并发
---
前面德章节主要介绍java一些底层基础的并发实现机制和java的一些并发基础知识，本章节主要是用上述的这些知识来构建线程安全的类。本章将会把前面介绍的不可变对象(Immutable Object)、对象的线程级限制(Thread Confinement)、锁等技术综合运用来构建符合各种要求的线程安全的类。
<!-- more -->
***
**java监视器模式(Java monitor pattern)**
下面先看一个典型的java监视器模式代码：
```
public class PersonList{
	private final List<Person> myList = new ArrayList<Person>();

	public synchronized void addPerson(Person person){
		myList.add(person);
	}

	public synchronized boolean containsPerson(Person p){
		return myList.contains(p);
	}

	// public synchronized Person get(index i){
	//	 return myList.get(i);
	// }
}
```
代码中PersonList只有一个成员变量myList，其被声明为private final形式的List类型，通过前面文章[ <font color=green> 《java并发编程实战》第三章：发布对象 </font> ](http://aworker.cn/2017/11/03/jcip-3/)的学习我们知道final变量的使用可以保证通过PersonList的构造方法返回的PersonList类型的实例的成员变量已经被初始化完成。而private的使用保证其成员变量不会被其他线程直接访问，其addPerson和containsPerson方法都是加锁的方法，这样就可以保证同一时间只能有一个线程来操作成员变量myList。注意到myList是ArrayList类型的变量，其不是线程安全的类，但是通过java的监视器模式，我们实现了PersonList类的线程安全。这里我们没有，也不需要考虑Person类的线程安全性，因为PersonList类并没有提供任何方法让线程获得myList容器中的Person对象。但是如果PersonList添加有如上被注释的get方法，那么就需要保证Person类也是线程安全的了。这就是java监视器设计模式,或许你在平时也用过此模式，只不过不知道其具体的名字罢了。
***
**从零设计线程安全的类**
为了让线程安全的类的设计更有实战意义，我们从一个真实的开发需求出发，具体需求如下：
某民航公司要求设计一套客机位置显示系统，要求能够显示客机在天空中的经度和维度值；同时每架客机身上都装有GPS定位系统，会实时向总控制台返回自己的位置信息；每架飞机都拥有自己独一无二的名字。
从需求上分析我们需要设计两种类型的线程：display线程，用来在界面上显示飞机的位置；update线程，用来实时更新每架飞机的位置信息这两个类型的线程由同事A来负责，我们只需要提供相应的接口即可。我们将综合运用前几章讲到的知识和本章的java监视器模式来设计一个符合要求的显示系统。
1. java监视器模式
飞机在空中的位置可以用经度和维度来表示，其名字可以用一个字符串来表示，于是我们定义好飞机类：
```
class MutablePlane {
	public float  x; //维度
	public float  y; //经度
	public String name; //飞机名字

	public MutablePlane(MutablePlane plane){
		this.x = plane.x;
		this.y = plane.y;
		this.name = plane.name;
	}	
}

```
 有了飞机的类，我们还需要设计一个类来存储民航公司的所有飞机的信息的类，同时此类还要给display线程提供所有飞机信息的接口(方法),此类还要为update类提供更新指定飞机坐标位置的接口(方法);当然此类可以被display线程，update线程访问，要保证其线程安全，我们首先用java监视器模式来设计此类，具体类设计如下：
```
public class MonitorSystem {
    private final Map<String,MutablePlane> planes;

    public MonitorSystem(){
        //对planes进行赋值操作，初始化planes 这里是略写
        planes = new HashMap<String, MutablePlane>();
    }

    private Map<String,MutablePlane> deepcopy(Map<String,MutablePlane> m){
        Map<String,MutablePlane> map = new HashMap<String,MutablePlane>(); //1
        for(String name : m.keySet()){                                    //1
            map.put(name,new MutablePlane(m.get(name)));                  //1
        }                                                                 //1

        return Collections.unmodifiableMap(map);                          //2
    }

    //update线程使用，用来实时更新指定飞机的位置信息
    public synchronized void setLocation(String name, float x, float y) {
        MutablePlane mutablePlane = planes.get(name);
        if(mutablePlane == null){
            throw new RuntimeException("the plane does not exist for name: " + name);
        }

        mutablePlane.x = x;
        mutablePlane.y = y;
    }

    //display线程使用，用来在界面上显示飞机的位置信息
    public synchronized  Map<String,MutablePlane> getPlanes(){
        return deepcopy(planes);
    }
}
```
 MonitorSystem类的设计是一个典型的java监视器模式，我们注意下其getPlanes方法，这个方法调用了一个deepcopy方法，deepcopy方法先通过//1处的代码深度复制planes类，然后通过Collections的unmodifiableMap方法返回一个可读不可写的map类。之所以这么复杂的设计getPlanes方法而不是直接返回planes成员变量给display线程是因为planes包含的对象是线程非线程安全的类,我们把planes变量直接暴露给display线程，就相当于把线程不安全的MutablePlane对象暴露给别的线程，我们不知道同事A要怎样设计他的display线程，为了安全起见，我们不能让其他线程获得MutablePlane对象的引用，以防止其对MutablePlane对象做修改。这样实现的getPlanes方法还有一个特点：每次display线程调用getPlanes方法后得到的飞机位置信息可能已经“过时”，在获得飞机位置信息后，update线程可能又对飞机的位置信息做了更新，如果不再次调用getPlanes方法，是不能获得新的更新信息的。
2. 使用代理
 观察MonitorSystem类，因为我们使用的成员变量是非线程安全的HashMap类型，所以我们设计的getPlanes方和setLocations方法使用了同步。如果我们把成员变量变成线程安全的：
```
public class DelegatingSystem {
    private final ConcurrentHashMap<String,ImmutablePlane> planes;
    private final Map<String,ImmutablePlane> unmodifiableMap;

    public DelegatingSystem() {

        //对planes进行赋值操作，初始化planes 这里是略写
        this.planes = new ConcurrentHashMap<>();
        this.unmodifiableMap = Collections.unmodifiableMap(planes);
    }

    //update线程使用，用来实时更新指定飞机的位置信息
    public void setLocation(String name, float x, float y) {
        ImmutablePlane mutablePlane = planes.remove(name);
        if(mutablePlane == null){
            throw new RuntimeException("the plane does not exist for name: " + name);
        }

        planes.put(name, new ImmutablePlane(name, x, y));

    }

    //display线程使用，用来在界面上显示飞机的位置信息
    public Map<String,ImmutablePlane> getPlanes(){
        return unmodifiableMap;
    }
}
```
 在DelegatingSystem中我们没有使用同步(没有用synchronized关键字声明方法),因为我们把线程安全代理给了其成员变量planes，planes的类型是ConcurrentHashMap。为了保证DelegatingSystem的线程安全，我们还要保证其所存储的飞机信息对象的安全，如果我们继续沿用上面的MutablePlane类，那么display线程可以通过getPlanes拿到MutablePlane对象，而MutablePlane对象是非线程安全的。为此我们定义了新的不可变对象ImmutablePlane如下：
```
public class ImmutablePlane {
    public final float x;
    public final float y;
    public final String name;

    public ImmutablePlane(String name, float x, float y) {
        this.x = x;
        this.y = y;
        this.name = name;
    }
}
```
 这样就可以保证DelegatingSystem的线程安全，当然我们仍然可以想MonitorSystem那么使用MutablePlane类，但其getPlanes方法就需要返回的是对planes成员变量的深度拷贝，就像MonitorSystem的getPlanes方法一样。我们这里用了线程安全的ImmutablePlane类，来避免深度拷贝，这样在飞机特别多的情况下，可以节省深度拷贝方法的调用时间，从而提高响应效率，当然DelegatingSystem还有另外一个特价，就是display线程能实时获得update线程更新飞机后的最新位置信息，而不需要重新调用getPlanes方法。两种方式各有利弊，需要具体情况下具体选择。
***
**利用现有线程安全的类**
我们从DelegatingSystem类的设计中可以看到其使用了java基础类库中的ConcurrentHashMap类来保证其线程安全，这往往是最高效最简单也是最安全的方式。
* 简单：它避免也我们像MonitorSystem类那样每个方法都要自己设计同步逻辑，像setLocation和getPlanes方法用到了锁，但deepCopy就可以不用锁，这对开发者的并发知识要求很高，设计起来不是很简单。
* 高效：java的concurrent包中的各种类设计的非常精巧，在保证线程安全的同时有可以有很高的并发率，我们很难也没有比较设计出比先有并发类更高效的线程安全的类。
* 安全：java基础库里面的类，都是经过千锤百炼的。我们自己设计的类往往因为测试不够或者设计不够缜密，而导致意想不到的问题，而在多线程环境下线程安全的类更是难上加难，需要开发者能深入理解java的并发机制，同时准确把握设计需求，稍有不慎就可能留下bug，而在[ <font color=green> 《java并发编程实战》第二章：线程安全 </font> ](http://aworker.cn/2017/10/10/jcip-2/)开头我们就介绍了多线程bug的严重危害，这里不再赘述。

当然利用现有线程安全的类构建新的线程安全的类，也并不是没有缺点和需要注意的事项，下面通过几种常见的构建方式也说说其缺点和需要注意的事项。假设我们有如下需求：设计一个线程安全的类，能够实现线程安全的读写同时还提供额外的方法put-if-absent，具体语意为：向其中添加元素如果此类中不存在此元素就添加，如果已经存在，就不添加此元素。我们用不同的方法来实现这个功能。
1. 改造现有类
我们发现有很多现有的线程安全的类可以满足需求的读写方法的要求，我们需要做的是设计一个线程安全的put-if-absent方法，我们可以直接选择修改一个线程安全的类，比如Vector,在其中添加一个线程安全的方法来实现put-if-absent的逻辑。
这种方式的优点是可以最大限度的保持代码的健壮性，前提是你能获得CopyOnWriteArrayList的修改权限，而且对其线程安全策略十分了解。
2. 继承现有类
我们可以构造如下的类：
```
public class BetterVector<E> extends Vector<E> {
	public synchronized boolean putIfAbsent(E x){
		boolean absent = !contains(x);
		if(absent){
			add(x);
		}
		return absent;
	}
}
```
这样设计的BetterVector类可以实现上述的要求，但是它健壮性就不如前面德直接改造现有类。因为我们putIfAbsent方法用的对象的内置锁,而且Vector类的确也是用的内置锁来实现的线程安全。但是如果我们继承的不是java基础的线程安全类，我们继承了别的线程安全的类A，使用了其线程安全策略。如果之后类A更换线程安全策略，比如从使用内置锁变为使用ReentantLock来实现其线程安全策略，那么我们的代码的线程安全性就会被无声无息的破坏。这也是继承现有类来实现线程安全的缺点之一。

最后的最后，需要强调的是不管你是从0开始设计了一个线程安全的类，还是用java现有的线程安全的类设计出来一个类，我们都要需要为我们设计的类写好说明文档，这样不仅仅是利于后面的维护人员日常维护，也对我们后续开发和查阅代码也相当重要。







