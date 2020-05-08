package spring.post1;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.post1.beans.Chicken;
import spring.post1.config.Config;

/**
 * @author limingyuan001
 * @createtime 2020/5/8
 * @description 演示spring处理循环依赖
 */
public class DemoSpringCircularDependencies {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Config.class);
        Chicken chicken = ac.getBean("chicken", Chicken.class);
        System.out.println(chicken);
    }
}
