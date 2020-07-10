package concurrency.guavacache;

import java.util.concurrent.FutureTask;

/**
 * @author limingyuan001
 * @createtime 2020/6/15
 * @description
 */
public class Test {
    public static void main(String[] args) throws  Exception{
        FutureTask<String> stringFutureTask = new FutureTask<>(() -> "ll");
        new Thread(() ->
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stringFutureTask.run();

        }
        ).start();
        String s = stringFutureTask.get();
        System.out.println(s);
    }
}
