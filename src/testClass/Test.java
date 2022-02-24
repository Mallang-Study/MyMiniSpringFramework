package testClass;


import annotation.Component;
import minispring.MySpringBootApplication;

@Component
public class Test {

    protected Test() {

    }

    public static void main(String[] args) {
        MySpringBootApplication.run(Test.class);
    }
}
