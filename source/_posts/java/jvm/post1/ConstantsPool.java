package jvm.post1;

/**
 * @createtime 2019/11/4
 * @description java常量池
 */
public class ConstantsPool {
    public static void main(String[] args) {

        String s = new String("Hello") + new String("Jvm");
        String s1 = "HelloJvm";
        String intern = s.intern();
        System.out.println(s == intern);
    }
}
