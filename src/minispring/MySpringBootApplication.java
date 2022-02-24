package minispring;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by ShinD on 25/02/2022.
 */
import static java.util.Objects.requireNonNull;

public class MySpringBootApplication {

    private static Class<?> mainClass;
    private static List<Class<?>> classList = new ArrayList<>();

    private static String PRE_PATH_MAIN_PACKAGE;



    public static void run(Class<?> clazz){

        setMainClass(clazz);


        /*
        D:\MiniSpringFramework\out\production\MiniSpringFramework\Main.class 에서
        D:\MiniSpringFramework\out\production\MiniSpringFramework 가져오기
         */
        setPrePathMainPackage();


        allDirUnderMainPackage().forEach(MySpringBootApplication::addSubClassToClassList);

        //classList.forEach(c -> System.out.println(c.getName()));
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
