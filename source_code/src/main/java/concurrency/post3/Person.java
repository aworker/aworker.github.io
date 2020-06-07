package concurrency.post3;

/**
 * @author limingyuan001
 * @createtime 2020/5/29
 * @description
 */
public class Person{
    public  int age;
    public final String name;

    public Person(int age,String name){
        this.age = age;
        this.name = name;
    }

    public static void main(String[] args){
        Person person = new Person(18,"栗子");
        new Thread(() -> System.out.println(person.name +":"+person.age ),"reader");
    }

}