package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author limingyuan001
 * @createtime 2020/6/16
 * @description
 */
public class Cache {
    private static ExpensiveCompution compution = new ExpensiveCompution();

    private static Map<Long,Future<BigDecimal>> map = new HashMap<>();

    private static volatile int num = 0;

    public static  BigDecimal compute(Long key) throws Exception{
        if (num != 0) {
            if (map.get(key) != null) {
                return map.get(key).get();
            }
        }



        return lockGetOrCompute(key);
    }

    public static   BigDecimal lockGetOrCompute(Long key) throws Exception{

        Future<BigDecimal> futureTask = null;
        synchronized (Cache.class) {
            if (map.get(key) != null) {
                return map.get(key).get();
            }

            futureTask = new FutureTask<>(() -> compution.compute(key));
            map.put(key, futureTask);
            num++;
        }


        ((FutureTask<BigDecimal>) futureTask).run();

        return futureTask.get();
    }


    public static void main(String[] args) {

    }

}
