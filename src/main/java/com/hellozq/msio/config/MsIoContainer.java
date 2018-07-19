package com.hellozq.msio.config;

import com.hellozq.msio.anno.MsIgnore;
import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import com.hellozq.msio.anno.MsPackageScan;
import com.hellozq.msio.bean.common.CommonBean;
import com.hellozq.msio.bean.common.Operator;
import com.hellozq.msio.bean.common.TransFunctionContainer;
import com.hellozq.msio.utils.ClassUtils;
import com.hellozq.msio.utils.StringRegexUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 内部映射生成方法
 * MsIo上下文全文容器，包含配置项缓冲项等
 */
@ConditionalOnMissingBean(value = MsIoContainer.class)
@Component
@SuppressWarnings("unused")
public class MsIoContainer {

    @Value("${spring.msIo.isHotCache:true}")
    private boolean hotDeploySign;

    private Class<? extends TransFunctionContainer> containerClass;

    private TransFunctionContainer transFunctionContainer;

    private AbstractMsConfigure abstractMsConfigure;

    private final Log log = LogFactory.getLog(MsIoContainer.class);

    private final static String CLASS_LABEL = "className";

    private final static String TRANSLATION_SIGN = "\\";

    private final static String FUNCTION_SIGN = "$$";

    private final static String FILE_NAME = "msio.json";
    /**
     * 映射缓存池
     */
    private Map<String,LinkedHashMap<String,Information>> mappingCache = new HashMap<>();

    /**
     * 类映射缓冲池
     */
    private Map<String,Class> classCache = new HashMap<>();

    /**
     * 仅有热部署被启用时才启用临时映射存储池，若热部署标志为true的情况下，所有通过配置文件引入的对象映射会被驻留在此处，
     * 每次使用时都会调用该池的初始化方法重新进行加载，生产环境下进行关闭，关闭后数据会自动注入到mappingCache中，之后
     * 所有的映射操作将以mappingCache为准。
     */
    private ConcurrentHashMap<String,LinkedHashMap<String,Information>> temporaryMappingCache = new ConcurrentHashMap<>();

    /**
     * 推荐方式
     * @param transFunctionContainer 导出操作容器
     */
    @Autowired
    public MsIoContainer(TransFunctionContainer transFunctionContainer,AbstractMsConfigure abstractMsConfigure) {
        this.transFunctionContainer = transFunctionContainer;
        this.abstractMsConfigure = abstractMsConfigure;
        containerClass = transFunctionContainer.getClass();
    }

    /**
     * 初始化对文件进行读取以及类进行加载
     */
    @PostConstruct
    public void init(){
        initJson();
        //类加载
        MsPackageScan scan = abstractMsConfigure.getClass().getAnnotation(MsPackageScan.class);
        if(scan == null || StringUtils.isEmpty(scan)){
            return;
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String packageName : scan.packageName()) {
            classes.addAll(ClassUtils.getClasses(packageName));
        }
        for (Class<?> clazz : classes) {
            if(clazz.getAnnotation(MsOperator.class) != null){
                try {
                    addMapping(clazz);
                }catch (NoSuchMethodException | InstantiationException | IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 遍历匹配获取映射,仅允许数据
     * @param titles 需要被匹配的头
     * @return 返回映射
     */
    @SuppressWarnings("all")
    public String match(List<String> titles){
        Map<String, LinkedHashMap<String,Information>> allRewords = new HashMap<>(16);
        if(hotDeploySign){
            initJson();
            allRewords.putAll(mappingCache);
            allRewords.putAll(temporaryMappingCache);
        }else{
            allRewords = mappingCache;
        }
        List<String> keys = new ArrayList<>();
        for (String key : allRewords.keySet()) {
            LinkedHashMap<String, Information> value = allRewords.get(key);
            List<String> datum = value.values().stream().map(Information::getName).collect(Collectors.toList());
            long count = titles.parallelStream().filter(datum::contains).count();
            if(count != titles.size()){
                continue;
            }
            keys.add(key);
        }
        if(keys.size() == 1){
            return keys.get(0);
        }else{
            Set<String> classKeys = classCache.keySet();
            return keys.stream().sorted((k1,k2) -> (classKeys.contains(k1) ? 1 : -1)).limit(1).collect(Collectors.toList()).get(0);
        }
    }

    /**
     * 根据key获取其缓存的的类型，如果未找到，返回Map
     * @param key 索引值
     * @return 缓存的类
     */
    public Class<?> getClazz(String key){
        if(null == key){
            return Map.class;
        }
        return classCache.getOrDefault(key, Map.class);
    }

    /**
     * 根据类的Class文件获取映射
     * @param key Class对象
     * @return 返回映射
     */
    public LinkedHashMap<String,Information> get(Class<?> key){
        MsOperator operator = key.getAnnotation(MsOperator.class);
        if(operator == null){
            return null;
        }
        return get(operator.value());
    }

    /**
     * 根据设置的时候给定的键获取其结构
     * @param key 键
     * @return 一个类或者一个Map的定向格式
     */
    public LinkedHashMap<String,Information> get(String key){
        if(null == key){
            return null;
        }
        if(hotDeploySign){
            return getTemporary(key) == null ? getCache(key) : getTemporary(key);
        }else{
            return getCache(key);
        }
    }

    /**
     * 从临时缓存池提取数据（热启动的时使用）
     * @param key 键
     * @return 定向格式
     */
    private LinkedHashMap<String,Information> getTemporary(String key){
        initJson();
        return temporaryMappingCache.get(key);
    }

    /**
     * 从常量库提取数据（pojo类注解实现的映射一般会被固化在这个池中不可修改）
     * @param key 键
     * @return 定向格式
     */
    private LinkedHashMap<String,Information> getCache(String key){
        return mappingCache.get(key);
    }


    /**
     * 配置文件的加载
     */
    private void initJson(){
        String jsonMapper = null;
        try {
            //获取文件外的配置文件
            ApplicationHome applicationHome = new ApplicationHome(getClass());
            File dir = applicationHome.getDir();
            File file = new File(dir.getPath() + File.separator + FILE_NAME);
            if(file.exists()){
                jsonMapper = IOUtils.toString(new FileInputStream(file));
            }else {
                //若文件外无数据则应用文件内的数据
                jsonMapper = IOUtils.toString(this.getClass().getResourceAsStream(File.separator + FILE_NAME));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            log.error("map配置文件为空，或者文件内容为空，默认用户不需要配置操作，初始化配置文件操作跳过");
            return;
        }
        try {
            LinkedHashMap linkedHashMap = CommonBean.OBJECT_MAPPER.readValue(jsonMapper, LinkedHashMap.class);
            addMapping(linkedHashMap);
        } catch (IOException e) {
            log.error("配置文件格式错误，请好好检查");
            e.printStackTrace();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 注解添加映射方法,专门为Pojo类使用的
     * @param clazz 需要被映射的方法
     * @return 是否被添加
     */
    @SuppressWarnings("all")
    public boolean addMapping(Class clazz) throws NoSuchMethodException,InstantiationException,IllegalAccessException{
        MsOperator msOperator = (MsOperator) clazz.getDeclaredAnnotation(MsOperator.class);
        if(mappingCache.containsKey(msOperator.value())){
            throw new IllegalAccessException("领域模型指向id重复，重复id：" + msOperator.value() + ",请检查类：" +
                    clazz.getName() + "||" + classCache.get(msOperator.value()).getName());
        }
        classCache.put(msOperator.value(),clazz);

        LinkedHashMap<String,Information> mappingItem = new LinkedHashMap<>();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if(field.getAnnotation(MsIgnore.class) != null){
                continue;
            }
            MsItem annotation = field.getDeclaredAnnotation(MsItem.class);
            if(annotation == null){
                continue;
            }
            Information information = new Information();
            //如果有方法名称的话
            if(!StringUtils.isEmpty(annotation.methodName())){
                try {
                    Method method = containerClass.getDeclaredMethod(annotation.methodName(), Object.class);
                    information.setMethod(method);
                    information.setInvokeObject(transFunctionContainer);
                }catch (NoSuchMethodException e){
                    log.error("获取对应的方法失败，原因可能为：容器中未定义该方法；容器中方法参数未定义为Object；自定义容器未初始化：检查并修正");
                    throw e;
                }
                //没有,自动调用默认的类型转换方法
            }else{
                //需要优化
                information.setOperator(annotation.transFormOperator().newInstance());
            }
            information.setName(StringRegexUtils.getOrDefault(annotation.value(),field.getName()));
            mappingItem.put(field.getName(),information);
        }

        mappingCache.put(msOperator.value(),mappingItem);
        return true;
    }

    /**
     * 对Map对象的解析方法，理论上这部分数据应有配置文件中的数据实现
     * 理论上对Map进行维护
     * @param jsonData 翻译过来的数据
     * @return 是否成功
     */
    @SuppressWarnings("all")
    public boolean addMapping(Map jsonData) throws ClassNotFoundException,NoSuchMethodException,IllegalAccessException{
        if(jsonData.isEmpty()){
            return false;
        }
        for (Object o : jsonData.entrySet()) {
            Map.Entry<String,LinkedHashMap<Object,Object>> item = (Map.Entry<String,LinkedHashMap<Object,Object>>) o;
            LinkedHashMap<String,Information> mappingItem = new LinkedHashMap<>();
            LinkedHashMap<Object, Object> information = item.getValue();
            //若有该字段，则标识这个映射对象为一个类（配置文件配置的类）,获取后将其移除
            if(information.containsKey(CLASS_LABEL)){
                Class pojo = Class.forName(information.remove(CLASS_LABEL).toString());
                classCache.put(item.getKey(),pojo);
            }
            //网上求证数据项标明顺序正常
            for (Object egName : information.keySet()) {
                String cnName = information.get(egName).toString();
                Information info = new Information();
                //方法获取
                int index = StringRegexUtils.checkIsContain(cnName, FUNCTION_SIGN);
                if(index == -1){
                    info.setName(cnName);
                }else{
                    info.setName(cnName.substring(0,index));
                    info.setInvokeObject(transFunctionContainer);
                    info.setMethod(containerClass.getDeclaredMethod(cnName.substring(index - 2),Object.class));
                }
                //若实在要使用className作为一个属性传入，进行转义即可
                if(egName.equals(TRANSLATION_SIGN + CLASS_LABEL)){
                    mappingItem.put(egName.toString().substring(1),info);
                }else{
                    mappingItem.put(egName.toString(),info);
                }
            }
            if(hotDeploySign){
                temporaryMappingCache.put(item.getKey(),mappingItem);
            }else{
                Class clazz = classCache.get(item.getKey());
                if(mappingCache.containsKey(item.getKey())){
                    throw new IllegalAccessException("领域模型指向id重复，重复id：" + item.getKey() +
                            (clazz == null ? ",请检查配置文件配置项是否重复：" : ("pojo类重复，类名为:" + clazz.getName())));
                }
            }
        }
        return true;
    }

    /**
     * @author bin
     * 用于存储基本信息的数据集
     * 保存当前代理对象，当前变量的中文注释，以及其缓冲的方法
     */
    public class Information{

        private String name;

        private Method method;

        private Object invokeObject;

        private Operator operator;

        public Operator getOperator() {
            return operator;
        }

        private void setOperator(Operator operator) {
            this.operator = operator;
        }

        public Method getMethod() {
            return method;
        }

        private void setMethod(Method method) {
            this.method = method;
        }

        public Object getInvokeObject() {
            return invokeObject;
        }

        private void setInvokeObject(Object invokeObject) {
            this.invokeObject = invokeObject;
        }

        public String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Information that = (Information) o;

            return (name != null ? name.equals(that.name) : that.name == null) &&
                    (method != null ? method.equals(that.method) : that.method == null) &&
                    (invokeObject != null ? invokeObject.equals(that.invokeObject) : that.invokeObject == null) &&
                    (operator != null ? operator.equals(that.operator) : that.operator == null);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (method != null ? method.hashCode() : 0);
            result = 31 * result + (invokeObject != null ? invokeObject.hashCode() : 0);
            result = 31 * result + (operator != null ? operator.hashCode() : 0);
            return result;
        }
    }
}
