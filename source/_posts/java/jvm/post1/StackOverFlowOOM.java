package jvm.post1;

/**
 * 虚拟机参数 -Xss1000K
 * @description 模拟栈内存溢出
 */
public class StackOverFlowOOM {
    private static int num = 0;

    public static void loop(){
        num++;
        loop();
    }

    public static void main(String[] args) {
        try {
            loop();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(num);
        }
    }
}
