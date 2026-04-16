package com.gemantic.workflow.repository;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * Tiptap 在线文档处理器接口
 * 用于处理新的 Tiptap 文档类型 (type="doc")
 */
public interface TiptapDocumentProcessor {

    /**
     * 处理 Tiptap 文档类型
     * 
     * @param parameters 包含文档内容和上游节点输出的参数对象
     * @return 处理后的文档内容列表
     */
    List<JSONObject> process(JSONObject parameters);
}

