package beanfactory;

/**
 * Created by ShinD on 2022-02-25.
 */
public interface BeanFactory {
    void prepareContext();

    void setScanRootClass(Class<?> clazz);

}
