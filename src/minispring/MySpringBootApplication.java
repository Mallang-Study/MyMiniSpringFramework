package minispring;

import annotation.Component;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class MySpringBootApplication {

    private static Class<?> mainClass;
    private static List<Class<?>> classList = new ArrayList<>();

    private static String PRE_PATH_MAIN_PACKAGE;

    private static Map<String, Object> beanMap = new HashMap<>(); //동시성 문제는 없을 거 같다. multiThread가 아니므로..?



    public static void run(Class<?> clazz){

        setMainClass(clazz);


        /*
        D:\MiniSpringFramework\out\production\MiniSpringFramework\Main.class 에서
        D:\MiniSpringFramework\out\production\MiniSpringFramework 가져오기
         */
        setPrePathMainPackage();


        allDirUnderMainPackage().forEach(MySpringBootApplication::addSubClassToClassList);

        classList.forEach(MySpringBootApplication::setBean);

/*        beanMap.keySet().stream().forEach(k -> {
            System.out.println("name: "+ k);
            System.out.println("bean: "+ beanMap.get(k).getClass());
        });*/

        //classList.forEach(c -> System.out.println(c.getName()));
    }

    private static void setBean(Class<?> c) {
        final Component declaredAnnotation = c.getDeclaredAnnotation(Component.class);//getAnnotation은 부모에 붙어있는 어노테이션까지 가져옴

        if(declaredAnnotation != null){
            final String beanName = beanName(c);
            try {
                beanMap.put(beanName, c.getDeclaredConstructor().newInstance());
            }
            catch (IllegalAccessException e) {

                System.out.println("[WARN]"+ c.getSimpleName()+"의 생성자의 접근 제어자가 public이 아닙니다. 생성하겠지만 오류가 발생할 수 있습니다.");

                createInstanceAccessLevelIsNotPublic(c, beanName);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static void createInstanceAccessLevelIsNotPublic(Class<?> c, String beanName) {
        try {
            final Constructor<?> declaredConstructor = c.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);//public이 아니어도 생성할 수 있게
            beanMap.put(beanName, declaredConstructor.newInstance());
        }

        catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    private static String beanName(Class<?> c) {
        final String firstChar = String.valueOf(c.getSimpleName().charAt(0));
        final String beanName = c.getSimpleName().replace(String.valueOf(firstChar), String.valueOf(firstChar).toLowerCase());
        return beanName;
    }


    private static void setMainClass(Class<?> clazz) {
        mainClass = clazz;//setMainClass
    }

    private static void setPrePathMainPackage() {
        //D:\MiniSpringFramework\out\production\MiniSpringFramework
        PRE_PATH_MAIN_PACKAGE = getPackageFile(mainClass.getPackageName()).getAbsolutePath();
        PRE_PATH_MAIN_PACKAGE= PRE_PATH_MAIN_PACKAGE.substring(0, PRE_PATH_MAIN_PACKAGE.lastIndexOf("\\"));

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
     Add a slash if the package name does not start with a slash
     */
    private static String addSlashPrefix(String packageName) {
        return (packageName.startsWith("/"))
                ?   packageName
                :  "/"+ packageName;
    }


    private static URL pathToUrl(String path) {
        return mainClass.getResource(path);
    }




    public static void addSubClassToClassList(File file){

        if(isPackage(file)){
            Arrays.stream(requireNonNull(file.listFiles()))//패키지 하위 파일을 모두 가져와서
                    .forEach(MySpringBootApplication::addSubClassToClassList);//재귀적으로 호출하여 수행
        }

        else if(isClassFile(file)) {
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
                .replaceFirst(".","");
    }

}
