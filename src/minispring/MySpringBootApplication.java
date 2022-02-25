package minispring;

import annotation.Component;
import beanfactory.BeanFactory;
import beanfactory.DefaultBeanFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class MySpringBootApplication {


    private static final BeanFactory beanFactory = new DefaultBeanFactory();

    public static void run(Class<?> clazz) {

        beanFactory.setScanRootClass(clazz);

        beanFactory.prepareContext();
    }


}
