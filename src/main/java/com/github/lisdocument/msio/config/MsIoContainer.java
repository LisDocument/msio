package com.github.lisdocument.msio.config;

import com.github.lisdocument.msio.anno.*;
import com.github.lisdocument.msio.bean.common.CommonBean;
import com.github.lisdocument.msio.bean.common.ITransFunctionContainer;
import com.github.lisdocument.msio.bean.common.Operator;
import com.github.lisdocument.msio.bean.common.impl.DefaultOperator;
import com.github.lisdocument.msio.exception.UnsupportFormatException;
import com.github.lisdocument.msio.utils.ClassUtils;
import com.github.lisdocument.msio.utils.StringRegexUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

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
@SuppressWarnings("unused")
public class MsIoContainer {

    @Value("${spring.msIo.isHotCache:true}")
    private boolean hotDeploySign;

    private Class<? extends ITransFunctionContainer> containerClass;

    private ITransFunctionContainer iTransFunctionContainer;

    private final static Log log = LogFactory.getLog(MsIoContainer.class);

    private final static String CLASS_LABEL = "className";

    private final static String TRANSLATION_SIGN = "\\";

    private final static String FUNCTION_SIGN = "$$";

    private final static String FILE_NAME = "msio.json";

    private final static String MAPPING_ID = "id";

    private final static String MAPPING_NAME = "name";
    /**
     * 映射缓存池
     */
    private Map<String,LinkedHashMap<String,Information>> mappingCache = Maps.newHashMapWithExpectedSize(32);

    /**
     * 复杂映射层数缓存池
     */
    private Map<String, Integer> complexMappingCache = Maps.newHashMapWithExpectedSize(32);

    /**
     * 类映射缓冲池
     */
    private Map<String,Class> classCache = Maps.newHashMapWithExpectedSize(32);

    /**
     * 对象缓冲池
     */
    private Map<Class,Object> instanceCache = Maps.newHashMapWithExpectedSize(128);

    /**
     * 仅有热部署被启用时才启用临时映射存储池，若热部署标志为true的情况下，所有通过配置文件引入的对象映射会被驻留在此处，
     * 每次使用时都会调用该池的初始化方法重新进行加载，生产环境下进行关闭，关闭后数据会自动注入到mappingCache中，之后
     * 所有的映射操作将以mappingCache为准。
     */
    private ConcurrentHashMap<String,LinkedHashMap<String,Information>> temporaryMappingCache = new ConcurrentHashMap<>();

    /**
     * 推荐方式
     * @param iTransFunctionContainer 转出格式转换器
     */
    public MsIoContainer(ITransFunctionContainer iTransFunctionContainer) {
        this.iTransFunctionContainer = iTransFunctionContainer;
        containerClass = this.iTransFunctionContainer.getClass();
    }

    /**
     * 初始化对文件进行读取以及类进行加载，
     * 会被初始化方法进行调用
     * @param abstractMsConfigure 获取用户配置对象
     */
    void init(AbstractMsConfigure abstractMsConfigure){
        initJson();
        //类加载
        MsPackageScan scan = abstractMsConfigure.getClass().getAnnotation(MsPackageScan.class);
        if(null == scan ||  scan.packageName().length == 0){
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
     * 遍历匹配获取映射,仅允许数据(中文数据匹配)
     * @param titles 需要被匹配的头
     * @param isEg 是否英文匹配项
     * @return 返回映射
     */
    @SuppressWarnings("all")
    public String match(Collection<String> titles,boolean isEg){
        Map<String, LinkedHashMap<String,Information>> allRewords = new HashMap<>(16);
        if(hotDeploySign){
            initJson();
            allRewords.putAll(mappingCache);
            allRewords.putAll(temporaryMappingCache);
        }else{
            allRewords = mappingCache;
        }
        //删除复杂的映射
        for (String key : complexMappingCache.keySet()) {
            allRewords.remove(key);
        }
        List<String> keys = new ArrayList<>();
        for (String key : allRewords.keySet()) {
            LinkedHashMap<String, Information> value = allRewords.get(key);
            List<String> datum = null;
            //获取比较项
            if(!isEg) {
                datum = value.values().stream().map(Information::getName).collect(Collectors.toList());
            }else{
                datum = Lists.newArrayList(value.keySet());
            }
            long count = titles.parallelStream().filter(datum::contains).count();
            if(count != titles.size()){
                continue;
            }
            keys.add(key);
        }
        if(keys.size() == 1){
            return keys.get(0);
        }else if(keys.size() > 1){
            //如果匹配的数据大于
            Set<String> classKeys = classCache.keySet();
            return keys.stream().sorted((k1,k2) -> (classKeys.contains(k1) ? 1 : -1)).limit(1).collect(Collectors.toList()).get(0);
        }else{
            return null;
        }
    }

    /**
     * 获取复杂映射深度
     * @param key 键值
     * @return 深度
     */
    public int getDepthLevel(String key){
        return complexMappingCache.getOrDefault(key,1);
    }

    public int getDepthLevel(Class<?> key){
        MsOperator msOperator = key.getDeclaredAnnotation(MsOperator.class);
        return complexMappingCache.get(msOperator.value());
    }

    /**
     * 根据key获取其缓存的的类型，如果未找到，返回Map
     * @param key 索引值
     * @return 缓存的类
     */
    public Class<?> getClazz(String key){
        if(null == key){
            return null;
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
            return new LinkedHashMap<>();
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
            return new LinkedHashMap<>();
        }
        if(hotDeploySign){
            return getTemporary(key) == null ? getCache(key) : getTemporary(key);
        }else{
            return getCache(key);
        }
    }

    /**
     * 内部初始化使用的提取映射的方法，与热启动无关
     * @param key 建
     * @return 定向格式
     */
    private LinkedHashMap<String,Information> innerGet(String key){
        return null == temporaryMappingCache.get(key) ? mappingCache.get(key) : temporaryMappingCache.get(key);
    }

    /**
     * 内部初始化根据类的Class文件获取映射
     * @param key Class对象
     * @return 返回映射
     */
    private LinkedHashMap<String,Information> innerGet(Class<?> key){
        MsOperator operator = key.getAnnotation(MsOperator.class);
        if(operator == null){
            return new LinkedHashMap<>();
        }
        return innerGet(operator.value());
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
     * 为节省资源创建的一个可复用的方法体
     * @param <T> 指定的对象初始化
     * @param clazz Class对象，用于自动生成处理对象
     * @return Class对象生成的一个对应的对象
     */
    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<T> clazz){
        if(!instanceCache.containsKey(clazz)){
            try {
                instanceCache.put(clazz, clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("通过class创建对象失败，检查是否私有化了构造函数，或者未定义无参构造函数");
                e.printStackTrace();
            }
        }
        return (T)instanceCache.get(clazz);
    }

    /**
     * 配置文件的加载
     */
    private void initJson(){
        String jsonMapper;
        try {
            //获取文件外的配置文件
            File file = ResourceUtils.getFile("classpath:" + File.separator + FILE_NAME);
            if(file.exists()){
                jsonMapper = IOUtils.toString(new FileInputStream(file));
            }else {
                //若文件外无数据则应用文件内的数据
                jsonMapper = IOUtils.toString(this.getClass().getResourceAsStream(File.separator + FILE_NAME));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
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
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException | UnsupportFormatException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断层数
     * @param data 数据
     * @param i 初始层数，一般定义为1
     * @return 实际最深的层数
     */
    private int checkDepthLevel(LinkedHashMap<String,Information> data,int i){
        int fuc = 0;
        for (Information value : data.values()) {
            if(null != value.getChildren() && !value.getChildren().isEmpty()){
                //仅仅取每个元素的深度，会将每个元素的最大深度得出
                int fucTemp = checkDepthLevel(value.getChildren(),0) + 1;
                fuc = fucTemp > fuc ? fucTemp : fuc;
            }
        }
        return i + fuc;
    }

    /**
     * 复杂的映射导入
     * @param clazz 复杂的Clazz
     * @return 成功
     * @throws NoSuchMethodException 找不到对应的方法
     * @throws InstantiationException 某种错误
     * @throws IllegalAccessException 配置文件内容错误
     */
    private boolean addMappingComplex(Class<?> clazz) throws NoSuchMethodException,InstantiationException,IllegalAccessException{
        MsOperator operator = clazz.getDeclaredAnnotation(MsOperator.class);
        if(complexMappingCache.containsKey(operator.value())){
            throw new IllegalAccessException("领域模型指向id重复，重复id：" + operator.value() + ",请检查类：" +
                    clazz.getName() + "||" + classCache.get(operator.value()).getName());
        }
        classCache.put(operator.value(),clazz);

        LinkedHashMap<String,Information> mappingItem = new LinkedHashMap<>();
        Field[] fields = clazz.getDeclaredFields();
        //遍历变量
        for (Field field : fields) {
            if(field.getAnnotation(MsIgnore.class) != null){
                continue;
            }
            MsItem annotation = field.getDeclaredAnnotation(MsItem.class);
            if(annotation == null){
                continue;
            }
            //判断是否是复杂对象==>需要修改返回类型
            Class<?> type = field.getType();
            MsOperator msOperator = type.getDeclaredAnnotation(MsOperator.class);
            if(null != msOperator){
                LinkedHashMap<String, Information> value = innerGet(type);
                if(null == value || value.isEmpty()){
                    addMapping(type);
                    value = innerGet(type);
                }
                Information information = new Information();
                information.setFieldType(type);
                information.setName(field.getDeclaredAnnotation(MsItem.class).value());
                information.setChildren(value);
                mappingItem.put(field.getName(),information);
                continue;
            }
            Information information = new Information();
            //如果有方法名称的话
            if(!StringUtils.isEmpty(annotation.methodName())){
                try {
                    Method method = containerClass.getDeclaredMethod(annotation.methodName(), Object.class);
                    information.setMethod(method);
                    information.setInvokeObject(iTransFunctionContainer);
                }catch (NoSuchMethodException e){
                    log.error("获取对应的方法失败，原因可能为：容器中未定义该方法；容器中方法参数未定义为Object；自定义容器未初始化：检查并修正",e);
                    throw e;
                }
                //没有,自动调用默认的类型转换方法
            }else{
                information.setOperator(newInstance(annotation.transFormOperator()));
            }
            information.setFieldType(type);
            information.setAutomatic(field.getAnnotation(MsAutomatic.class));
            information.setName(StringRegexUtils.getOrDefault(annotation.value(),field.getName()));
            mappingItem.put(field.getName(),information);
        }
        mappingCache.put(operator.value(),mappingItem);
        //计算层数
        int depthLevel = checkDepthLevel(mappingItem, 1);
        complexMappingCache.put(operator.value(),depthLevel);
        return true;
    }

    /**
     * 注解添加映射方法,专门为Pojo类使用的
     * @param clazz 需要被映射的方法
     * @return 是否被添加
     * @throws NoSuchMethodException 找不到对应的方法
     * @throws InstantiationException 某种错误
     * @throws IllegalAccessException 配置文件内容错误
     */
    @SuppressWarnings("all")
    public boolean addMapping(Class<?> clazz) throws NoSuchMethodException,InstantiationException,IllegalAccessException{
        MsOperator msOperator = (MsOperator) clazz.getDeclaredAnnotation(MsOperator.class);
        //复杂映射计算直接推送至复杂计算单元执行
        if(0 != msOperator.subClazz().length){
            return addMappingComplex(clazz);
        }
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
                    information.setInvokeObject(iTransFunctionContainer);
                }catch (NoSuchMethodException e){
                    log.error("获取对应的方法失败，原因可能为：容器中未定义该方法；容器中方法参数未定义为Object；自定义容器未初始化：检查并修正",e);
                    throw e;
                }
                //没有,自动调用默认的类型转换方法
            }else{
                information.setOperator(newInstance(annotation.transFormOperator()));
            }
            information.setFieldType(field.getType());
            information.setAutomatic(field.getAnnotation(MsAutomatic.class));
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
     * @throws ClassNotFoundException 找不到对应的类
     * @throws NoSuchMethodException 找不到对应的方法
     * @throws NoSuchFieldException 找不到对应的属性
     * @throws IllegalAccessException 配置文件内容错误
     * @throws UnsupportFormatException 非法格式
     */
    @SuppressWarnings("all")
    public boolean addMapping(Map jsonData) throws ClassNotFoundException,NoSuchMethodException,IllegalAccessException,NoSuchFieldException,UnsupportFormatException{
        if(jsonData.isEmpty()){
            return false;
        }
        for (Object o : jsonData.entrySet()) {
            Map.Entry<String,LinkedHashMap<Object,Object>> item = (Map.Entry<String,LinkedHashMap<Object,Object>>) o;
            addMappingItem(item.getKey().toUpperCase(),item.getValue());
        }
        return true;
    }


    /**
     * 对复杂Map对象的解析方法，
     * @param jsonData 复杂map
     * @param key 键
     * @throws ClassNotFoundException 找不到对应的类
     * @throws NoSuchMethodException 找不到对应的方法
     * @throws NoSuchFieldException 找不到对应的属性
     * @throws IllegalAccessException 配置文件内容错误
     * @throws UnsupportFormatException 非法格式
     */
    @SuppressWarnings("unchecked")
    private void addMappingComplex(Object key,Map jsonData) throws ClassNotFoundException,NoSuchMethodException,IllegalAccessException,NoSuchFieldException,UnsupportFormatException{
        Class<?> pojo = null;
        //若有该字段，则标识这个映射对象为一个类（配置文件配置的类）,获取后将其移除
        if(jsonData.containsKey(CLASS_LABEL)){
            pojo = Class.forName(jsonData.remove(CLASS_LABEL).toString());
            classCache.put(key.toString(),pojo);
        }
        LinkedHashMap<String,Information> mappingItem = Maps.newLinkedHashMapWithExpectedSize(16);
        //网上求证数据项标明顺序正常
        for (Object egName : jsonData.keySet()) {
            //复杂Map递归求证
            if(jsonData.get(egName) instanceof Map){
                Object name = ((Map) jsonData.get(egName)).remove(MAPPING_NAME);
                if(null == name){
                    throw new UnsupportFormatException("配置文件映射时内部集egName找不到必须存在的name属性");
                }
                Information info = new Information();
                info.setName(name.toString());
                //如果存在id项，使用id项进行分析->直接将已经缓存的mapping调入并抛弃原先map中的数据
                if(((Map) jsonData.get(egName)).containsKey(MAPPING_ID)){
                    String id = ((Map) jsonData.get(egName)).remove(MAPPING_ID).toString();
                    LinkedHashMap<String, Information> valueTemp = innerGet(id);
                    if(null == valueTemp){
                        throw new RuntimeException(new UnsupportFormatException("未找到对应子映射，请确认是否未定义或者是否位置放置在该映射之前"));
                    }
                    info.setChildren(valueTemp);
                    mappingItem.put(egName.toString(),info);
                    continue;
                }
                //尝试获取是否已经缓存了数据，如果获取数据返回null则放入map重新解析
                LinkedHashMap<String, Information> value = innerGet(egName.toString());
                if(value == null){
                    addMappingItem(egName.toString(),(LinkedHashMap<Object,Object>) jsonData.get(egName));
                    value = innerGet(egName.toString());
                }
                info.setChildren(value);
                mappingItem.put(egName.toString(),info);
                //映射直接获取
                continue;
            }
            String cnName = jsonData.get(egName).toString();
            Information info = new Information();
            //方法获取
            int index = StringRegexUtils.checkIsContain(cnName, FUNCTION_SIGN);
            if(index == -1){
                //将数据从转义恢复
                info.setName(cnName.replace("\\$$","$$"));
                info.setOperator(newInstance(DefaultOperator.class));
            }else{
                info.setName(cnName.substring(0,index));
                info.setInvokeObject(iTransFunctionContainer);
                info.setMethod(containerClass.getDeclaredMethod(cnName.substring(index + 2),Object.class));
            }
            if(pojo != null){
                Field field = pojo.getDeclaredField(egName.toString());
                info.setFieldType(field.getType());
            }
            //将转义的字段删除
            if(egName.equals(TRANSLATION_SIGN + CLASS_LABEL)
                ||egName.equals(TRANSLATION_SIGN + MAPPING_ID)
                ||egName.equals(TRANSLATION_SIGN + MAPPING_NAME)){
                mappingItem.put(egName.toString().substring(1),info);
            }else{
                mappingItem.put(egName.toString(),info);
            }
        }
        //计算深度
        int depthLevel = checkDepthLevel(mappingItem, 1);
        complexMappingCache.put(key.toString(),depthLevel);
        if(hotDeploySign){
            temporaryMappingCache.put(key.toString(),mappingItem);
        }else{
            Class clazz = classCache.get(key.toString());
            if(mappingCache.containsKey(key.toString())){
                throw new IllegalAccessException("领域模型指向id重复，重复id：" + key.toString() +
                        (clazz == null ? ",请检查配置文件配置项是否重复：" : ("pojo类重复，类名为:" + clazz.getName())));
            }
            mappingCache.put(key.toString(),mappingItem);
        }
    }

    /**
     * 文件配置项单项处理
     * @param key 键
     * @param information 键对应的映射
     * @throws ClassNotFoundException 找不到对应的类
     * @throws NoSuchMethodException 找不到对应的方法
     * @throws NoSuchFieldException 找不到对应的属性
     * @throws IllegalAccessException 配置文件内容错误
     * @throws UnsupportFormatException 非法格式
     */
    private void addMappingItem(String key, LinkedHashMap<Object, Object> information)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException,UnsupportFormatException {
        if(information.values().stream().anyMatch(obj -> obj instanceof Map)){
            addMappingComplex(key,information);
            return;
        }
        LinkedHashMap<String, Information> mappingItem = new LinkedHashMap<>();
        Class pojo = null;
        //若有该字段，则标识这个映射对象为一个类（配置文件配置的类）,获取后将其移除
        if(information.containsKey(CLASS_LABEL)){
            pojo = Class.forName(information.remove(CLASS_LABEL).toString());
            classCache.put(key,pojo);
        }
        //网上求证数据项标明顺序正常
        for (Object egName : information.keySet()) {
            String cnName = information.get(egName).toString();
            Information info = new Information();
            //方法获取
            int index = StringRegexUtils.checkIsContain(cnName, FUNCTION_SIGN);
            if(index == -1){
                info.setName(cnName.replace("\\$$","$$"));
                info.setOperator(newInstance(DefaultOperator.class));
            }else{
                info.setName(cnName.substring(0,index));
                info.setInvokeObject(iTransFunctionContainer);
                info.setMethod(containerClass.getDeclaredMethod(cnName.substring(index + 2),Object.class));
            }
            if(pojo != null){
                Field field = pojo.getDeclaredField(egName.toString());
                info.setFieldType(field.getType());
            }
            //若实在要使用className作为一个属性传入，进行转义即可
            if(egName.equals(TRANSLATION_SIGN + CLASS_LABEL)){
                mappingItem.put(egName.toString().substring(1),info);
            }else{
                mappingItem.put(egName.toString(),info);
            }
        }
        if(hotDeploySign){
            temporaryMappingCache.put(key,mappingItem);
        }else{
            Class clazz = classCache.get(key);
            if(mappingCache.containsKey(key)){
                throw new IllegalAccessException("领域模型指向id重复，重复id：" + key +
                        (clazz == null ? ",请检查配置文件配置项是否重复：" : ("pojo类重复，类名为:" + clazz.getName())));
            }
            mappingCache.put(key,mappingItem);
        }
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

        private MsAutomatic automatic;

        private Class<?> fieldType;

        private LinkedHashMap<String,Information> children;

        public LinkedHashMap<String, Information> getChildren() {
            return children;
        }

        public Information setChildren(LinkedHashMap<String, Information> children) {
            this.children = children;
            return this;
        }

        public Class<?> getFieldType() {
            return fieldType;
        }

        private void setFieldType(Class<?> fieldType) {
            this.fieldType = fieldType;
        }

        public MsAutomatic getAutomatic() {
            return automatic;
        }

        private void setAutomatic(MsAutomatic automatic) {
            this.automatic = automatic;
        }

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
        public String toString() {
            return "Information{" +
                    "name='" + name + '\'' +
                    ", method=" + method +
                    ", invokeObject=" + invokeObject +
                    ", operator=" + operator +
                    ", automatic=" + automatic +
                    ", fieldType=" + fieldType +
                    ", children=" + children +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }
            Information that = (Information) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(method, that.method) &&
                    Objects.equals(invokeObject, that.invokeObject) &&
                    Objects.equals(operator, that.operator) &&
                    Objects.equals(automatic, that.automatic) &&
                    Objects.equals(fieldType, that.fieldType) &&
                    Objects.equals(children, that.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, method, invokeObject, operator, automatic, fieldType, children);
        }
    }
}
