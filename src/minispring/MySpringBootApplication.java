package minispring;

import annotation.Component;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class MySpringBootApplication {

    private static Class<?> mainClass;
    private static List<Class<?>> classList = new ArrayList<>();

    private static String PRE_PATH_MAIN_PACKAGE;

    private static Map<String, Object> beanStore = new ConcurrentHashMap<>(); //Exception in thread "main" java.util.ConcurrentModificationException 발생해버림 (remove를 할 때)
    //https://m.blog.naver.com/tmondev/220393974518 참고


    private static Map<Class<?>, String> beanNameStoreByType = new ConcurrentHashMap<>();
    private static Map<String, Constructor<?>> constructorStoreByBeanName = new ConcurrentHashMap<>();


    public static void run(Class<?> clazz) {

        setMainClass(clazz);

        /*
        D:\MiniSpringFramework\out\production\MiniSpringFramework\Main.class 에서
        D:\MiniSpringFramework\out\production\MiniSpringFramework 가져오기
         */
        setPrePathMainPackage();

        allDirUnderMainPackage().forEach(MySpringBootApplication::addSubClassToClassList);

        classList.forEach(MySpringBootApplication::setBean);

        //아직 생성 안된 빈들 의존성 주입해서 생성해주기(재귀호출)
        createBeanByDI();

    }


    private static void createBeanByDI() {
        for (String beanName : constructorStoreByBeanName.keySet()) {

            Constructor<?> constructor = constructorStoreByBeanName.get(beanName);


            Class<?>[] parameterTypeArr = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypeArr.length];
            int count = 0;


            count = addRequiredArgsIn(parameterTypeArr, args, count);

            if (count != args.length) throw new IllegalMonitorStateException(constructor.getDeclaringClass()+"의 생성자 중 빈으로 등록되지 않은 필드가 있어 생성할 수 업습니다.");


            try {
                makeBeanAndStore(constructor.getDeclaringClass(), beanName, constructor, args);
            } catch (IllegalAccessException e) {
                System.out.println("[ERROR]" + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }


            constructorStoreByBeanName.remove(beanName);


        }

        constructorStoreByBeanName.keySet().stream().forEach(System.out::println);
        if (constructorStoreByBeanName.size() != 0) {
            createBeanByDI();
        }
    }

    /**
     * @return 추가한 매개변수의 개수
     */
    private static int addRequiredArgsIn(Class<?>[] parameterTypes, Object[] args, int count) {
        for (Class<?> parameterType : parameterTypes) {
            String paramBeanName = beanNameStoreByType.get(parameterType);

            if (paramBeanName == null) break;

            args[count++] = beanStore.get(paramBeanName);
        }
        return count;
    }


    /**
     * 기본 생성자 없는 경우 -> 예외
     * <p>
     * 파라미터 없는 기본 생성자가 있는 경우 -> 빈 생성, 생성한 빈을
     * beanStore - {Key:빈이름 , value:생성한객체},
     * beanNameStoreByType - {Key:클래스타입, value:"빈이름"}
     * <p>
     * 파라미터 있는 기본 생성자가 있는 경우 ->
     * constructorStoreByBeanName - {Key:빈 이름, vaule:생성자}
     */
    private static void setBean(Class<?> clazz) {

        if (!notHasComponentAnnot(clazz)) return;


        try {
            String beanName = beanName(clazz);
            Constructor<?> defaultConstructor = getDefaultConstructor(clazz);

            //매개변수 없는 기본 생성자-> 바로 Bean 만들어서 보관
            if (isNoArgsConstructor(defaultConstructor)) makeBeanAndStore(clazz, beanName, defaultConstructor);

                //매개변수 개수가 여러개-> 생성자를 빈 이름과 함께 보관
            else constructorStoreByBeanName.put(beanName, defaultConstructor);

        } catch (IllegalArgumentException e) {//기본 생성자가 없어서 발생하는 오류
            System.out.println("[ERROR]" + e.getMessage());
        } catch (Exception e) {//ETC Exception
            e.printStackTrace();
        }
    }

    private static boolean notHasComponentAnnot(Class<?> clazz) {
        return clazz.getDeclaredAnnotation(Component.class) != null;
    }


    private static void makeBeanAndStore(Class<?> clazz, String beanName, Constructor<?> constructor, Object... initargs) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        if (isPrivateModifier(constructor.getModifiers()))
            throw new IllegalAccessException(clazz.getSimpleName() + "의 생성자의 접근 제어자가 private입니다.");

        if (!isPublicModifier(constructor.getModifiers()))
            constructor.setAccessible(true);


        beanStore.put(beanName, constructor.newInstance(initargs));

        beanNameStoreByType.put(clazz, beanName);


    }


    private static boolean isPrivateModifier(int modifiers) {
        return (modifiers & Modifier.PRIVATE) == Modifier.PRIVATE;
    }

    private static boolean isPublicModifier(int modifiers) {
        return (modifiers & Modifier.PUBLIC) == Modifier.PUBLIC;
    }

    private static boolean isFinalModifier(int modifiers) {
        return (modifiers & Modifier.FINAL) == Modifier.FINAL;
    }


    private static Constructor<?> getDefaultConstructor(Class<?> clazz) throws IllegalArgumentException {

        if(clazz.getDeclaredConstructors().length == 1){
            return clazz.getDeclaredConstructors()[0];
        }


        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (isNoArgsConstructor(constructor)) return constructor;

            Constructor<?> requiredArgsConstructor = getRequiredArgsConstructor(clazz);
            if (requiredArgsConstructor != null) return requiredArgsConstructor;
        }

        throw new IllegalArgumentException(clazz.getSimpleName() + "클래스는 기본 생성자가 없습니다!");
    }


    private static Constructor<?> getRequiredArgsConstructor(Class<?> c) {
        List<Class<?>> finalFieldTypes = new ArrayList<>();
        fillFinalFields(c, finalFieldTypes);

        Constructor<?>[] constructors = c.getDeclaredConstructors();

        return Arrays.stream(constructors)
                .filter(constructor ->
                        finalFieldTypes.containsAll(
                                List.of(constructor.getParameterTypes())
                        ))
                .findAny().orElse(null);
    }


    private static void fillFinalFields(Class<?> c, List<Class<?>> finalFieldTypes) {
        Arrays.stream(c.getDeclaredFields())
                .filter(field -> isFinalModifier(field.getModifiers()))
                .forEach(field -> finalFieldTypes.add(field.getType()));
    }


    private static boolean isNoArgsConstructor(Constructor<?> constructor) {
        return constructor.getParameterTypes().length == 0;
    }


    private static String beanName(Class<?> c) {
        String firstChar = String.valueOf(c.getSimpleName().charAt(0));
        return c.getSimpleName().replace(String.valueOf(firstChar), String.valueOf(firstChar).toLowerCase());
    }


    private static void setMainClass(Class<?> clazz) {
        mainClass = clazz;//setMainClass
    }

    private static void setPrePathMainPackage() {
        //D:\MiniSpringFramework\out\production\MiniSpringFramework
        PRE_PATH_MAIN_PACKAGE = getPackageFile(mainClass.getPackageName()).getAbsolutePath();
        PRE_PATH_MAIN_PACKAGE = PRE_PATH_MAIN_PACKAGE.substring(0, PRE_PATH_MAIN_PACKAGE.lastIndexOf("\\"));

    }


    private static List<File> allDirUnderMainPackage() {
        return Arrays.stream(requireNonNull(
                getPackageFile(mainClass.getPackageName()).listFiles())
        ).toList();
    }


    private static File getPackageFile(String dirName) {
        return new File(pathToUrl(addSlashPrefix(dirName)).getFile());
    }


    /**
     * Add a slash if the package name does not start with a slash
     */
    private static String addSlashPrefix(String packageName) {
        return (packageName.startsWith("/"))
                ? packageName
                : "/" + packageName;
    }


    private static URL pathToUrl(String path) {
        return mainClass.getResource(path);
    }


    public static void addSubClassToClassList(File file) {

        if (isPackage(file)) {
            Arrays.stream(requireNonNull(file.listFiles()))//패키지 하위 파일을 모두 가져와서
                    .forEach(MySpringBootApplication::addSubClassToClassList);//재귀적으로 호출하여 수행
        } else if (isClassFile(file)) {
            addClassList(file);
        }
    }

    private static boolean isPackage(File file) {
        return !file.getName().contains(".");
    }

    private static boolean isClassFile(File file) {
        return file.getName().contains(".class");
    }

    private static void addClassList(File file) {
        try {
            classList.add(Class.forName(className(file)));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static String className(File file) {
        return file.getAbsolutePath()
                .replace(PRE_PATH_MAIN_PACKAGE, "")
                .replace(".class", "")
                .replaceAll("\\\\", ".")
                .replaceFirst(".", "");
    }

}
