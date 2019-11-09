package jvm.post1;


/**
 * @author limingyuan001
 * 执行栈上分配的虚拟机参数  -XX:+DoEscapeAnalysis -Xmx10M
 * 不执行栈上分配的虚拟机参数  -XX:-DoEscapeAnalysis -Xmx10M
 * @description 栈上分配
 */
public class StackAllocation {
    static class User{
        Integer i;
    }

    public static void allo() {
        User user = new User();
        user.i = 4;
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000000L; i++) {
            allo();
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
    }
}
