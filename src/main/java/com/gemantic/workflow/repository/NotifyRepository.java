package com.gemantic.workflow.repository;

import com.gemantic.gpt.model.NotifyLog;
import com.gemantic.springcloud.model.Response;

import java.util.List;

public interface NotifyRepository {

    Response<List<NotifyLog>> push(List<NotifyLog> data) throws Exception;
}
