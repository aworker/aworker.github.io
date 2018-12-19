---
title: 《java并发编程实战》第六章：任务的执行
date: 2018-7-15 11:29:50
tags: java并发
---
在组建java程序时候关于任务执行方面第一个要考虑的就是任务边界的划分，即把执行的程序分成合理的任务，理想的情况是这些任务的状态，结果不会影响到其它任务。

以java最擅长的服务器端开发为例子，服务器端要做到两点：高并发（good throughput）、低延迟（good responsiveness）。高并发是服务提供者的需要，我们想让我们的程序承载更多的用户来分担服务器开销；低延迟是用户的需求，用户想尽快的看到服务器对请求的响应结果。同样服务器的任务划分也是比较清晰和明确的：我们可以把用户的一个请求，作为一个任务。在高并发、低延迟的要求下，我们来设计下服务器的并发框架。
首先能想到的是单线程模式，程序如下:
```
public class SingleThreadWebServer{
	public static void main(String[] args) throws Exception{
		ServerSocket server = new ServerSocket(8900);
		while(true){
			Socket socket = server.accept(); 
			handlerRequest(socket);
		}
	}
}
```
这个程序中，每个客户端请求和其处理都在一个程序中执行，在handlerRequest方法比较耗时的情况下，用户将感觉到卡顿，没能做到低延迟；同时因为同一时间只能有一个客户端请求被处理，服务器也没能做到高并发。在真实的生产环境中，除了GUI程序，基本上不会用到这种模型框架。
<!-- more -->

因为handlerRequest方法可能造成延迟，我们改进成程序如下：

```
public class ThreadPerTaskWebServer{
	public static void main(String[] args){
		ServerSocket serverSocket = new ServerSocket();
		while(true){
			Socket socket = serverSocket.accpet();
			new Thread(){
				@Override
				public void run(){
					handlerReqeust(socket);
				}
			}.start();
		}
	}
}
```
此模式下将任务接受和任务执行分别放在不同的线程中来执行，main方法所在的线程主要负责接受客户端请求，具体的任务执行放在单独的一个新的线程中来执行。此模式可以很好的满足服务器高并发，低延迟的设计需求。但还是存在三个缺点：
1、线程生命周期成本太大（thread lifecycle overhead）：此模式中，每个请求都要创建一个新的处理线程，当任务处理完毕后，此线程还要被销毁。但是线程的创建和销毁并不是没有成本的。线程的创建需要向jvm申请一些资源，这就给处理客户端请求带来了一些延迟。如果需要此线程处理的任务都是一些短时间的任务，那么线程的创建时间将会称为主要的延迟。
2、资源的消耗严重（Resource consumption）：线程是需要消耗资源的，比如内存。当线程的数量超过了处理器的核数，多余的线程将阻塞，这将造成资源的浪费同时给gc造成极大的压力，同时线程之间竞争CPU也会造成一些性能上的牺牲。
3、影响程序稳定性（stability）：程序能创建多少线程是有限制的，如果没有限制的创建过多的线程，很可能会得到OutOfMemoryError。

造成上述问题的根本原因是系统没有对程序中可创建的线程数量做限制，对上述程序改进如下：

```
public class TaskExecutionWebServer{
	private static Executor executor = Exectutors.newFixedThreadPool(100); //1
	public static void main(String[] args){
		ServerSocket serverSocket = new ServerSocket(9090);
		while(true){
			Socket socket = serverSocket.accpet();
			Runnable task = new Runnable(){
				public void run(){
					handlerRequest(socket);
				}
			}
			executor.execute(task);
		}
	}
}
```
Executor 是一个接口，其只有一个方法execute。execute方法用来执行Runnable实例的，我们只需要把我们的handlerRequest方法的方法封装在一个Runnable实例中，然后把这个任务提交给Executor，具体的任务运行管理，交给Executor即可。同时我们在//1处可以看到我们最多可以创建
的线程数量被限定为100个。这就没有了ThreadPerTaskWebServer中的无限制创建线程的问题了。同时把任务的提交和任务的执行解耦还给我们带来了灵活的执行任务策略。我们可以很容易的把执行策略编程单线程模式，只需要把 //1处 exectuor变成一个OneThreadExecutor实例：
```
public class OneThreadExecutor implements Executor {
	public void execute(Runnable task){
		task.run();
  }
}
```
我们还可以把执行策略变成我们之前的一个任务一个线程的模式，只需要把 //1处 executor变成ThreadPerTaskExecutor实例：
```
public class ThreadPerTaskExecutor implements Executor{
	public void execute(Runnable task){
		new Thread(task).start();
	}
}
```
使用Executor接口来执行任务，可以让我们在获得了高并发，低延迟的前提下，又有了很大的灵活性，很方便的更改执行策略。但是考虑一个程序完整的生命周期，不应该只有执行状态，还应该有关闭状态。当程序要关闭的时候，Executor应该能对其做出相应的响应，所以一个Executor也应该有自己的生命周期。为了描述Executor的生命周期，我们引进了ExecutorService接口，其继承了Executor接口。一个ExecutorService实例有三种状态：running、shutting down、terminated。ExecutorService实例在刚刚创建的时候就处在running状态；当调用shutdown方法的时候，ExecutorService实例会把状态置为shutting down，同时把正在执行的任务继续执行，当通过execute方法向其中添加新的任务时，ExecutorService实例可以忽略新添加的任务，或者抛出一个异常，具体取决于策略的选取，这在以后再说。实例中没有正在执行的任务的时候，实例就进入terminated状态。一个具有生命周期管理的WebServer服务器类如下：
```
public class LifeCycleWebServer{
	private final ExecutorService exec = ....;
	public static void main(String[] args) throws Exception{
		ServerSocket socket = new ServerSocket(9090);
		while(!exec.isShutdown()){
			Socket socket = socket.accpet();
			exec.execute(new Runnable(){
					public void run(){
						if(isShutdownRequest(socket)){
							exec.shutdown();
						}
						handleRequest(socket);
					}
				};)
		}
	}
}
```
在LifeCycleWebServer类中，我们可以通过一个特殊的客户端请求来让ExecutorService进入关闭状态。这样ExecutorService实例也有了生命周期，更能适合真实的开发需求。

通过以上比较可知，用Executor框架执行任务的好处如下：
1、能够实现高并发和低延迟。
2、因为Executor的实现类可以很好的管理其线程的创建数量，可能任务提交给Executor的时候，已经有可以执行此任务的线程了，这样减少了
线程创建带来的延迟。因为Executor实例限制了，创建线程的数量，减少了线程之间对CPU的竞争，也在一定程度上提高了系统性能。对线程创建数量的限制，保证了系统的稳定性。
3、更加灵活的执行策略，因为Executor实现了任务的提交和执行的解耦，可以让我们灵活的更换任务的执行策略。
4、继承Executor接口的ExecutorService增加了生命周期的管理，更加贴近真实的生产开发。

综上所述，在java中执行任务首要考虑的是Executor，而不是Thread.