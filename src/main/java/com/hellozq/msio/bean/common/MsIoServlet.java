package com.hellozq.msio.bean.common;

import com.hellozq.msio.anno.MsReturnTranslator;
import com.hellozq.msio.unit.ExcelFactory;
import com.hellozq.msio.utils.MsELUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.NestedServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 文件转发接口
 * 控制文件转发，接管部分接口
 * @author bin
 */
@SuppressWarnings("unused")
public class MsIoServlet extends DispatcherServlet {

    /**
     * 定义辅助信息防止servlet名称导致的方法无法映射的问题
     */
    private static final String INFO = "javax.servlet.include.servlet_path";

    private static final String HTTP_GET = "get";

    private static final String HTTP_POST = "post";

    private static final String HTTP_PUT = "put";

    private static final String HTTP_DELETE = "delete";

    private static final String HTTP_HEAD = "head";

    private final ServletAssessUtils servletAssessUtils = new ServletAssessUtils();

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        //更改请求url
        servletAssessUtils.changeRequestURI(request);
        //添加一个request
        processedRequest.setAttribute(INFO,servletAssessUtils.getRequestUri(request));
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
                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                // Determine handler adapter for the current request.
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
                boolean isGet = HTTP_GET.equals(method);
                if (isGet || HTTP_HEAD.equals(method)) {
                    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Last-Modified value for [" + servletAssessUtils.getRequestUri(request) + "] is: " + lastModified);
                    }
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }
                //在获取函数体并进行处理前执行的拦截器的开始操作
                if (!servletAssessUtils.applyPreHandle(mappedHandler, processedRequest, response)) {
                    return;
                }

                // Actually invoke the handler.
                // transport to excel
                Method invokeMethod = ((HandlerMethod) mappedHandler.getHandler()).getMethod();
                MsReturnTranslator translator = invokeMethod.getDeclaredAnnotation(MsReturnTranslator.class);
                Object requestResult = servletAssessUtils.getRequestResult(request,response, mappedHandler);

                if(translator != null){
                    requestResult = MsELUtils.getValueByEL(requestResult,translator.value());
                }
                if(requestResult instanceof List){
                    response.setContentType("application/vnd.ms-excel;charset=utf-8");
                    response.setCharacterEncoding("utf-8");
                    response.setHeader("Content-disposition", "attachment;filename=download.xlsx");
                    ExcelFactory.SimpleExcelBeanReverse ins = ExcelFactory.getSimpleExcelBeanReverseInstance((List)requestResult, (e, item) -> item);
                    ins.getWorkbook().write(response.getOutputStream());
                }else {
                    mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
                }

                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }

                if(mv != null && !mv.hasView()){
                    String defaultViewName = getDefaultViewName(request);
                    if (defaultViewName != null) {
                        mv.setViewName(defaultViewName);
                    }
                }
                //在获取函数体并进行处理后执行的拦截器的结束操作
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
            if(dispatchException != null) {
                dispatchException.printStackTrace();
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
