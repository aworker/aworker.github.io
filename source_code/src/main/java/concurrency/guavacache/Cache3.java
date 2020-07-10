package concurrency.guavacache;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author limingyuan001
 * @createtime 2020/6/14
 * @description
 */
public class Cache3 {
    private final static ExpensiveCompution computer = new ExpensiveCompution();
    private final static Map<Long, BigDecimal> map = new HashMap<Long, BigDecimal>();
    private static volatile long num = 0;


    private Cache3() {
        throw new RuntimeException("the Cache3 cannot be instantiated!");
    }

    public static BigDecimal compute(Long key) {
        BigDecimal value;
        if (num != 0) {     //1
            value = map.get(key);  //2
            if (value != null) {
                return value;
            }
        }

        return lockGetOrLoad(key);

    }

    private static synchronized BigDecimal lockGetOrLoad(Long key) {

        BigDecimal value = map.get(key);
            if (value == null) {
                value = computer.compute(key);   //3
                map.put(key, value);
                num++;
            }

            return value;
    }
}