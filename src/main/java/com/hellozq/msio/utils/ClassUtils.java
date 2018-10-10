package com.hellozq.msio.utils;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * 类相关工具
 * @author bin
 */
@SuppressWarnings("all")
public class ClassUtils {

    /**
     * 该工具类维护的映射方法缓存
     * 不对外展示只对工具类中的方法负责
     */
    private static ConcurrentHashMap<String, MethodAccess> methodAccessCache = new ConcurrentHashMap<>(128);

    /**
     * 私有获取MethodAccess的方法，
     * @param clazz
     * @return
     */
    public static MethodAccess getMethodAccess(Class<?> clazz){
        String name = clazz.getName();
        if(!methodAccessCache.containsKey(name)){
            methodAccessCache.put(name,MethodAccess.get(clazz));
        }
        return methodAccessCache.get(name);
    }
    /**
     * 根据属性名自动获取其中的数据
     * @param fieldName 属性名称
     * @param o 对象
     * @return 该属性值
     */
    public static Object getFieldValue(String fieldName, Object o, Class<?> clazz){
        String methodName = "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
        return getMethodAccess(clazz).invoke(o,methodName);
    }

    /**
     * 根据属性名自动赋值
     * @param fieldValue 属性值
     * @param fieldName 属性名
     * @param o 对象
     */
    public static void setFieldValue(Object fieldValue,String fieldName,Object o,Class<?> clazz){
        String methodName = "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
        getMethodAccess(clazz).invoke(o,methodName,fieldValue);
    }


    /**
     * 默认获取比对map
     * @param headLine keySet
     * @param jsonMappings 映射
     * @return 对比映射
     */
    public static LinkedHashMap<String,String> getMapping(Set<String> headLine, Collection<LinkedHashMap<String,String>> jsonMappings){
        for (LinkedHashMap<String,String> value : jsonMappings) {
            boolean sign = true;
            for (String s : headLine) {
                if(!value.containsKey(s)){
                    sign = false;
                }
            }
            if(sign){
                LinkedHashMap<String,String> finalResult = new LinkedHashMap<>();
                value.forEach((k,v) ->{
                    if(headLine.contains(k)){
                        finalResult.put(k,v);
                    }
                });
                return finalResult;
            }
        }
        return null;
    }

    /**
     * 从包package中获取所有的Class
     * @param packageName 包名
     * @return
     */
    public static List<Class<?>> getClasses(String packageName){
        List<Class<?>> classes = new ArrayList<>();
        //是否循环迭代
        boolean recursive = true;
        String packageDirName = packageName.replace(".","/");
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

            while (dirs.hasMoreElements()){
                //获取下一个元素
                URL url = dirs.nextElement();
                //得到协议名称
                String protocol = url.getProtocol();
                //文件形式保存
                if("file".equals(protocol)){
                    //获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(),"UTF-8");
                    //以文件的方式扫描整个包下的文件，添加到集合
                    findAndAddClassesInPackageByFile(packageName,filePath,recursive,classes);
                }else if("jar".equals(protocol)){
                    //如果是jar包文件，定义一个JarFile
                    JarFile jarFile;
                    try{
                        jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()){
                            //获取jar的一个实体 目录或其他
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            //如果是以/开头的
                            if(name.charAt(0) == '/'){
                                name = name.substring(1);
                            }
                            //如果前半部分和定义的包名相同
                            if(name.startsWith(packageDirName)){
                                int idx = name.lastIndexOf("/");
                                if(idx != -1){
                                    //获取包名 把'/'替换成'.'
                                    packageName = name.substring(0,idx).replace("/",".");
                                }
                                //如果可以迭代下去，并且是一个包
                                if((idx != -1) || recursive){
                                    //如果是一个.class文件 而且不是目录
                                    if(name.endsWith(".class") && !entry.isDirectory()){
                                        //去掉后面的.class 获取真正的类名
                                        String className = name.substring(packageName.length() + 1
                                                ,name.length() - 6);
                                        try {
                                            classes.add(Class.forName(packageName + "." +className));
                                        }catch (ClassNotFoundException e){
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     * @param packageName 包名
     * @param packagePath 包的地址
     * @param recursive 迭代标志
     * @param classes 保存位置
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath
            , final boolean recursive,List<Class<?>> classes){
        //获取此包的目录，建立一个File
        File dir = new File(packagePath);
        //如果不存在也不是目录就直接返回
        if(!dir.exists() || !dir.isDirectory()){
            return;
        }
        //如果存在 就获取包下的所有文件 包括目录
        File[] dirFiles = dir.listFiles((pathname) ->{
                    //自定义过滤规则 如果可以循环或则以.class结尾的文件
                    return (recursive && pathname.isDirectory())
                            || (pathname.getName().endsWith(".class"));
                }
        );
        //循环所有文件
        for (File file : dirFiles) {
            //如果目录,则继续扫描
            if(file.isDirectory()){
                findAndAddClassesInPackageByFile(packageName+"."+file.getName(),
                        file.getAbsolutePath(),
                        recursive,
                        classes);
            }else{
                //如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0,file.getName().length() - 6);
                try{
                    classes.add(Class.forName(packageName + "." + className));
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
