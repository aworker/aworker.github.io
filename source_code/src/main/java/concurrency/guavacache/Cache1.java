package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author limingyuan001
 * @createtime 2020/6/14
 * @description
 */
public class Cache1 {
    private final static  ExpensiveCompution computer = new ExpensiveCompution();
    private  final static Map<Long, BigDecimal> map = new HashMap<>();

    private Cache1() {
        throw new RuntimeException("the Cache1 cannot be instantiated!");
    }


    public static synchronized BigDecimal compute(long key) {
        BigDecimal value = map.get(key);
        if (value == null) {
            value = computer.compute(key);
            map.put(key, value);
        }

        return value;
    }
}

