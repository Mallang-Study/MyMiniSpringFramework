package testClass;


import annotation.Component;
import minispring.MySpringBootApplication;
import testClass.bb.C;

@Component
public class Test {

    private final C c;

    public Test(C c) {
        System.out.println("Test 생성되었어용");
        this.c = c;
    }

    public static void main(String[] args) {
        MySpringBootApplication.run(Test.class);
    }
}
