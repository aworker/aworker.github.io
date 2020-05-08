package spring.post1.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author limingyuan001
 * @createtime 2020/5/8
 * @description
 */
@Component
public class Chicken {
    @Autowired
    Egg egg;
}
