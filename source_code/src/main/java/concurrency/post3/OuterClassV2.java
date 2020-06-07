package concurrency.post3;

/**
 * @author limingyuan001
 * @createtime 2020/6/5
 * @description
 */
public class OuterClassV2 {
    private final InnerClass innerClass;


    private static class InnerClass{
        private volatile int state;

        public InnerClass(int state) {
            this.state = state;
        }

    }

    public OuterClassV2() {

        innerClass = new InnerClass(3);
    }

    public  int getState(){
        return innerClass.state;
    }


    public void setState(int newState){
        innerClass.state = newState;
    }


    public static void main(String[] args) {
        OuterClassV2 outerClass = new OuterClassV2();

        outerClass.setState(33);
        new Thread(() -> System.out.println(outerClass.getState()),"reader");
    }
}
