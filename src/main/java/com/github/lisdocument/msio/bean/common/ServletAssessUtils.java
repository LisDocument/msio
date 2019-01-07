package com.github.lisdocument.msio.bean.common;

import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.OutputBuffer;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.buf.MessageBytes;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 辅助工具，用于修改并模拟操作
 *
 * @author bin
 */
final class ServletAssessUtils{

    private Log log = LogFactory.getLog(ServletAssessUtils.class);

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();



    /**
     * 获取请求响应应该返回的对象信息
     * @param request 请求
     * @param response 响应
     * @param handle 请求上下文
     * @param <T> 泛型，一般无用，预留字段
     * @return 接口获取的数据
     */
    @SuppressWarnings("unchecked")
    @Nullable
    <T> T getRequestResult(HttpServletRequest request, HttpServletResponse response,HandlerExecutionChain handle){
        ServletWebRequest webRequest = new ServletWebRequest(request, response);
        HandlerMethod handlerMethod = (HandlerMethod) handle.getHandler();
        try {
            Object[] methodArgumentValues = getMethodArgumentValues(webRequest, handlerMethod);
            Object invoke = handlerMethod.getMethod().invoke(handlerMethod.getBean(), methodArgumentValues);
            return (T)invoke;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        finally {
            webRequest.requestCompleted();
        }
    }

    /**
     * 获取方法的对应参数，获取原定参数相对应的数组
     * @param request 请求
     * @param method 切割出来该请求的方法
     * @param providedArgs 预留参数
     * @return 参数
     * @throws Exception 错误
     */
    @NonNull
    private Object[] getMethodArgumentValues(@NonNull ServletWebRequest request,@NonNull HandlerMethod method, Object... providedArgs) throws Exception {

        MethodParameter[] parameters = method.getMethodParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            args[i] = resolveProvidedArgument(parameter, providedArgs);
            if (args[i] != null) {
                continue;
            }
            Map<String, String[]> parameterMap = request.getRequest().getParameterMap();
            if(parameterMap.containsKey(parameter.getParameterName())){
                String[] values = parameterMap.get(parameter.getParameterName());
                if(parameter.getParameterType().isArray()){
                    args[i] = values;
                }
                if(null != values && values.length == 1) {
                    args[i] = values[0];
                }
            }
        }
        return args;
    }

    @Nullable
    private Object resolveProvidedArgument(@NonNull MethodParameter parameter, @Nullable Object... providedArgs) {
        if (providedArgs == null) {
            return null;
        }
        for (Object providedArg : providedArgs) {
            if (parameter.getParameterType().isInstance(providedArg)) {
                return providedArg;
            }
        }
        return null;
    }



    /**
     * 解析response中方法返回的数据并打包为对象
     * @param response 响应
     * @param clazz 映射目标
     * @param <T> 泛型对象
     * @return 解析完之后的对象
     * @throws IOException 翻译时获取刘对象失败
     * @throws NoSuchFieldException 找不到对应字段报错，一般由于版本更迭，response结构改变
     * @throws IllegalAccessException 反射时候出错
     */
    @Deprecated
    @NonNull
    <T> T getResponseBody(HttpServletResponse response, Class<T> clazz)  throws IOException, NoSuchFieldException, IllegalAccessException {
        CoyoteOutputStream outputStream = (CoyoteOutputStream)response.getOutputStream();
        Field ob = CoyoteOutputStream.class.getDeclaredField("ob");
        ob.setAccessible(true);
        OutputBuffer outputBuffer = (OutputBuffer) ob.get(outputStream);
        Field bb = outputBuffer.getClass().getDeclaredField("bb");
        bb.setAccessible(true);
        ByteBuffer byteBuffer = (ByteBuffer) bb.get(outputBuffer);
        byte[] array = byteBuffer.array();
        String s = new String(array, "UTF-8");
        s = s.substring(0,s.lastIndexOf("}")+1);
        return CommonBean.OBJECT_MAPPER.readValue(s, clazz);
    }

    /**
     * 通过反射更改request的url，切除用于进入这个servlet的第一层url，
     * 使用剩下的url进行模拟请求数据处理
     * @param request 请求实体
     */
    void changeRequestURI(@NonNull ServletRequest request){
        RequestFacade requestFacade = (RequestFacade) request;
        Class clazz = RequestFacade.class;
        try{
            Field field = clazz.getDeclaredField("request");
            field.setAccessible(true);
            Request req = (Request) field.get(requestFacade);
            Class<? extends Request> aClass = req.getClass();
            Field coyoteRequest = aClass.getDeclaredField("coyoteRequest");
            coyoteRequest.setAccessible(true);
            org.apache.coyote.Request req1 = (org.apache.coyote.Request) coyoteRequest.get(req);
            Class<org.apache.coyote.Request> requestClass = org.apache.coyote.Request.class;
            //获取org.apache.coyote.Request中保存路径的字段
            Field uriMBField= requestClass.getDeclaredField("uriMB");
            uriMBField.setAccessible(true);
            MessageBytes uriMB=(MessageBytes)uriMBField.get(req1);
            //这里就是改变路径的地方
            String path = uriMB.toString();
            path = path.substring(1);
            int i = path.indexOf("/");
            path = path.substring(i);
            //给值
            uriMB.setString(path);
            uriMBField.set(req1,uriMB);
        }catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取URI
     * @param request 请求实体
     * @return 返回当前Uri
     */
    @NonNull
    String getRequestUri(@NonNull HttpServletRequest request) {
        String uri = (String)request.getAttribute("javax.servlet.include.request_uri");
        if (uri == null) {
            uri = request.getRequestURI();
        }

        return uri;
    }

    boolean applyPreHandle(HandlerExecutionChain chain,HttpServletRequest request, HttpServletResponse response) throws Exception {
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (HandlerInterceptor interceptor : interceptors) {
                if (!interceptor.preHandle(request, response, chain.getHandler())) {
                    afterInterceptorHandle(chain, request, response);
                    return false;
                }
            }
        }

        return true;
    }

    private void afterInterceptorHandle(HandlerExecutionChain chain,HttpServletRequest request,HttpServletResponse response){
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors)){
            for (HandlerInterceptor interceptor : interceptors) {
                try {
                    interceptor.afterCompletion(request, response,chain.getHandler(), null);
                }catch (Throwable e){
                    log.error("HandlerInterceptor.afterCompletion threw exception", e);
                }
            }
        }
    }

    /**
     * 拦截器的收尾工作
     * @param chain 当前访问上下文
     * @param request 请求
     * @param response 响应
     * @param mv 映射静态资源
     * @throws Exception 错误
     */
    void applyPostHandle(HandlerExecutionChain chain,HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
            throws Exception {

        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = interceptors.length - 1; i >= 0; i--) {
                HandlerInterceptor interceptor = interceptors[i];
                interceptor.postHandle(request, response, chain.getHandler(), mv);
            }
        }
    }

    void applyAfterConcurrentHandlingStarted(HandlerExecutionChain chain,HttpServletRequest request, HttpServletResponse response) {
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = interceptors.length - 1; i >= 0; i--) {
                if (interceptors[i] instanceof AsyncHandlerInterceptor) {
                    try {
                        AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptors[i];
                        asyncInterceptor.afterConcurrentHandlingStarted(request, response, chain.getHandler());
                    }
                    catch (Throwable ex) {
                        log.error("Interceptor [" + interceptors[i] + "] failed in afterConcurrentHandlingStarted", ex);
                    }
                }
            }
        }
    }
}
