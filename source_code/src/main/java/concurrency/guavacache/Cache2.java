package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author limingyuan001
 * @createtime 2020/6/14
 * @description
 */
public class Cache2 {
    private final static ExpensiveCompution computer = new ExpensiveCompution();
    private final static Map<Long, BigDecimal> map = new HashMap<Long, BigDecimal>();


    private Cache2() {
        throw new RuntimeException("the Cache2 cannot be instantiated!");
    }

    public static BigDecimal compute(Long key) {
        BigDecimal value = map.get(key);   //1
        if (value != null) {
            return value;
        }

        return lockGetOrLoad(key);
    }

    private static synchronized BigDecimal lockGetOrLoad(Long key) {

        BigDecimal value = map.get(key);
        if (value == null) {
            value = computer.compute(key);
            map.put(key, value);   //2
        }

        return value;
    }
}