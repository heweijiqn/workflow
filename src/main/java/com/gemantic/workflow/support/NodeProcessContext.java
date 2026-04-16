package com.gemantic.workflow.support;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeProcessContext {
    private String nodeId;
    private int degree;
}
