package jvm.post1;

import java.util.ArrayList;

/**
 * @createtime 2019/11/2
 * @description 对区内存溢出
 * -Xms5M -Xmx5M -XX:+PrintGC
 */
public class HeapOOM1 {
    public static void main(String[] args) {
        ArrayList<Object> heapOOM1s = new ArrayList<>();
        for (; ; ) {
            heapOOM1s.add(new Object());
        }
    }
}
