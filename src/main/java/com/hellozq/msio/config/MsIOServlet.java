package com.hellozq.msio.config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.buf.MessageBytes;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.*;
import org.springframework.web.util.NestedServletException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;

/**
 * 文件转发接口
 * 控制文件转发，接管部分接口
 * @author bin
 */
public class MsIOServlet extends DispatcherServlet {

    //定义辅助信息防止servlet名称导致的方法无法映射的问题
    private final String info = "javax.servlet.include.servlet_path";


    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        changeRequestURI(request);
        //添加一个request
        processedRequest.setAttribute(info,getRequestUri(request));
        HandlerExecutionChain mappedHandler = null;
        boolean multipartRequestParsed = false;
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        try {
            ModelAndView mv = null;
            Exception dispatchException = null;

            try {
                processedRequest = checkMultipart(request);
                multipartRequestParsed = (processedRequest != request);

                // Determine handler for the current request.
                mappedHandler = getHandler(processedRequest);
                if (mappedHandler == null || mappedHandler.getHandler() == null) {
                    noHandlerFound(processedRequest, response);
                    return;
                }

                // Determine handler adapter for the current request.
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                boolean isGet = "GET".equals(method);
                if (isGet || "HEAD".equals(method)) {
                    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
                    }
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }

                if (!applyPreHandle(mappedHandler, processedRequest, response)) {
                    return;
                }

                Object handler = mappedHandler.getHandler();
                // Actually invoke the handler.
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }

                if(mv == null || !mv.hasView()){
                    String defaultViewName = getDefaultViewName(request);
                    if (defaultViewName != null) {
                        mv.setViewName(defaultViewName);
                    }
                }
                applyPostHandle(mappedHandler,processedRequest, response, mv);
            }
            catch (Exception ex) {
                dispatchException = ex;
            }
            catch (Throwable err) {
                // As of 4.3, we're processing Errors thrown from handler methods as well,
                // making them available for @ExceptionHandler methods and other scenarios.
                dispatchException = new NestedServletException("Handler dispatch failed", err);
            }
        }
        catch (Exception ex) {
            //triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        }
        catch (Throwable err) {
            //triggerAfterCompletion(processedRequest, response, mappedHandler,
             //       new NestedServletException("Handler processing failed", err));
        }
        finally {
            if (asyncManager.isConcurrentHandlingStarted()) {
                // Instead of postHandle and afterCompletion
                if (mappedHandler != null) {
                    applyAfterConcurrentHandlingStarted(mappedHandler,processedRequest, response);
                }
            }
            else {
                // Clean up any resources used by a multipart request.
                if (multipartRequestParsed) {
                    cleanupMultipart(processedRequest);
                }
            }
        }
    }

    private void changeRequestURI(ServletRequest request){
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
        }catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取URI
     * @param request
     * @return
     */
    private  String getRequestUri(HttpServletRequest request) {
        String uri = (String)request.getAttribute("javax.servlet.include.request_uri");
        if (uri == null) {
            uri = request.getRequestURI();
        }

        return uri;
    }

    boolean applyPreHandle(HandlerExecutionChain chain,HttpServletRequest request, HttpServletResponse response) throws Exception {
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for(int i = 0; i < interceptors.length; i++) {
                HandlerInterceptor interceptor = interceptors[i];
                if (!interceptor.preHandle(request, response, chain.getHandler())) {
                    afterInterceptorHandle(chain,request,response);
                    return false;
                }
            }
        }

        return true;
    }

    void afterInterceptorHandle(HandlerExecutionChain chain,HttpServletRequest request,HttpServletResponse response){
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        if(!ObjectUtils.isEmpty(interceptors)){
            for (HandlerInterceptor interceptor : interceptors) {
                try {
                    interceptor.afterCompletion(request, response,chain.getHandler(), (Exception)null);
                }catch (Throwable e){
                    logger.error("HandlerInterceptor.afterCompletion threw exception", e);
                }
            }
        }
    }

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
                        logger.error("Interceptor [" + interceptors[i] + "] failed in afterConcurrentHandlingStarted", ex);
                    }
                }
            }
        }
    }
}
