package com.gemantic.workflow.repository;

import com.gemantic.workflow.consumer.BaseConsumer;

import java.util.Map;

public interface ConsumerRepository {

    BaseConsumer getConsumer(String nodeType);

    Map<String, BaseConsumer> getConsumerMap();
}
