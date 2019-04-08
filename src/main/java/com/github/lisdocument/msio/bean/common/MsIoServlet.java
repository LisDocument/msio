package com.github.lisdocument.msio.bean.common;

import com.github.lisdocument.msio.anno.MsTranslateOperator;
import com.github.lisdocument.msio.bean.db.DownloadReword;
import com.github.lisdocument.msio.config.AbstractStoreRecordConfigure;
import com.github.lisdocument.msio.config.StoreRecordConfiguration;
import com.github.lisdocument.msio.unit.excel.ExcelFactory;
import com.github.lisdocument.msio.unit.excel.IExcelBeanReverse;
import com.github.lisdocument.msio.anno.MsReturnTranslator;
import com.github.lisdocument.msio.utils.MsELUtils;
import com.github.lisdocument.msio.utils.MsUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * 文件转发接口
 * 控制文件转发，接管部分接口
 * @author bin
 */
@SuppressWarnings("unused")
public class MsIoServlet extends DispatcherServlet {

    @Autowired
    private AbstractStoreRecordConfigure storeRecordConfiguration;
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
        //servletAssessUtils.changeRequestURI(request,response);
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
                //用户访问原路径拦截请求成功，开始解析更换后的新的请求
                //封装一个新的请求,更改路径
                HttpServletRequestWrapper httpServletRequestWrapper = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        String path = super.getRequestURI();
                        path = path.substring(1);
                        int i = path.indexOf("/");
                        path = path.substring(i);
                        return path;
                    }
                };

                HandlerExecutionChain handler = getHandler(httpServletRequestWrapper);
                if (handler == null || handler.getHandler() == null) {
                    noHandlerFound(httpServletRequestWrapper, response);
                    return;
                }
                //拦截器
                if (!servletAssessUtils.applyPreHandle(handler, httpServletRequestWrapper, response)) {
                    return;
                }

                // Actually invoke the handler.
                // transport to excel
                //------------------------------//
                Method invokeMethod = ((HandlerMethod) handler.getHandler()).getMethod();
                MsReturnTranslator translator = invokeMethod.getDeclaredAnnotation(MsReturnTranslator.class);
                Object requestResult = servletAssessUtils.getRequestResult(request,response, handler);

                //完成后直接调用拦截器的后续操作
                servletAssessUtils.applyPostHandle(handler,httpServletRequestWrapper, response, mv);
                MsTranslateOperator msTranslateOperator = requestResult.getClass().getDeclaredAnnotation(MsTranslateOperator.class);
                //转义List操作
                String fileName = "download.xlsx";
                ExcelFactory.ExcelDealType type = ExcelFactory.ExcelDealType.XLSX;
                if(translator != null){
                    requestResult = MsELUtils.getValueByEL(requestResult,translator.value());
                    fileName = translator.fileName() + translator.type().getValue();
                    type = translator.type();
                }else if(msTranslateOperator != null){
                    requestResult = MsELUtils.getValueByEL(requestResult,msTranslateOperator.value());
                }
                if(requestResult instanceof List){
                    logger.info("Download task is beginning");
                    //創建對象，生成記錄項
                    DownloadReword downloadReword = new DownloadReword();
                    downloadReword.setId(UUID.randomUUID().toString())
                            .setIp(request.getRemoteAddr())
                            .setMethod(method)
                            .setUsername(request.getParameter("username"))
                            .setUrl(request.getRequestURI())
                            .setParams(CommonBean.OBJECT_MAPPER.writeValueAsString(request.getParameterMap()));

                    long last = System.currentTimeMillis();

                    if(translator == null || !translator.isComplex()) {
                        IExcelBeanReverse ins = ExcelFactory.getSimpleExcelBeanReverseInstance((List) requestResult
                                , type,null == translator ? new String[]{""} : translator.title(), (e, item) -> item);
                        ins.write(response, fileName);
                    }else if(translator.isComplex()){
                        IExcelBeanReverse ins = ExcelFactory.getComplexExcelBeanReverseInstance(translator.id()[0]
                                ,(List)requestResult,type,translator.title(),(e, item) -> item);
                        ins.write(response, fileName);
                    }
                    downloadReword.setTime(System.currentTimeMillis());
                    downloadReword.setCostTime((int)(downloadReword.getTime() - last));
                    //發送數據進行发送，
                    if(translator.logSign()) {
                        storeRecordConfiguration.send(downloadReword);
                    }


                    logger.info("Download task completed in "+ (System.currentTimeMillis() - last)+" ms");
                }else {
                    mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
                }
                //------------------------------//
                //transport end//

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

}
