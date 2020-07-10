package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;

/**
 * @author limingyuan001
 * @createtime 2020/6/14
 * @description
 */
public class Cache5 {
    private final static ExpensiveCompution computer = new ExpensiveCompution();
    private final static Map<Long, FutureTask<BigDecimal>> map = new HashMap<>();
    private static volatile long num = 0;


    private Cache5() {
        throw new RuntimeException("the Cache5 cannot be instantiated!");
    }

    public static BigDecimal compute(Long key) throws Exception {
        FutureTask<BigDecimal> valueTask;

        if (num != 0) {
            valueTask = map.get(key);
            if (valueTask != null) {
                return valueTask.get();
            }
        }

        return lockGetOrLoad( key);
    }

    private static BigDecimal lockGetOrLoad(Long key) throws Exception{
        FutureTask<BigDecimal> valueTask;
        Boolean needRun = false; //是否需要创建一个计算任务
            synchronized (Cache5.class) {
                valueTask = map.get(key);
                if (valueTask == null) {
                    valueTask = new FutureTask<>(() -> computer.compute(key));
                    map.put(key, valueTask);
                    num++;
                    needRun = true;
                }
            }
            if (needRun) {
                valueTask.run();
            }


        return valueTask.get();
    }
}
