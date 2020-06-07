package concurrency.post3;

/**
 * @author limingyuan001
 * @createtime 2020/6/3
 * @description
 */
public class OuterClassV1 {
    private final InnerClass innerClass;


    private static class InnerClass{
        private int state;

        public InnerClass(int state) {
            this.state = state;
        }

    }

    public OuterClassV1() {

        innerClass = new InnerClass(3);
    }

    public  int getState(){
        return innerClass.state;
    }


//    public void setState(int newState){
//        innerClass.state = newState;
//    }


    public static void main(String[] args) {
        OuterClassV1 outerClass = new OuterClassV1();

//        outerClass.setState(33);
        new Thread(() -> System.out.println(outerClass.getState()),"reader");
    }
}
