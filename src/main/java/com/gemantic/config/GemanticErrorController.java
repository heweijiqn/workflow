package com.gemantic.config;

import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.model.ResponseMessage;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

/**
 * 404统一返回处理
 *
 * @author yhye 2016年11月25日下午6:02:46
 */
@Hidden
@RestController
public class GemanticErrorController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(GemanticErrorController.class);
    private static final String ERROR_PATH = "/error";
    //    private static final int ERROR_NOT_FOUND = -4041;
    public static final String FAVICON_PATH = "/favicon.ico";

    public static final DefaultErrorAttributes DEFAULT_ERROR_ATTRIBUTES = new DefaultErrorAttributes();

    public static final ErrorAttributeOptions DEFAULT_ERROR_ATTRIBUTES_OPTIONS = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION,ErrorAttributeOptions.Include.ERROR,ErrorAttributeOptions.Include.BINDING_ERRORS,ErrorAttributeOptions.Include.STATUS, ErrorAttributeOptions.Include.MESSAGE);

    @RequestMapping(value = ERROR_PATH)
    public Response<Void> error(HttpServletRequest request, HttpServletResponse response) {
        String request_uri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String query_string = (String) request.getAttribute("jakarta.servlet.forward.query_string");
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> errorMessage = DEFAULT_ERROR_ATTRIBUTES.getErrorAttributes(webRequest, DEFAULT_ERROR_ATTRIBUTES_OPTIONS);
        Integer status = MapUtils.getInteger(errorMessage, "status");
        String error = MapUtils.getString(errorMessage, "error", StringUtils.EMPTY);
        String exception = MapUtils.getString(errorMessage,"exception",StringUtils.EMPTY);
        String message = MapUtils.getString(errorMessage, "message", StringUtils.EMPTY);
//        String trace = MapUtils.getString(errorMesage, "trace", StringUtils.EMPTY);
        List<String> messages = Lists.newArrayList();
        messages.add(request.getMethod());
        if (StringUtils.isNotBlank(query_string)) {
            messages.add(String.join("?", request_uri, query_string));
        } else {
            messages.add(request_uri);
        }
        if(StringUtils.isNotBlank(error)) {
            messages.add(error);
        }
        if(StringUtils.isNotBlank(exception) && !exception.equalsIgnoreCase(error)){
            messages.add(exception);
        }
        if (!message.equalsIgnoreCase(error) && !message.equalsIgnoreCase(exception)) {
            messages.add(message);
        }
        String urlMessage = String.join(StringUtils.SPACE, messages);
        Response<Void> errorResponse = Response.error();
        ResponseMessage messageResponse = ResponseMessage.error(status, -1 * status, urlMessage);
        errorResponse.setMessage(messageResponse);
        if (!request_uri.equalsIgnoreCase(FAVICON_PATH) && status != 404) {
            LOG.error("status={} {}", messageResponse.getStatus(), urlMessage);
        }
        return errorResponse;
    }


}
