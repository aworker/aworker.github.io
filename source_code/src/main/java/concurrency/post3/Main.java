package concurrency.post3;

/**
 * @author limingyuan001
 * @createtime 2020/5/28
 * @description
 */
public class Main{
    public static void main(String[] args){
        ThreadAlarm countor = new ThreadAlarm(10000);
        for(int i=0; i<10000; i++){
            new Thread(() -> countor.invokeByPerThread()).start();
        }

        for (;;) {
            if (countor.getRemained() == 0) {
                throw new RuntimeException("达到配额了");
            }
        }
    }


}
