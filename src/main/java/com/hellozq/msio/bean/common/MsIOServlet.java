package com.hellozq.msio.bean.common;

import com.hellozq.msio.bean.common.CommonBean;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.OutputBuffer;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.buf.MessageBytes;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.springframework.web.util.NestedServletException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * 文件转发接口
 * 控制文件转发，接管部分接口
 * @author bin
 */
public class MsIOServlet extends DispatcherServlet {

    /**
     * 定义辅助信息防止servlet名称导致的方法无法映射的问题
     */
    private final String info = "javax.servlet.include.servlet_path";

    private final ServletAssessUtils servletAssessUtils = new ServletAssessUtils();

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        //更改请求url
        servletAssessUtils.changeRequestURI(request);
        //添加一个request
        processedRequest.setAttribute(info,servletAssessUtils.getRequestUri(request));
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
                mappedHandler.getHandler();
                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                // Determine handler adapter for the current request.
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
                boolean isGet = "GET".equals(method);
                if (isGet || "HEAD".equals(method)) {
                    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Last-Modified value for [" + servletAssessUtils.getRequestUri(request) + "] is: " + lastModified);
                    }
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }

                if (!servletAssessUtils.applyPreHandle(mappedHandler, processedRequest, response)) {
                    return;
                }

                Object handler = mappedHandler.getHandler();
                // Actually invoke the handler.
                Object requestResult = servletAssessUtils.getRequestResult(request,response, mappedHandler);
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
                //getResponseBody(response);
                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }

                if(mv != null && !mv.hasView()){
                    String defaultViewName = getDefaultViewName(request);
                    if (defaultViewName != null) {
                        mv.setViewName(defaultViewName);
                    }
                }
                servletAssessUtils.applyPostHandle(mappedHandler,processedRequest, response, mv);
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
                    servletAssessUtils.applyAfterConcurrentHandlingStarted(mappedHandler,processedRequest, response);
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

    void downloadFileProcess(){

    }
}
