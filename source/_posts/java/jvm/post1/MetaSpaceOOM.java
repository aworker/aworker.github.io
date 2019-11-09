package jvm.post1;

import net.sf.cglib.beans.BeanGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * -XX:MaxMetaspaceSize=10M -XX:+PrintGC
 * @description 元空间内存溢出
 */
public class MetaSpaceOOM {
    public static void main(String[] args) {
        BeanGenerator beanGenerator = new BeanGenerator();
        List<Class> classes = new ArrayList<>();
        for (int i=0; i<1000000000L;i++ ) {

            beanGenerator.addProperty("id"+i, Integer.class);
            Object aClass = beanGenerator.createClass();
            classes.add((Class) aClass);

        }
    }
}
