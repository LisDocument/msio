package com.hellozq.msio.config;

import com.hellozq.msio.anno.MsIgnore;
import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import com.hellozq.msio.bean.common.Operator;
import com.hellozq.msio.bean.common.TransFunctionContainer;
import com.hellozq.msio.utils.StringRegexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 内部映射生成方法
 * MsIo上下文全文容器，包含配置项缓冲项等
 */
@Component
@SuppressWarnings("unused")
public class MsIoContainer {

    @Value("${spring.msIo.isHotCache:true}")
    private boolean hotDeploySign;

    private Class<? extends TransFunctionContainer> containerClass;

    private final TransFunctionContainer transFunctionContainer;

    private final Log log = LogFactory.getLog(MsIoContainer.class);

    private final static String CLASS_LABEL = "className";

    private final static String TRANSLATION_SIGN = "\\";

    private final static String FUNCTION_SIGN = "$$";
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
    public MsIoContainer(TransFunctionContainer transFunctionContainer) {
        this.transFunctionContainer = transFunctionContainer;
        containerClass = transFunctionContainer.getClass();
    }


    /**
     * 注解添加映射方法,专门为Pojo类使用的
     * @param clazz 需要被映射的方法
     * @return 是否被添加
     */
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
            information.setName(annotation.value());
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
    public boolean addMapping(Map<String,LinkedHashMap<String,String>> jsonData) throws ClassNotFoundException,NoSuchMethodException{
        if(jsonData.isEmpty()){
            return false;
        }
        for (Map.Entry<String, LinkedHashMap<String, String>> item : jsonData.entrySet()) {
            LinkedHashMap<String,Information> mappingItem = new LinkedHashMap<>();
            Map<String, String> information = item.getValue();
            //若有该字段，则标识这个映射对象为一个类（配置文件配置的类）,获取后将其移除
            if(information.containsKey(CLASS_LABEL)){
                Class pojo = Class.forName(information.remove(CLASS_LABEL));
                classCache.put(item.getKey(),pojo);
            }
            information.forEach((egName,cnName) ->{
                Information info = new Information();
                //方法获取
                int index = StringRegexUtils.checkIsContain(cnName, FUNCTION_SIGN);
                if(index == -1){
                    info.setName(cnName);
                }else{
                    info.setName(cnName.substring(0,index));
                    info.setInvokeObject(transFunctionContainer);
                    //info.setMethod(containerClass.getDeclaredMethod(cnName.substring(index - 2),Object.class));
                }
                //若实在要使用className作为一个属性传入，进行转义即可
                if(egName.equals(TRANSLATION_SIGN + CLASS_LABEL)){
                    mappingItem.put(egName.substring(1),info);
                }
            });
        }
        return true;
    }
    /**

     * @author bin
     * 用于存储基本信息的数据集
     * 保存当前代理对象，当前变量的中文注释，以及其缓冲的方法
     */
    private class Information{

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
    }
}
