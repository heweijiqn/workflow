package com.gemantic.workflow.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.ConsumerRepository;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ConsumerRepositoryImpl implements ConsumerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerRepositoryImpl.class);


    private  Map<String, BaseConsumer> consumerMap = Maps.newHashMap();


    @Override
    public BaseConsumer getConsumer(String nodeType) {
        if (StringUtils.isBlank(nodeType)) {
            return null;
        }
        String className = StrUtil.toCamelCase(nodeType);
        Map<String, BaseConsumer> consumerMap = getConsumerMap();
        return consumerMap.get(className);
    }

    public Map<String, BaseConsumer> getConsumerMap() {
        if (MapUtils.isEmpty(consumerMap)) {
            this.consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
            LOG.warn("初始化consumerMap={}", consumerMap.keySet());
        }
        return consumerMap;
    }
}
