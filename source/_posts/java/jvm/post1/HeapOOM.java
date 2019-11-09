package jvm.post1;

import java.util.ArrayList;

/**
 * @createtime 2019/11/2
 * @description 堆区内存溢出
 * -Xms10M -Xmx10M
 */
public class HeapOOM {
    public static void main(String[] args) {
        ArrayList<Byte[]> bytes = new ArrayList<>();
        for (; ; ) {
            Byte[] _1M = new Byte[1024 * 1024];
            bytes.add(_1M);
        }

    }
}
