package concurrency.post3;

/**
 *  ThreadAlarm类用统计线程调用次数
 */
public class ThreadAlarm {
    private int quota;

    public ThreadAlarm(int quota){
        this.quota = quota;
    }

    public void invokeByPerThread(){
        quota--;
    }

    public int getRemained(){
        return quota;
    }
}