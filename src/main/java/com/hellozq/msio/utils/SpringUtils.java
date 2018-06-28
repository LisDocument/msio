package com.hellozq.msio.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
public class SpringUtils implements ApplicationContextAware,InitializingBean{

    private Log log = LogFactory.getLog(SpringUtils.class);

    private static ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("--------------IOC容器注入成功--------------");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(SpringUtils.applicationContext == null){
            SpringUtils.applicationContext = applicationContext;
        }
        log.info("----------------------------------------------------------------");

        log.info("=========================信息注入成功=============================");

        log.info("----------------------------------------------------------------");
    }

    //获取applicationContext
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    //通过name获取 Bean.
    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }

    //通过class获取Bean.
    public static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }

    //通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }

    @Deprecated
    public static void addBean(Object o,String name){
        DefaultListableBeanFactory list = (DefaultListableBeanFactory)getApplicationContext().getAutowireCapableBeanFactory();
        if(list.containsBean(name)){
            return;
        }
        GenericBeanDefinition bean = new GenericBeanDefinition();
        bean.setBeanClass(o.getClass());
        bean.setPropertyValues(new MutablePropertyValues());
    }
}
