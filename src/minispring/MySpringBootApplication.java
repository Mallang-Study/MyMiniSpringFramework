package minispring;

import beanfactory.BeanFactory;
import beanfactory.DefaultBeanFactory;

public class MySpringBootApplication {


    private static final BeanFactory beanFactory = new DefaultBeanFactory();

    public static void run(Class<?> clazz) {

        beanFactory.setScanRootClass(clazz);

        beanFactory.prepareContext();
    }


}
