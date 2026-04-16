package com.gemantic.config;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;


/**
 * 容器配置
 *
 * @author yhye 2016年9月20日下午4:55:17
 */
@Configuration
public class HttpConfig {


    public @Bean
    FastJsonHttpMessageConverter fastJsonpHttpMessageConverter() {

        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setWriterFeatures(JSONWriter.Feature.WriteEnumsUsingName,
                JSONWriter.Feature.FieldBased, JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.LargeObject, JSONWriter.Feature.WriteBigDecimalAsPlain);
        converter.setFastJsonConfig(fastJsonConfig);
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
        converter.setSupportedMediaTypes(supportedMediaTypes);

        return converter;
    }


}
