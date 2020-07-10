package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author limingyuan001
 * @createtime 2020/6/14
 * @description
 */
public class Cache4 {
    private final static ExpensiveCompution computer = new ExpensiveCompution();
    private final static Map<Long, BigDecimal> map = new HashMap<>();
    private static volatile long num = 0;


    private Cache4() {
        throw new RuntimeException("the Cache4 cannot be instantiated!");
    }

    public static BigDecimal compute(Long key) {
        BigDecimal value;
        if (num != 0) {
            value = map.get(key);
            if (value != null) {
                return  value;
            }
        }
        return lockGetOrLoad(key);
    }

    private static BigDecimal lockGetOrLoad(Long key) {
        BigDecimal value = null;
        synchronized (Cache4.class) {
            value = map.get(key);
        }
        if (value == null) {
            value = computer.compute(key);
            synchronized (Cache4.class) {
                map.put(key, value);
                num++;
            }
        }

        return value;
    }
}