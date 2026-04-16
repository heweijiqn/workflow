package com.gemantic.workflow.support;

import com.gemantic.gpt.support.workflow.Edge;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class EdgeIndex {

    /** source节点ID -> 以该节点为起点的边列表 */
    private final Map<String, List<Edge>> sourceIndex;

    /** target节点ID -> 以该节点为终点的边列表 */
    private final Map<String, List<Edge>> targetIndex;

    /** 原始边列表 */
    private final List<Edge> allEdges;

    /**
     * 从边列表构建索引
     * 时间复杂度: O(E)
     */
    public EdgeIndex(List<Edge> edges) {
        this.allEdges = edges != null ? edges : Collections.emptyList();

        this.sourceIndex = this.allEdges.stream()
                .filter(e -> StringUtils.isNotBlank(e.getSource()))
                .collect(Collectors.groupingBy(Edge::getSource));

        this.targetIndex = this.allEdges.stream()
                .filter(e -> StringUtils.isNotBlank(e.getTarget()))
                .collect(Collectors.groupingBy(Edge::getTarget));
    }

    /**
     * 获取从指定节点出发的边（O(1)）
     */
    public List<Edge> getEdgesBySource(String source) {
        return sourceIndex.getOrDefault(source, Collections.emptyList());
    }

    /**
     * 获取指向指定节点的边（O(1)）
     */
    public List<Edge> getEdgesByTarget(String target) {
        return targetIndex.getOrDefault(target, Collections.emptyList());
    }

    /**
     * 获取与指定节点相关的所有边（O(1)）
     */
    public List<Edge> getEdgesByNode(String nodeId) {
        List<Edge> result = new ArrayList<>();
        result.addAll(getEdgesBySource(nodeId));
        result.addAll(getEdgesByTarget(nodeId));
        return result;
    }

    /**
     * 获取指定节点的所有下游节点ID（O(1) + O(出度)）
     */
    public List<String> getTargetNodeIds(String source) {
        return getEdgesBySource(source).stream()
                .map(Edge::getTarget)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取指定节点的所有上游节点ID（O(1) + O(入度)）
     */
    public List<String> getSourceNodeIds(String target) {
        return getEdgesByTarget(target).stream()
                .map(Edge::getSource)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断边是否为空
     */
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(allEdges);
    }
}
