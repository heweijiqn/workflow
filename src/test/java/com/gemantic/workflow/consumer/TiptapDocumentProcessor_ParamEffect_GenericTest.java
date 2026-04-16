package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.repository.impl.TiptapDocumentProcessorImpl;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// 替换为你的实际包路径

@ExtendWith(MockitoExtension.class)
@Ignore
class TiptapDocumentProcessor_ParamEffect_GenericTest {

    // 被测试对象
    @InjectMocks
    private TiptapDocumentProcessorImpl tiptapDocumentProcessor;
    @InjectMocks
    private DocOutput docOutput;

    /**
     * 通用参数效果测试：输入自定义参数，验证处理结果符合预期
     */
    @Test
    void process_CustomParams_VerifyEffect() {
        String json= """
                {
                	"content": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": {
                			"showLock": false,
                			"data": {
                				"type": "doc",
                				"content": [{
                					"type": "paragraph",
                					"attrs": {
                						"id": "0aa2b1ff-c55e-4ad5-9306-572e7fd64f8c"
                					}
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "7d07285b-6210-45b4-8588-c78c3fbc1770"
                					}
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "fdf310b9-b9ce-43ad-a29b-a58e34d09fa1"
                					},
                					"content": [{
                						"type": "text",
                						"marks": [{
                							"type": "textStyle",
                							"attrs": {
                								"color": "",
                								"fontFamily": "宋体",
                								"fontSize": "10.5",
                								"backgroundColor": "",
                								"lineHeight": "1"
                							}
                						}],
                						"text": "                 "
                					}]
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "cd907e26-827b-4cd0-8daa-800a608985ea"
                					}
                				}, {
                					"type": "conditionTemplate",
                					"attrs": {
                						"id": "2678776d-008a-4302-adaf-2ccfbc55a8e5",
                						"backgroundColor": "rgb(249, 240, 255)"
                					},
                					"content": [{
                						"type": "paragraph",
                						"attrs": {
                							"id": "affec98f-ce17-42b2-b116-fbf0daa64089"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "<#if \\""
                						}, {
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "bc71a024-06b7-4511-b920-73d31835d3c4",
                								"isTemplate": "text",
                								"backgroundColor": "rgb(230, 247, 255)",
                								"displayMode": "inline"
                							},
                							"content": [{
                								"type": "text",
                								"marks": [{
                									"type": "textStyle",
                									"attrs": {
                										"fontFamily": "宋体",
                										"fontSize": "10.5",
                										"lineHeight": "1"
                									}
                								}],
                								"text": "{{开始输入#检测机构}}"
                							}],
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "",
                									"backgroundColor": "rgb(230, 247, 255)",
                									"lineHeight": ""
                								}
                							}]
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "\\" == \\""
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "天\\" || "
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "0924cc7b-9abb-49d3-b1db-8ecfc683d3a8"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "\\""
                						}, {
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "4be89d5b-9fe7-4206-8ca5-bc04d95bdd81",
                								"isTemplate": "text",
                								"backgroundColor": "rgb(230, 247, 255)",
                								"displayMode": "inline"
                							},
                							"content": [{
                								"type": "text",
                								"marks": [{
                									"type": "textStyle",
                									"attrs": {
                										"fontFamily": "宋体",
                										"fontSize": "10.5",
                										"lineHeight": "1"
                									}
                								}],
                								"text": "{{ETL#原始数据#$1#企业名称}}"
                							}],
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "",
                									"backgroundColor": "rgb(230, 247, 255)",
                									"lineHeight": ""
                								}
                							}]
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "\\"?contains(\\""
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "青岛"
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "\\") >"
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "1fcb57f1-dcd6-479b-9399-bea69851c0c2"
                						},
                						"content": [{
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "c9d42ce1-bfa2-4784-a2b4-9efcf73ef607",
                								"isTemplate": "text",
                								"backgroundColor": "#e6f7ff",
                								"displayMode": "inline"
                							},
                							"content": [{
                								"type": "text",
                								"text": "{{2345678#输出}}"
                							}]
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "99e79f99-bdf4-4646-8780-af19c9733a9b"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "123131"
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "cab34fc7-8e44-4232-883a-b30aa9396583"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "111"
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "c9e8fcc2-8f12-4564-b0bb-03b0d2bf0ecd"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "1111"
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "7d5ac52c-729e-4f21-ad22-35b44fee3b9b"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "111"
                						}]
                					}, {
                						"type": "table",
                						"attrs": {
                							"id": "936a5fa2-1ca0-42a9-8b8b-c330d7475713",
                							"width": 517,
                							"columnWidths": [172, 172, 172]
                						},
                						"content": [{
                							"type": "tableRow",
                							"attrs": {
                								"id": "e49c311c-0b6b-40b7-9365-4c3fa0128433"
                							},
                							"content": [{
                								"type": "tableHeader",
                								"attrs": {
                									"id": "3e21f30b-867a-4cbf-9f17-952882054daf",
                									"colspan": 1,
                									"rowspan": 1
                								},
                								"content": [{
                									"type": "paragraph",
                									"attrs": {
                										"id": "669e3bbb-30c9-450a-b578-5075e50bf29c"
                									},
                									"content": [{
                										"type": "text",
                										"marks": [{
                											"type": "textStyle",
                											"attrs": {
                												"fontFamily": "宋体",
                												"fontSize": "10.5",
                												"lineHeight": "1"
                											}
                										}],
                										"text": "项目"
                									}]
                								}]
                							}, {
                								"type": "tableHeader",
                								"attrs": {
                									"id": "1e9299d3-5309-484a-acc6-25922a5733de",
                									"colspan": 1,
                									"rowspan": 1
                								},
                								"content": [{
                									"type": "paragraph",
                									"attrs": {
                										"id": "f4f819b0-0fde-4560-80e9-faff42fe1029"
                									},
                									"content": [{
                										"type": "text",
                										"marks": [{
                											"type": "textStyle",
                											"attrs": {
                												"fontFamily": "宋体",
                												"fontSize": "10.5",
                												"lineHeight": "1"
                											}
                										}],
                										"text": "报告"
                									}]
                								}]
                							}, {
                								"type": "tableHeader",
                								"attrs": {
                									"id": "2171f28f-82a2-4ecd-bcec-b73a71286dc2",
                									"colspan": 1,
                									"rowspan": 1
                								},
                								"content": [{
                									"type": "paragraph",
                									"attrs": {
                										"id": "aa43ffc7-5ac0-4ed2-a8a6-a8a42d1c2f00"
                									},
                									"content": [{
                										"type": "text",
                										"marks": [{
                											"type": "textStyle",
                											"attrs": {
                												"fontFamily": "宋体",
                												"fontSize": "10.5",
                												"lineHeight": "1"
                											}
                										}],
                										"text": "图表"
                									}]
                								}]
                							}]
                						}, {
                							"type": "tableRow",
                							"attrs": {
                								"id": "f751f5d9-9438-41c2-81ff-76de08841c23",
                								"loop": "true",
                								"startLoopIndex": 0,
                								"endLoopIndex": 1
                							},
                							"content": [{
                								"type": "tableCell",
                								"attrs": {
                									"id": "519c767e-e7eb-4c38-b2cf-7e4552d7ede7",
                									"colspan": 1,
                									"rowspan": 1,
                									"inLoopRange": "true"
                								},
                								"content": [{
                									"type": "paragraph",
                									"attrs": {
                										"id": "69615efd-f939-427d-95bc-ccbb801613d7"
                									},
                									"content": [{
                										"type": "inlineTemplate",
                										"attrs": {
                											"id": "8caf7ccb-ecca-4a0f-871e-4c5a61f9a185",
                											"isTemplate": "text",
                											"backgroundColor": "#e6f7ff",
                											"displayMode": "inline"
                										},
                										"content": [{
                											"type": "text",
                											"text": "{{ETL2#原始数据#$1#报告期}}"
                										}]
                									}]
                								}]
                							}, {
                								"type": "tableCell",
                								"attrs": {
                									"id": "2aff35a0-507e-4ecd-a229-3bf24f1ea03a",
                									"colspan": 1,
                									"rowspan": 1,
                									"inLoopRange": "true"
                								},
                								"content": [{
                									"type": "paragraph",
                									"attrs": {
                										"id": "44ab4a92-2c73-4f21-8a44-ccfe111c3236"
                									},
                									"content": [{
                										"type": "inlineTemplate",
                										"attrs": {
                											"id": "046d127a-5a08-4c49-bedf-25db5092892d",
                											"isTemplate": "text",
                											"backgroundColor": "#e6f7ff",
                											"displayMode": "inline"
                										},
                										"content": [{
                											"type": "text",
                											"text": "{{ETL2#原始数据#$1#报告期}}"
                										}]
                									}]
                								}]
                							}, {
                								"type": "tableCell",
                								"attrs": {
                									"id": "74ccdba9-9258-40af-a40e-c8b72447a4af",
                									"colspan": 1,
                									"rowspan": 1
                								},
                								"content": [{
                									"type": "image",
                									"attrs": {
                										"id": "4c4162b3-fa28-41ab-802d-856d69b866d2",
                										"src": "group1/M00/AD/F7/CgAAcGktON2AMjGQAAAVMcsyfls308.png",
                										"alt": "chart-1764571357614.png",
                										"title": "chart-1764571357614.png",
                										"width": 320,
                										"height": 180,
                										"chartValue": "{{图表#输出}}"
                									}
                								}]
                							}]
                						}]
                					}, {
                						"type": "image",
                						"attrs": {
                							"id": "eab6a5e8-368c-4c1d-acf1-f94980d79ae8",
                							"src": "group1/M00/AD/F7/CgAAcGktOLiAL3qtAAAVMcsyfls975.png",
                							"alt": "chart-1764571320265.png",
                							"title": "chart-1764571320265.png",
                							"width": 320,
                							"height": 180,
                							"chartValue": "{{图表#输出}}"
                						}
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "f3d392f4-d9ef-42e5-ba2e-ee5807a442ed"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": " 支持引用开始输入 、ETL、SQL数据,进行文本比较,包含 ==、?contains、?? <#else if "
                						}, {
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "4aab59ab-d80a-4cae-9f18-8b7b1416a821",
                								"isTemplate": "text",
                								"backgroundColor": "rgb(230, 247, 255)",
                								"displayMode": "inline"
                							},
                							"content": [{
                								"type": "text",
                								"marks": [{
                									"type": "textStyle",
                									"attrs": {
                										"fontFamily": "宋体",
                										"fontSize": "10.5",
                										"lineHeight": "1"
                									}
                								}],
                								"text": "{{ETL#原始数据#$1#持续经营年限}}"
                							}],
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "",
                									"backgroundColor": "rgb(230, 247, 255)",
                									"lineHeight": ""
                								}
                							}]
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": " == \\"25\\" || "
                						}, {
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "bfb3e4ad-5ae2-4adc-a598-be3ba1f76360",
                								"isTemplate": "text",
                								"backgroundColor": "rgb(230, 247, 255)",
                								"displayMode": "inline"
                							},
                							"content": [{
                								"type": "text",
                								"marks": [{
                									"type": "textStyle",
                									"attrs": {
                										"fontFamily": "宋体",
                										"fontSize": "10.5",
                										"lineHeight": "1"
                									}
                								}],
                								"text": "{{ETL#原始数据#$1#企业规模}}"
                							}],
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "",
                									"backgroundColor": "rgb(230, 247, 255)",
                									"lineHeight": ""
                								}
                							}]
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": " ?contains(\\"6000\\")> "
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "3955ce2a-33df-4a3e-8120-72de79733b50"
                						}
                					}, {
                						"type": "image",
                						"attrs": {
                							"id": "0a917e8f-93fd-4f8d-8b7d-078d0d3beb34",
                							"src": "group1/M00/AE/03/CgAAcGkuXiuAH8KTAAAVMcsyfls381.png",
                							"alt": "chart-1764646443087.png",
                							"title": "chart-1764646443087.png",
                							"width": 320,
                							"height": 180,
                							"chartValue": "{{图表#输出}}"
                						}
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "a364bd8d-fbae-4aaa-b6df-36ba6930d84e"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"color": "",
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"backgroundColor": "",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "也支持数字比较,包括:==、gt、gte、lt、lte <#else if (true || !false ) && false> 支持布尔操作 <#else> 也支持else "
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "</#if>"
                						}]
                					}]
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "354f363b-2906-40c7-a142-4231d9e3a95b"
                					},
                					"content": [{
                						"type": "hardBreak",
                						"attrs": {
                							"id": "931f2a47-90f2-4e70-9d29-68d9b0e27036"
                						}
                					}, {
                						"type": "hardBreak",
                						"attrs": {
                							"id": "aad2cf91-3769-433c-b78a-491cd0459471"
                						}
                					}]
                				}]
                			},
                			"pageMarginState": {
                				"top": 2.54,
                				"bottom": 2.54,
                				"left": 3.17,
                				"right": 3.17
                			},
                			"dpi": 96,
                			"paperSizeState": {
                				"width": 794,
                				"height": 1123,
                				"name": "A4"
                			},
                			"conditionArr": []
                		},
                		"password": false,
                		"name": "content",
                		"display_name": "输出内容",
                		"type": "dict",
                		"clear_after_run": true,
                		"list": false,
                		"field_type": "doc_editor",
                		"hide_show": true,
                		"hide_copy": true,
                		"display_index": 1000,
                		"conditionArr": []
                	},
                	"ETL#原始数据": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": true,
                		"value": [{
                			"企业名称": "青岛xx公司",
                			"法定代表人名称": "张三",
                			"成立时间": "2020年",
                			"持续经营年限": "25",
                			"注册地址": "山东青岛",
                			"经营地址": "香港中路",
                			"注册资本": "8000万",
                			"企业规模": "6000人",
                			"所属行业": "软件",
                			"经营范围": "软件服务,软件开发"
                		}],
                		"options": [],
                		"password": false,
                		"name": "ETL#原始数据",
                		"display_name": "ETL#原始数据",
                		"type": "dict",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 101,
                		"node_id": "eaf7913c-f1c0-4de0-9733-97cbb70a7594",
                		"node_type": "sql_etl",
                		"is_output": true,
                		"source_node_id": "eaf7913c-f1c0-4de0-9733-97cbb70a7594",
                		"source_node_type": "sql_etl",
                		"source_node_handle": "output_standard_chart",
                		"source_node_name": "ETL",
                		"source_workflow_run_result_id": "2258667"
                	},
                	"ETL2#原始数据": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": true,
                		"value": [{
                			"报告期": "张三111111111111111111111111",
                			"项目": "18122132234132536478"
                		}, {
                			"报告期": "李四22222222222222222222",
                			"项目": "31324354635243123452"
                		}, {
                			"报告期": "王武22222222222222222222222222",
                			"项目": "21234312123454671"
                		}],
                		"options": [],
                		"password": false,
                		"name": "ETL2#原始数据",
                		"display_name": "ETL2#原始数据",
                		"type": "dict",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 102,
                		"node_id": "7f3b97bb-8348-46d7-82fb-3555ed6d7e8f",
                		"node_type": "sql_etl",
                		"is_output": true,
                		"source_node_id": "7f3b97bb-8348-46d7-82fb-3555ed6d7e8f",
                		"source_node_type": "sql_etl",
                		"source_node_handle": "output_standard_chart",
                		"source_node_name": "ETL2",
                		"source_workflow_run_result_id": "2258669"
                	},
                	"图表#输出": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": {
                			"type": "chart_bar",
                			"title": "",
                			"xAxisTitle": "",
                			"yAxisTitle": "",
                			"colorMatch": "default",
                			"colors": ["#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de"],
                			"showTitle": true,
                			"showGrid": true,
                			"showLegend": true,
                			"showAxisLabel": true,
                			"showAxis": true,
                			"showDataLabel": false,
                			"legend": ["法定代表人名称", "成立时间", "持续经营年限", "注册地址", "经营地址", "注册资本", "企业规模", "所属行业", "经营范围"],
                			"valueList": [{
                				"name": "青岛xx公司",
                				"value": [null, null, 25.0, null, null, null, null, null, null]
                			}]
                		},
                		"options": [],
                		"password": false,
                		"name": "图表#输出",
                		"display_name": "图表#输出",
                		"type": "dict",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 103,
                		"node_id": "1555fe3f-7a60-4178-96e0-ec9c00fc3235",
                		"node_type": "chart_setup",
                		"is_output": true,
                		"source_node_id": "1555fe3f-7a60-4178-96e0-ec9c00fc3235",
                		"source_node_type": "chart_setup",
                		"source_node_handle": "output",
                		"source_node_name": "图表",
                		"source_workflow_run_result_id": "2258668"
                	},
                	"2345678#输出": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": {
                			"content": {
                				"required": true,
                				"placeholder": "",
                				"show": true,
                				"multiline": false,
                				"value": {
                					"showLock": false,
                					"data": {
                						"type": "doc",
                						"content": [{
                							"type": "paragraph",
                							"attrs": {
                								"id": "88062979-3d7e-4587-ac1a-8d253bd884ac"
                							},
                							"content": [{
                								"type": "text",
                								"marks": [{
                									"type": "textStyle",
                									"attrs": {
                										"fontFamily": "宋体",
                										"fontSize": "10.5",
                										"lineHeight": "1"
                									}
                								}],
                								"text": "1234567890"
                							}]
                						}]
                					},
                					"pageMarginState": {
                						"top": 2.54,
                						"bottom": 2.54,
                						"left": 3.17,
                						"right": 3.17
                					},
                					"dpi": 96,
                					"paperSizeState": {
                						"width": 794,
                						"height": 1123,
                						"name": "A4"
                					},
                					"conditionArr": []
                				},
                				"password": false,
                				"name": "content",
                				"display_name": "输出内容",
                				"type": "dict",
                				"clear_after_run": true,
                				"list": false,
                				"field_type": "doc_editor",
                				"hide_show": true,
                				"hide_copy": true,
                				"display_index": 1000,
                				"conditionArr": []
                			},
                			"docOutput": {
                				"showLock": false,
                				"data": {
                					"type": "doc",
                					"content": [{
                						"type": "paragraph",
                						"attrs": {
                							"id": "88062979-3d7e-4587-ac1a-8d253bd884ac"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "1234567890"
                						}]
                					}]
                				},
                				"pageMarginState": {
                					"top": 2.54,
                					"bottom": 2.54,
                					"left": 3.17,
                					"right": 3.17
                				},
                				"dpi": 96,
                				"paperSizeState": {
                					"width": 794,
                					"height": 1123,
                					"name": "A4"
                				},
                				"conditionArr": []
                			},
                			"fileUrl": "http://10.0.0.22:10105/workflow-editor?workflowId=2265&workflowRunId=719275"
                		},
                		"options": [],
                		"password": false,
                		"name": "2345678#输出",
                		"display_name": "2345678#输出",
                		"type": "doc_output",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 104,
                		"node_id": "1b36d5da-b647-4442-9821-52d0fa3ad64d",
                		"node_type": "sub_workflow",
                		"is_output": true,
                		"source_node_id": "1b36d5da-b647-4442-9821-52d0fa3ad64d",
                		"source_node_type": "sub_workflow",
                		"source_node_handle": "output",
                		"source_node_name": "2345678",
                		"source_workflow_run_result_id": "2258665"
                	},
                	"检测机构": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": "天",
                		"options": [],
                		"password": false,
                		"name": "检测机构",
                		"display_name": "检测机构",
                		"type": "str",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 101,
                		"is_start": true
                	}
                }
                """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process_CustomTable_VerifyEffect() {
        String json= """
                {
                	"content": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": {
                			"showLock": false,
                			"data": {
                				"type": "doc",
                				"content": [{
                					"type": "paragraph",
                					"attrs": {
                						"id": "78935a2c-ab2e-4052-b3fc-9d8f8b2d8b37"
                					}
                				}, {
                					"type": "table",
                					"attrs": {
                						"id": "ae913c94-56fe-472a-937b-0c9e3f49d122",
                						"width": 554,
                						"columnWidths": [184, 184, 184]
                					},
                					"content": [{
                						"type": "tableRow",
                						"attrs": {
                							"id": "6ced45a5-1d34-431f-914f-8cff61bcd6d3"
                						},
                						"content": [{
                							"type": "tableHeader",
                							"attrs": {
                								"id": "d22143f7-34d6-40fe-97f4-37349aac9ffa",
                								"colspan": 1,
                								"rowspan": 1
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "6334465f-d5b9-4402-ae29-225f6e617a0c"
                								},
                								"content": [{
                									"type": "text",
                									"marks": [{
                										"type": "textStyle",
                										"attrs": {
                											"fontFamily": "宋体",
                											"fontSize": "10.5",
                											"lineHeight": "1"
                										}
                									}],
                									"text": "项目"
                								}]
                							}]
                						}, {
                							"type": "tableHeader",
                							"attrs": {
                								"id": "ce082896-3400-4a3b-a4d5-563fceffbe68",
                								"colspan": 1,
                								"rowspan": 1
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "674b3f44-a2d0-4344-9ba4-dc5d6c704510"
                								},
                								"content": [{
                									"type": "text",
                									"marks": [{
                										"type": "textStyle",
                										"attrs": {
                											"fontFamily": "宋体",
                											"fontSize": "10.5",
                											"lineHeight": "1"
                										}
                									}],
                									"text": "报告期"
                								}]
                							}]
                						}, {
                							"type": "tableHeader",
                							"attrs": {
                								"id": "721da20f-2c97-4f3d-bbb7-9ebe6d4f950a",
                								"colspan": 1,
                								"rowspan": 1
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "0adfc0cb-9934-4923-becd-1e90ec0fe04f"
                								},
                								"content": [{
                									"type": "text",
                									"marks": [{
                										"type": "textStyle",
                										"attrs": {
                											"fontFamily": "宋体",
                											"fontSize": "10.5",
                											"lineHeight": "1"
                										}
                									}],
                									"text": "合并"
                								}]
                							}]
                						}]
                					}, {
                						"type": "tableRow",
                						"attrs": {
                							"id": "86d5713f-2a21-446b-93d3-50ab34ad744f",
                							"loop": "true",
                							"startLoopIndex": 0,
                							"endLoopIndex": 1
                						},
                						"content": [{
                							"type": "tableCell",
                							"attrs": {
                								"id": "e7d19585-6741-42bb-950a-db5b5bc5395b",
                								"colspan": 1,
                								"rowspan": 1,
                								"inLoopRange": "true"
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "80e54e57-4728-4c32-aebd-a61a3a0f0c40"
                								},
                								"content": [{
                									"type": "inlineTemplate",
                									"attrs": {
                										"id": "2d76570b-1e2d-4b5b-b77e-420ee1014fe0",
                										"isTemplate": "text",
                										"backgroundColor": "#e6f7ff",
                										"displayMode": "inline"
                									},
                									"content": [{
                										"type": "text",
                										"text": "{{ETL#原始数据#$1#项目}}"
                									}]
                								}]
                							}]
                						}, {
                							"type": "tableCell",
                							"attrs": {
                								"id": "e0978f5e-6d17-4cc3-a41a-395a1ee94c0f",
                								"colspan": 1,
                								"rowspan": 1,
                								"inLoopRange": "true"
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "59b5884f-3029-4f56-80f5-cf47d7f21753"
                								},
                								"content": [{
                									"type": "inlineTemplate",
                									"attrs": {
                										"id": "248dfb36-4707-4303-8990-c6cf133e303b",
                										"isTemplate": "text",
                										"backgroundColor": "#e6f7ff",
                										"displayMode": "inline"
                									},
                									"content": [{
                										"type": "text",
                										"text": "{{ETL#原始数据#$1#报告期}}"
                									}]
                								}]
                							}]
                						}, {
                							"type": "tableCell",
                							"attrs": {
                								"id": "096cfbff-927d-42ce-a168-d188c628353d",
                								"colspan": 1,
                								"rowspan": 2
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "686a38bc-0a74-4409-bd10-14de5c85f06f"
                								},
                								"content": [{
                									"type": "text",
                									"marks": [{
                										"type": "textStyle",
                										"attrs": {
                											"fontFamily": "宋体",
                											"fontSize": "10.5",
                											"lineHeight": "1"
                										}
                									}],
                									"text": "3"
                								}]
                							}]
                						}]
                					}, {
                						"type": "tableRow",
                						"attrs": {
                							"id": "6fa01cfe-5c1f-4edc-a70e-5a359b7841b6",
                							"loop": "true",
                							"startLoopIndex": 0,
                							"endLoopIndex": 1
                						},
                						"content": [{
                							"type": "tableCell",
                							"attrs": {
                								"id": "f40fd94a-c423-4b39-a602-8ed3f696f0a2",
                								"colspan": 1,
                								"rowspan": 1,
                								"inLoopRange": "true"
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "85de3a82-ccf1-43b9-b4fa-b50b9db3ee0b"
                								},
                								"content": [{
                									"type": "inlineTemplate",
                									"attrs": {
                										"id": "92d3d523-548c-42e8-8cd7-57b2df30b77f",
                										"isTemplate": "text",
                										"backgroundColor": "#e6f7ff",
                										"displayMode": "inline"
                									},
                									"content": [{
                										"type": "text",
                										"text": "{{ETL#原始数据#$1#报告期}}"
                									}]
                								}]
                							}]
                						}, {
                							"type": "tableCell",
                							"attrs": {
                								"id": "0d05c673-450e-4964-841d-e3e33e58cc38",
                								"colspan": 1,
                								"rowspan": 1,
                								"inLoopRange": "true"
                							},
                							"content": [{
                								"type": "paragraph",
                								"attrs": {
                									"id": "e6369947-af10-4e71-bd96-7078978aae1c"
                								},
                								"content": [{
                									"type": "inlineTemplate",
                									"attrs": {
                										"id": "a3eebc27-5fda-4652-ad5d-82b0aa06b621",
                										"isTemplate": "text",
                										"backgroundColor": "#e6f7ff",
                										"displayMode": "inline"
                									},
                									"content": [{
                										"type": "text",
                										"text": "{{ETL#原始数据#$1#项目}}"
                									}]
                								}]
                							}]
                						}]
                					}]
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "d1b04236-5b31-4435-9aef-c7cb862e3ab5"
                					}
                				}]
                			},
                			"pageMarginState": {
                				"top": 2.54,
                				"bottom": 2.54,
                				"left": 3.17,
                				"right": 3.17
                			},
                			"dpi": 96,
                			"paperSizeState": {
                				"width": 794,
                				"height": 1123,
                				"name": "A4"
                			},
                			"conditionArr": []
                		},
                		"password": false,
                		"name": "content",
                		"display_name": "输出内容",
                		"type": "dict",
                		"clear_after_run": true,
                		"list": false,
                		"field_type": "doc_editor",
                		"hide_show": true,
                		"hide_copy": true,
                		"display_index": 1000,
                		"conditionArr": []
                	},
                	"ETL#原始数据": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": true,
                		"value": [{
                			"报告期": "张三111111111111111111111111",
                			"项目": "18122132234132536478"
                		}, {
                			"报告期": "李四22222222222222222222",
                			"项目": "31324354635243123452"
                		}, {
                			"报告期": "王武22222222222222222222222222",
                			"项目": "21234312123454671"
                		}],
                		"options": [],
                		"password": false,
                		"name": "ETL#原始数据",
                		"display_name": "ETL#原始数据",
                		"type": "dict",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 101,
                		"node_id": "c7edaf03-67d3-48e1-a402-53aa341ce97e",
                		"node_type": "sql_etl",
                		"is_output": true,
                		"source_node_id": "c7edaf03-67d3-48e1-a402-53aa341ce97e",
                		"source_node_type": "sql_etl",
                		"source_node_handle": "output_standard_chart",
                		"source_node_name": "ETL",
                		"source_workflow_run_result_id": "2259943"
                	},
                	"ETL2#原始数据": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": true,
                		"value": [{
                			"企业名称": "青岛xx公司",
                			"法定代表人名称": "张三",
                			"成立时间": "2020年",
                			"持续经营年限": "25",
                			"注册地址": "山东青岛",
                			"经营地址": "香港中路",
                			"注册资本": "8000万",
                			"企业规模": "6000人",
                			"所属行业": "软件",
                			"经营范围": "软件服务,软件开发"
                		}],
                		"options": [],
                		"password": false,
                		"name": "ETL2#原始数据",
                		"display_name": "ETL2#原始数据",
                		"type": "dict",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 102,
                		"node_id": "6a1c90dc-c46c-4121-95ae-80ffc6e1b52b",
                		"node_type": "sql_etl",
                		"is_output": true,
                		"source_node_id": "6a1c90dc-c46c-4121-95ae-80ffc6e1b52b",
                		"source_node_type": "sql_etl",
                		"source_node_handle": "output_standard_chart",
                		"source_node_name": "ETL2",
                		"source_workflow_run_result_id": "2259944"
                	}
                }
                """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }


    @Test
    void process_CustomParams1_VerifyEffect() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"545232f2-2509-425b-9f34-ec3bfd04b361"}},{"type":"table","attrs":{"id":"337c8aab-eebb-4580-899b-0e3e3f8b99bc","width":556,"columnWidths":[556]},"content":[{"type":"tableRow","attrs":{"id":"d2492157-c08d-4aa3-9751-0302cd4264e9"},"content":[{"type":"tableCell","attrs":{"id":"8879ba73-8375-41dc-8c69-5224080d12be","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"a3479d3a-2edc-4954-8095-dd7d2c116f83"},"content":[{"type":"inlineTemplate","attrs":{"id":"f9180e45-5e88-4bd6-b830-7369235166c6","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#is_completed}}"}]}]},{"type":"paragraph","attrs":{"id":"419fb76e-0401-4293-b734-4d6c04b0a101"},"content":[{"type":"inlineTemplate","attrs":{"id":"4ddc16cf-87a1-4acb-a4da-f12553023025","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$2#is_completed}}"}]}]},{"type":"paragraph","attrs":{"id":"f3e6f9ed-ef26-4b41-baab-18b3e3f131f6"},"content":[{"type":"inlineTemplate","attrs":{"id":"e35a561a-36e5-44a3-8401-289690bdd806","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$3#is_completed}}"}]},{"type":"hardBreak","attrs":{"id":"ff59fc82-bf48-4a5b-bd41-31d4333ed83e"}}]}]}]},{"type":"tableRow","attrs":{"id":"53c17248-e079-4d16-9e15-ac1fa3ff19a7"},"content":[{"type":"tableCell","attrs":{"id":"33efffea-c14b-4806-a7a9-92874ceb7406","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"0bd594cd-98be-48fc-90b1-63bb0a3d493b"},"content":[{"type":"inlineTemplate","attrs":{"id":"d0425591-d886-4cfe-9e27-6badfdc3b8c8","isTemplate":"radio","backgroundColor":"#fff7e6"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#test_result}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"47325946-ebfe-445d-a2de-5340615f02fb"},"content":[{"type":"inlineTemplate","attrs":{"id":"96b842f3-7f63-4ea2-9e2a-9e11c75a91be","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#is_completed}}"}]}]},{"type":"paragraph","attrs":{"id":"ff7e480d-cc98-42dc-9dba-46eac91094b7"},"content":[{"type":"inlineTemplate","attrs":{"id":"1622058b-dbce-4041-bbc5-ac64841a86c0","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#is_completed}}"}]}]},{"type":"paragraph","attrs":{"id":"7e54b858-3486-45a3-b8b6-d69be9b4c259"},"content":[{"type":"inlineTemplate","attrs":{"id":"7d2788e0-ea12-4929-8b3f-df93fafe6e73","isTemplate":"checkbox","backgroundColor":"#f6ffed"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#is_completed}}"}]}]},{"type":"paragraph","attrs":{"id":"5006f958-ab91-406c-8677-7ac3bec7b854"}},{"type":"paragraph","attrs":{"id":"a8b8aff0-003f-4632-ac11-250630c4abdd"},"content":[{"type":"inlineTemplate","attrs":{"id":"e5b7cea9-eb1f-4a79-bb85-923fb7359646","isTemplate":"radio","backgroundColor":"#fff7e6"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#test_result}}"}]}]}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":" 功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条 动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"96724acb-5d42-4e72-b92d-77b31a06680c","node_type":"sql_etl","is_output":true,"source_node_id":"96724acb-5d42-4e72-b92d-77b31a06680c","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2298992"}}
""";
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }
    @Test
    void process_CustomTable_VerifyEffect1() {
        String json = """
        {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"073ef2d0-3e71-4fca-9d6e-eb70fca3215a"}},{"type":"paragraph","attrs":{"id":"84591131-b747-4ccf-a3d9-a45ae2408224"}},{"type":"paragraph","attrs":{"id":"bf6b73cd-26a4-41f2-b081-13d29efa186f"}},{"type":"table","attrs":{"id":"1ad088b3-46fb-4598-8466-56e64c5db111","width":554,"columnWidths":[110,110,110,110,110]},"content":[{"type":"tableRow","attrs":{"id":"32e01c62-7fd3-4e50-a49c-b69d8c78c12a"},"content":[{"type":"tableCell","attrs":{"id":"59271b9e-9d73-4c25-9f7a-e8f178435701","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ca206da1-e329-4aed-a62c-b19146bc0b0f"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"企业名称"}]}]},{"type":"tableCell","attrs":{"id":"06d36753-3db2-40d8-a2a0-fb5dfb473347","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"975a372d-2e49-430a-afb8-fe151ad11928"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"法定代表人"}]}]},{"type":"tableCell","attrs":{"id":"f410e3ae-8cf2-4514-96b0-36049a4ff764","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"868c5b48-4ad8-4084-930d-485cf836e895"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"成产时间"}]}]},{"type":"tableCell","attrs":{"id":"86e34740-944e-4414-8bcd-5df4f5ed16bd","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"03f4ec67-990a-48e7-a1e3-6655a70505b5"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"持续经营年限"}]}]},{"type":"tableCell","attrs":{"id":"f1b3ad0e-cf4b-4ab4-9468-cf8d14aa5c2d","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"b2920a34-4d7e-47e2-aab0-ef9e78b2d159"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"经营范围"}]}]}]},{"type":"tableRow","attrs":{"id":"0a1c4f12-494a-45c4-b5df-58383d3a3cf6"},"content":[{"type":"tableCell","attrs":{"id":"77cd6333-78e6-4401-a753-b5f731b0d4c4","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"4e8b6409-4112-4a2b-af7f-0d484e90e1ae"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#企业名称}}"}]}]},{"type":"tableCell","attrs":{"id":"3824cbd3-9486-4b5d-ba80-e4bbe15f80a2","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"9d8a9ea4-aad6-4740-8fa8-325b67f709f1"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#法定代表人名称}}"}]}]},{"type":"tableCell","attrs":{"id":"d4e3439c-d8fb-4a07-812a-3f7e1c19fcae","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"80ea4f23-81d5-42b7-950d-771bccebae39"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#成立时间}}"}]}]},{"type":"tableCell","attrs":{"id":"edfb9c15-1a7e-4934-98df-78d23cc34a7e","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"40bfa00f-7b4f-4bb5-8ed5-768628601b3c"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#持续经营年限}}"}]}]},{"type":"tableCell","attrs":{"id":"8be4fe97-d3a0-4f82-a797-82871c1082c0","colspan":1,"rowspan":3},"content":[{"type":"paragraph","attrs":{"id":"b7f049ba-722b-4358-886b-9266076b1806"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#经营范围}}"}]}]}]},{"type":"tableRow","attrs":{"id":"4b27e378-665e-48f1-ac4f-2fe3f8a0f67e"},"content":[{"type":"tableCell","attrs":{"id":"faceaa81-24bc-4439-9fa0-41ec09a174b6","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"9283f011-dadc-4a63-9963-edb2be6bd018"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"注册资本"}]}]},{"type":"tableCell","attrs":{"id":"5ae47155-3714-404b-bdf0-f0a7e5f6dfea","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"2221b3b6-c3e1-4887-9ffc-76a9227fda5b"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"企业规模"}]}]},{"type":"tableCell","attrs":{"id":"c78dcd08-4171-491c-83e4-244a6ba3670c","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"9a98a53b-9d0e-42d2-b09b-e23c93efcc5a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"所属行业"}]}]},{"type":"tableCell","attrs":{"id":"835e7d15-ab88-4161-9b25-0d912c792115","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"2ec5c3fd-dd5e-437d-87bc-fa16f56136d9"}}]}]},{"type":"tableRow","attrs":{"id":"65de9e88-ef57-4bfa-96b8-7cead67d915f"},"content":[{"type":"tableCell","attrs":{"id":"91f66e59-7580-4582-a557-d499100ea73b","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"64b2da06-5a8f-48c0-8b51-40b9a0cccf66"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#注册资本}}"}]}]},{"type":"tableCell","attrs":{"id":"be15197a-ef1d-4f29-a6ae-e53cf8f4e663","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"bb72e58e-8da0-44e2-8a5d-b05e6935e20c"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#企业规模}}"}]}]},{"type":"tableCell","attrs":{"id":"adda4444-688b-4e3e-834f-e2950c9a54a6","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"4a688337-bd82-444b-b0fd-8beef11d0ea1"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#所属行业}}"}]}]},{"type":"tableCell","attrs":{"id":"7a79e25b-8f13-42c5-9e3b-77037804e8ad","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ea7d5763-8488-462f-95d7-3e7bf1481a36"}}]}]},{"type":"tableRow","attrs":{"id":"40e7a5e8-681c-44f6-97bd-8e813e4c8338"},"content":[{"type":"tableCell","attrs":{"id":"534d957e-b314-49bc-ad70-81f34aab3c66","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"9109e9d7-a943-460a-b7cb-252e79df7695"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"16","backgroundColor":"","lineHeight":""}}],"text":"注册地址"}]}]},{"type":"tableCell","attrs":{"id":"4d1edc00-7ed9-4add-9097-a14d6ec6373c","colspan":4,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"209acc9a-5261-452d-b032-5db020470ca0"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL# 原始数据#$1#注册地址}}"}]}]}]},{"type":"tableRow","attrs":{"id":"8260254c-9d6d-456f-a42c-3a5813d3f1c8"},"content":[{"type":"tableCell","attrs":{"id":"6d487e8e-4ada-4c05-af3d-418b8e6255ab","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"3f65da82-66e0-4f46-93c2-f0f86796c803"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"16","backgroundColor":"","lineHeight":""}}],"text":"经营地址"}]}]},{"type":"tableCell","attrs":{"id":"423bdd78-c8e5-46db-8920-07b36d7ad009","colspan":4,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ce7e8eee-9520-4740-ae49-b488b6116d58"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"","lineHeight":""}}],"text":"{{ETL#原始数据#$1#经营地址}}"}]}]}]}]},{"type":"paragraph","attrs":{"id":"3761ee9c-0066-4493-aa0a-64ec9a22641c"},"content":[{"type":"hardBreak","attrs":{"id":"0c794050-d53a-48a0-aa63-bb5b59617863"},"marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"14","backgroundColor":"rgb(242, 244, 247)","lineHeight":""}}]}]},{"type":"paragraph","attrs":{"id":"a697ff96-05e8-41c7-92ee-3cc434a905aa"}},{"type":"paragraph","attrs":{"id":"02bdaf98-5870-449a-80a0-9fd3f29bb2d4"}},{"type":"paragraph","attrs":{"id":"9663eab0-6df5-4d60-b9ac-259943620aa0"}},{"type":"table","attrs":{"id":"3f8b5152-07d3-4f88-9ca2-1d79b6f18380","width":554,"columnWidths":[184,184,184]},"content":[{"type":"tableRow","attrs":{"id":"f08018a7-1922-48a6-b7fd-1855b9926c7d"},"content":[{"type":"tableCell","attrs":{"id":"4c7c23c8-d870-4249-b509-9f851664f61b","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"3ceb20f7-8b43-424b-ae85-1ef8c554f337"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"11c58300-5d2d-4ac7-a4be-77f02f988304","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ac4ff465-9b32-417f-8858-c3657577d28a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"报告"}]}]},{"type":"tableCell","attrs":{"id":"5b36e02d-5998-49a3-8191-aead6382f8bf","colspan":1,"rowspan":3},"content":[{"type":"paragraph","attrs":{"id":"ae567681-1015-4f87-a86c-51883de40829"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"备注"}]}]}]},{"type":"tableRow","attrs":{"id":"9b81f5bb-f2ed-45ef-94be-76049911c49a","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"9e3555e1-1f0a-4d01-8201-b9a864d8e536","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"055ea959-4915-4818-9d7f-69e4db733ec2"},"content":[{"type":"inlineTemplate","attrs":{"id":"75cd4fe2-d07e-46af-b5a8-1ea3de2b2838","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"045c2e8a-edb5-4313-8634-629d878852f7","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"60a24122-b05f-4496-b57d-6f8e1b821a94"},"content":[{"type":"inlineTemplate","attrs":{"id":"5107cf30-1efe-4f73-90f5-31c94943d9c5","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]}]},{"type":"tableRow","attrs":{"id":"2083a11b-0337-422b-a496-845fd3033c92","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"8c2fd41f-a06a-4d6a-ad63-e000f3982874","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"3618ec54-759f-4c4a-8e36-459c563aaee7"},"content":[{"type":"inlineTemplate","attrs":{"id":"5fb42ae6-8ec7-4041-91bd-f70a27cee7ee","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"5cf107d1-e7fd-44ce-bed4-7ae6c5c3de16","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"495f54b9-0339-47a8-b9d6-ef7d3546caf6"},"content":[{"type":"inlineTemplate","attrs":{"id":"ef136840-3012-49b6-822c-89fe5b557cb2","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"b4cd979e-4618-4440-b803-6cfc22de5a6a"}},{"type":"table","attrs":{"id":"8adcfb6a-556d-4a11-84d5-a40e9bd75ac7","width":554,"columnWidths":[184,184,184]},"content":[{"type":"tableRow","attrs":{"id":"fe7fd0aa-51a1-4fd8-b7f0-0d038109eb56"},"content":[{"type":"tableCell","attrs":{"id":"15a91b1f-a06b-4954-92ed-de76c3b686a6","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ab385936-8071-4724-be9d-95c6f8a465f9"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"报告"}]}]},{"type":"tableCell","attrs":{"id":"7e402df6-d1db-4e05-b852-5b068508820a","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"65daa399-c3ab-473a-9f73-e8bab1285c4d"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"1b81007b-7143-41fc-aa06-ad5619214a78","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"57685408-cfe6-4e42-8afd-3538899fe88c"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"插入"}]}]}]},{"type":"tableRow","attrs":{"id":"4fddcd7d-024e-470a-9aad-a65b29bf08e4","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"126a2c28-745d-49da-90aa-9bf3bf29eb80","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"9be9b1c1-f04e-4f2b-8fd3-6a1b96229060"},"content":[{"type":"inlineTemplate","attrs":{"id":"b7fe383c-26f5-4b5e-8480-140e8338eff0","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"6eeeb532-eea7-4ddd-aff3-1fa91ac0bf47","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"3e0fb822-9b6e-4e44-b0ff-3602d1eabac0"},"content":[{"type":"inlineTemplate","attrs":{"id":"66fdb828-5aa8-4f35-8887-8699a16e4deb","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"8bfdab02-2f33-4cac-a1ee-2d5fb312dc98","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"ff7dc8df-b10c-414d-b769-fea9ecd61ea8"},"content":[{"type":"inlineTemplate","attrs":{"id":"e7947f93-1209-41f3-9f22-a7708b79bd2a","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#企业名称}}"}]}]}]}]},{"type":"tableRow","attrs":{"id":"497ec023-6a01-4b79-ae95-63b524d89e76","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"c61133d1-2790-495d-b79e-4997522127ab","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"2b7e042e-7e88-40fe-8c26-0f4398d41ee5"},"content":[{"type":"inlineTemplate","attrs":{"id":"b5ab7875-e981-47d5-bf14-c7c34b90cdba","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"90f7d206-09df-46ef-b262-1b823eed1ba9","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"24b742fc-4f59-4e2c-a81d-dd44f4dfb26f"},"content":[{"type":"inlineTemplate","attrs":{"id":"506f764b-99db-4ac7-98ea-385f6b0c43af","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"3a54fc55-4435-4937-9f3c-4ea65c1fe0dd","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"f0b1ae42-3703-4f75-b890-82a9ff021a52"},"content":[{"type":"inlineTemplate","attrs":{"id":"6e53e435-7433-4d8f-b937-015fe37ed5c9","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#企业名称}}"}]}]}]}]},{"type":"tableRow","attrs":{"id":"35386b73-1643-4e74-ad15-ea6ecee26c85"},"content":[{"type":"tableCell","attrs":{"id":"6187fa99-0b6f-485e-b7fb-1dc6dbb4da02","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"67a069fe-1c9d-412f-b594-d54847588a39"},"content":[{"type":"inlineTemplate","attrs":{"id":"85fcd864-6326-4c61-9d58-d8cd870ed02a","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#持续经营年限}}"}]}]}]},{"type":"tableCell","attrs":{"id":"85cb0609-ed0e-4f83-b7de-7733569ffcd2","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"a841a731-b03d-47cb-81e1-b3d9cf116fa3"},"content":[{"type":"inlineTemplate","attrs":{"id":"9394324b-6fa5-4379-9439-d77a7893e8bf","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#注册地址}}"}]}]}]},{"type":"tableCell","attrs":{"id":"25456fc9-c7e0-427b-9452-4b791176a8a8","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"f460d69e-d9f5-4286-a3f3-e0d8873b0a29"},"content":[{"type":"inlineTemplate","attrs":{"id":"40bd439a-45cc-4d4e-adee-ab1021c81621","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#注册地址}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"e0cbe365-eb0c-4a6f-9196-dd39053290b5"}},{"type":"table","attrs":{"id":"9d319f6a-5ee9-4158-b89e-01e555bd6f05","width":554,"columnWidths":[277,277]},"content":[{"type":"tableRow","attrs":{"id":"56dd52c1-3812-4383-8ec8-daa70b6482b5"},"content":[{"type":"tableCell","attrs":{"id":"2e6b3d99-f55c-4d8d-934b-2a8d72eee703","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"cc99cda8-1c0a-4b4f-8183-da17efd4bdbc"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"af04a1f0-7f7b-4fb2-a0f8-4a61f20179b4","colspan":1,"rowspan":2},"content":[{"type":"image","attrs":{"id":"c9bd4072-0881-4bbc-9585-6df54c8c00ba","src":"group1/M00/AE/46/CgAAcGk3fUWAE5uNAAAVMcsyfls905.png","alt":"chart-1765244229231.png","title":"chart-1765244229231.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}}]}]},{"type":"tableRow","attrs":{"id":"14da90cd-0d11-448a-875e-d6902064b849","loop":"true","startLoopIndex":0,"endLoopIndex":0},"content":[{"type":"tableCell","attrs":{"id":"252d4cdf-3943-4be8-b656-cd551ce042f5","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"e511f4b7-1827-444d-ad48-0fd42e6cd6f0"},"content":[{"type":"inlineTemplate","attrs":{"id":"d28b661e-b94e-48b3-9134-3af9d5f2bc08","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]}]}]},{"type":"table","attrs":{"id":"7495d0eb-f05c-4639-b69b-1e723b4c11f9","width":554,"columnWidths":[138,138,138,138]},"content":[{"type":"tableRow","attrs":{"id":"db81313c-f9c1-4de3-b303-6dd9b3355295"},"content":[{"type":"tableCell","attrs":{"id":"99a0802b-3194-4bef-a6a4-f9df7892ab6f","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"35e7e6d7-4e72-4da0-a317-9ee2d145d08a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"报告期"}]}]},{"type":"tableCell","attrs":{"id":"dadb0962-f94d-424c-906b-fb8ef7be1f6d","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"5d1a5ab0-4aaf-4076-ab03-f44b5ba8b247"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"ded26d38-5f1d-4e55-b976-c3c00d13ee03","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"c1422baa-e308-4997-99b0-648b6765e594"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"普通"}]}]},{"type":"tableCell","attrs":{"id":"cb58cb7b-ed4b-4111-a2db-18eec4390d5d","colspan":1,"rowspan":4},"content":[{"type":"image","attrs":{"id":"91949237-bdf4-4bee-9fdf-f4f49f825d3b","src":"group1/M00/AE/46/CgAAcGk3fV2ATi6HAAAVMcsyfls219.png","alt":"chart-1765244253715.png","title":"chart-1765244253715.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}}]}]},{"type":"tableRow","attrs":{"id":"6a7d281f-0984-4ad0-b94a-72c8464ff5ea","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"9be45bad-1e10-4db8-9b4f-a294c5943e8b","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"75ba150d-f633-460b-8437-6718774eb6c9"},"content":[{"type":"inlineTemplate","attrs":{"id":"f44f08da-55f1-47d0-8e48-6dc10439b333","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"f8e97c42-5846-4eff-87fd-db6b1e4a00d6","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"ce6a4207-7620-4c42-a76a-5532eed07c08"},"content":[{"type":"inlineTemplate","attrs":{"id":"22f1f4ad-090a-46aa-acc1-eaa00bfb7c0d","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"30c0007a-6ce4-43c8-8e8f-c0f5e70d6664","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"c108ae00-3431-4cc6-818b-a11bdf032a62"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"1"}]}]}]},{"type":"tableRow","attrs":{"id":"4ef8c006-fe34-4b92-a2af-896ede2a7f51","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"3dbf1aab-8ee2-4294-9396-1901df8ae010","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"d5a8844e-6b46-4efe-a911-2351bc9a0202"},"content":[{"type":"inlineTemplate","attrs":{"id":"1c1d87bf-71a5-4d25-b61a-19774d6f40ae","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"fe7fa428-4c9c-49bc-ba18-d425ef8f53e3","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"1366194e-ad9f-4297-8eab-18f0fab4de39"},"content":[{"type":"inlineTemplate","attrs":{"id":"2c8521b3-4560-483e-9cf5-a4273ee0facb","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"998a7f6a-3c43-4998-b3a9-ffb10c0a320f","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"2058d1fd-73d5-4925-a27c-dd001e7416e2"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"2"}]}]}]},{"type":"tableRow","attrs":{"id":"53e2b8f4-bae1-4574-a829-c7301f2b075d"},"content":[{"type":"tableCell","attrs":{"id":"305aa574-4017-4317-9ed4-a065a821ec0a","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"a89dff86-9fd4-47b2-becb-6efc1b745992"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"4"}]}]},{"type":"tableCell","attrs":{"id":"32eac93e-855e-4bba-9ba3-8c89e0c161ed","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"f4393b2c-0874-4f50-9d5a-cb2995cc215d"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"5"}]}]},{"type":"tableCell","attrs":{"id":"4c5d94b9-7e0b-4ef5-9be3-5777b1b23557","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"cc74cbd3-6de4-4bf7-92cf-84bc548737f7"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"3"}]}]}]}]},{"type":"paragraph","attrs":{"id":"d88a5c89-6625-40bb-b8c2-172756333dc7"}},{"type":"table","attrs":{"id":"1488ce24-6d9a-4e8a-bb1e-52a2ff28616f","width":554,"columnWidths":[184,184,184]},"content":[{"type":"tableRow","attrs":{"id":"c632b508-d508-47a4-8bef-84d53b72f4ea"},"content":[{"type":"tableCell","attrs":{"id":"d8c4e76a-8cb0-45da-8894-7bacfa9912a7","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"a8986db1-85d1-475e-bcaa-0bfcebe4bf6d"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"报告期"}]}]},{"type":"tableCell","attrs":{"id":"42e2ef4e-0ece-4956-a6be-1960a9a3ccac","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"8fa13829-41bf-46f4-80a1-f92d95a10d18"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"85b82409-099d-4f87-b96f-e04eafa6bb83","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"4526b215-aafd-4ef3-b9a3-999abee0931b"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"列"}]}]}]},{"type":"tableRow","attrs":{"id":"8e4066fe-6d5c-42d7-9d45-49cd3f5c4d9e","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"94ebb6ff-47d7-4b26-ba25-a657d77508a2","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"8d8a2abd-1662-4a96-b937-a36ade087b9e"},"content":[{"type":"inlineTemplate","attrs":{"id":"aca3749f-39ba-4eb1-a6e3-c1b8e9646907","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"9a62f5a3-e17a-4af4-a299-bace41f26eb4","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"9eeeda69-c31c-4fe1-af21-02530e6d211d"},"content":[{"type":"inlineTemplate","attrs":{"id":"f12b121b-0864-4576-a293-ff41d15d7702","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"b2cd6cc6-0ea6-440e-b301-b4f0a2b5b212","colspan":1,"rowspan":1},"content":[{"type":"image","attrs":{"id":"9ffcd490-50af-4d8a-862d-8496506344c1","src":"group1/M00/AE/46/CgAAcGk3fMKAFkUmAAAVMcsyfls155.png","alt":"chart-1765244098628.png","title":"chart-1765244098628.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}}]}]}]},{"type":"paragraph","attrs":{"id":"ef836909-56e1-4024-89d4-aafc880e3ecc"}},{"type":"table","attrs":{"id":"4deac28d-6377-4120-8ae6-1603d04466b6","width":554,"columnWidths":[184,184,184]},"content":[{"type":"tableRow","attrs":{"id":"fb566d22-55dc-4b11-a9e6-2b77d70a77a5"},"content":[{"type":"tableCell","attrs":{"id":"5b4eadaf-263b-468f-8287-5be370be75ec","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"11841975-0b60-474b-9b4b-86e0270caa16"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"报告期"}]}]},{"type":"tableCell","attrs":{"id":"0365b4b8-0489-43e8-b876-bec459cf6e9d","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"f5b8dca7-4c39-4065-a50f-c854234736b7"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"项目"}]}]},{"type":"tableCell","attrs":{"id":"0e9a724f-9309-4946-aa77-d42f4f52fb27","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"6d44d80a-3801-4329-82b0-99d5ae6b95a0"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"插入"}]}]}]},{"type":"tableRow","attrs":{"id":"409d9ece-3fdd-4c25-bf3c-917615e79d6b","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"947ef948-16ed-4814-a7b5-1e0469698550","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"fd4b38fd-c7d7-4c0d-a22c-16d1e469e280"},"content":[{"type":"inlineTemplate","attrs":{"id":"4a1da126-eae6-4b9d-931b-967c37ffdcee","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"851d1dbe-402d-4045-afc0-7ecc9e8deebb","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"6a7bc193-016f-442d-8e41-47560f5f7ced"},"content":[{"type":"inlineTemplate","attrs":{"id":"9e0ddb14-a417-4d12-94d7-39322b8b896e","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#项目}}"}]}]}]},{"type":"tableCell","attrs":{"id":"424b4d21-6c8f-4a56-af9e-4f9fac543d26","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"35473cea-f4f2-4d41-858b-5497075c87cb"},"content":[{"type":"inlineTemplate","attrs":{"id":"5d949000-5b1b-4e48-883f-1e30b3547529","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{hwj#原始数据#$1#企业名称}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"d41f37eb-7c7b-4581-b918-9cae8161af5d"}},{"type":"table","attrs":{"id":"90c3d541-4a58-4017-9c62-c779bf6b7875","width":554,"columnWidths":[277,277]},"content":[{"type":"tableRow","attrs":{"id":"2e7d38bd-1df4-4acc-a0d1-187a6cdcce3c"},"content":[{"type":"tableHeader","attrs":{"id":"35fdeb22-aa14-48f1-a23d-39d3ed07904d","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"538539eb-3255-4b88-afa8-15fe971d3b1e"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"单选"}]}]},{"type":"tableHeader","attrs":{"id":"b22f49d8-d51a-4fe6-8edb-966b6b2f43d6","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"1f118cf7-fa01-4f6b-b22e-27d9123307aa"},"content":[{"type":"inlineTemplate","attrs":{"id":"cef31a7c-c37d-4d56-91e9-37ce6ce0210a","isTemplate":"radio","backgroundColor":"rgb(255, 247, 230)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"{{ETL3#原始数据#$1#test_result}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(255, 247, 230)","lineHeight":""}}]}]}]}]},{"type":"tableRow","attrs":{"id":"e5ada009-a2a0-4466-a503-478ecf9f3ec4"},"content":[{"type":"tableCell","attrs":{"id":"de197036-7ffc-48d2-9c6a-dbb5051b1921","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"3e290758-fd3b-4c9c-86e4-8b0453bba403"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"复选"}]}]},{"type":"tableCell","attrs":{"id":"c685fd10-70a6-40cd-90e3-d2f8b04f1962","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"766ab437-92a7-4c4d-bb8a-f688b351a21f"},"content":[{"type":"inlineTemplate","attrs":{"id":"5c82d607-21c2-495d-9836-e6979f4b860b","isTemplate":"checkbox","backgroundColor":"rgb(246, 255, 237)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"{{ETL3#原始数据#$1#is_completed}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(246, 255, 237)","lineHeight":""}}]}]},{"type":"paragraph","attrs":{"id":"0eec155e-73ec-4429-9f8f-bfd088fd57b7"},"content":[{"type":"inlineTemplate","attrs":{"id":"76848abf-1ad1-41d9-aea1-235c9eb98b57","isTemplate":"checkbox","backgroundColor":"rgb(246, 255, 237)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"{{ETL3#原始数据#$"},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"rgb(246, 255, 237)","lineHeight":"1"}}],"text":"2"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"#is_completed}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(246, 255, 237)","lineHeight":""}}]}]},{"type":"paragraph","attrs":{"id":"c96a4a99-3c48-4d08-92a2-17f3a89b6ea9"},"content":[{"type":"inlineTemplate","attrs":{"id":"d1bf3b6d-017c-40d1-8c1c-5ebebbc1c823","isTemplate":"checkbox","backgroundColor":"rgb(246, 255, 237)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"{{ETL3#原始数据#$"},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"rgb(246, 255, 237)","lineHeight":"1"}}],"text":"3"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"#is_completed}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(246, 255, 237)","lineHeight":""}}]}]}]}]},{"type":"tableRow","attrs":{"id":"c779ed41-51be-4d3a-9a60-37501b0e3f2b"},"content":[{"type":"tableCell","attrs":{"id":"001b6f51-ca40-4891-8c71-76e4ab24cc04","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"67e20db8-bfee-4f3d-86f9-95454b10f1f7"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"提示"}]}]},{"type":"tableCell","attrs":{"id":"61b92130-6bf0-4d11-a70c-5f96b7c0abd8","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"42e820f1-ffd6-4381-937f-76a889dd7877"},"content":[{"type":"inlineTemplate","attrs":{"id":"d6ede3a1-285b-4580-801d-33cfbdf5e312","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"{{普通提示#输出}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)","lineHeight":""}}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"577b5d83-4744-4099-ba34-01942f142422"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"hwj#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000万","企业规模":"6000人","所属行业":"软件","经营范围":"软件服务,软件开发"}],"options":[],"password":false,"name":"hwj#原始数据","display_name":"hwj#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"3e3f2e75-0392-4cf1-9c68-fac7a2efb629","node_type":"sql_etl","is_output":true,"source_node_id":"3e3f2e75-0392-4cf1-9c68-fac7a2efb629","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"hwj","source_workflow_run_result_id":"2298451"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"4bcdfc3b-736d-4022-afdc-4ed3540123b8","node_type":"sql_etl","is_output":true,"source_node_id":"4bcdfc3b-736d-4022-afdc-4ed3540123b8","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2298453"},"图 表#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"type":"chart_bar","title":"","xAxisTitle":"","yAxisTitle":"","colors":["#5470c6","#91cc75","#fac858","#ee6666","#73c0de"],"colorMatch":"default","showTitle":true,"showGrid":true,"showLegend":true,"showAxisLabel":true,"showDataLabel":false,"showAxis":true,"seriesConfig":[],"legend":["项目"],"valueList":[{"name":"张三111111111111111111111111","value":[1.8122132234132537E19]},{"name":"李四22222222222222222222","value":[3.1324354635243123E19]},{"name":"王武22222222222222222222222222","value":[2.123431212345467E16]}]},"options":[],"password":false,"name":"图表#输出","display_name":"图表#输出","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"node_id":"3b7a23af-ed47-47f0-85a9-1e1f3a84798a","node_type":"chart_setup","is_output":true,"source_node_id":"3b7a23af-ed47-47f0-85a9-1e1f3a84798a","source_node_type":"chart_setup","source_node_handle":"output","source_node_name":"图表","source_workflow_run_result_id":"2298454"},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000万","企业规模":"6000人","所属行业":"软件","经营范围":"软件服务,软件开发"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"node_id":"27ffd343-89d2-45d3-b260-7541a3cd5509","node_type":"sql_etl","is_output":true,"source_node_id":"27ffd343-89d2-45d3-b260-7541a3cd5509","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2298457"},"ETL3#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL3#原始数据","display_name":"ETL3#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":105,"node_id":"39b0dedb-da47-40cd-a00a-3f3c4f5e5e9d","node_type":"sql_etl","is_output":true,"source_node_id":"39b0dedb-da47-40cd-a00a-3f3c4f5e5e9d","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL3","source_workflow_run_result_id":"2298456"},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"截至目前(2024年6月),贵州茅台(600519.SH)尚未正式发布其2024年度的完整财务报告。因此,对贵州茅台2024年财务情况的分析主要基于以下几方面进行合理预测与趋势判断:\\n\\n---\\n\\n### 一、2024年财务预测背景\\n\\n#### 1. **宏观经济环境**\\n- 中国经济在2024年持续复苏,消费市场逐步回暖,尤其是高端白酒需求呈现温和回升态势。\\n- 政策层面支持“提振消费”和“扩大内需”,为高端消费品(如茅 台)创造了有利环境。\\n- 消费者信心指数稳步回升,中产阶级对高品质生活的需求仍具韧性。\\n\\n#### 2. **行业趋势**\\n- 高端白酒行业整体进入“稳增长、控量保价”阶段,茅台作为行业龙头,继续执行“控量挺价”策略。\\n- 市场对茅 台“稀缺性”和品牌价值的认知持续强化,价格体系保持稳定甚至略有上行。\\n\\n---\\n\\n### 二、2024年财务表现预测(基于2023年数据及当前趋势)\\n\\n| 指标 | 2023年实际值 | 2024年预测值(估算) | 变化趋势 |\\n|------|----------------|------------------------|----------|\\n| 营业收入 | 约150亿元 | 约158–162亿元 | +5%~8% |\\n| 归母净利润 | 约77亿元 | 约82–86亿元 | +6%~10% |\\n| 毛利率 | 约92% | 维持在92%以上 | 稳定或微升 |\\n| 净利率 | 约51% | 约52%–53% | 略有提升 |\\n| 总资产收益率(ROE) | 约30% | 约31%–32% | 稳中有升 |\\n\\n> 注:上述数据为基于公开信息、行业趋势及公司历史表现的合理推断,非官方披露。\\n\\n---\\n\\n### 三、关键驱动因素分析\\n\\n#### 1. **产品结构优化**\\n- 茅台持续推进“i茅台”数字化平台建设,提升直营渠道占比,减少中间环节,增强利润空间。\\n- 2024年预计“i茅台”线上销售占比进一步提升,有望突破30%,带动毛利率改善。\\n\\n#### 2. **提价预期与控量策略**\\n- 尽 管2024年未官宣提价,但市场普遍预期未来将适度提价(如飞天茅台出厂价上调5%-10%),以应对成本压力并维持品牌溢价。\\n- 公司坚持“控量保价”策略,避免价格剧烈波动,维护高端形象。\\n\\n#### 3. **非标产品放量**\\n- 茅台系列酒(如赖茅、王子、迎宾等)持续发力,2024年预计营收贡献占比提升至约15%-18%。\\n- 通过丰富产品线满足不同消费层级需求,拓展增量空间。\\n\\n#### 4. **国际化布局推进**\\n- 茅台加快海外市场布局,尤其在东南亚、欧洲、北美等地加强品牌推广。\\n- 2024年海外收入预计同比增长10%-15%,成为新增长点之一。\\n\\n---\\n\\n### 四、潜在风险与挑战\\n\\n1. **消费税改革预期**  \\n   若国家对高端消费品征收更高消费税,可能影响茅台终端价格和利润空间。\\n\\n2. **库存压力与渠道管理**  \\n   若经销商库存过高,可能引发价格回调风险,影响品牌形象。\\n\\n3. **经济下行压力**  \\n   若居民可支配收入增速放缓,高端白酒消费可能承压。\\n\\n4. **竞争加剧**  \\n   五粮液、泸州老窖等品牌在高端市场持续发 力,对茅台形成一定挤压。\\n\\n---\\n\\n### 五、总结与展望\\n\\n> **2024年贵州茅台预计将实现稳健增长,核心逻辑在于:品牌护城河深厚、渠道管控能力强、产品结构持续优化、数字化转型成效显著。**\\n\\n- **核心亮点**:盈利能力强、现金流充沛、分红能力持续领先(预计2024年每股分红维持在30元以上)。\\n- **投资价值**:作为A股“核心资产”代表,茅台仍是长期配置型投资者的重要标的。\\n- **关注重点**:2024年年报(预计2025年3月发布)将披露真实数据,届时需重点 关注营收增速、毛利率变化、直营渠道占比、非标酒收入贡献等关键指标。\\n\\n---\\n\\n### 建议投资者关注:\\n- 2024年第三季度财报(10月发布)——观察季度增长趋势;\\n- “i茅台”平台用户活跃度与销售数据;\\n- 海外市场拓展进展;\\n- 2024年股东大会关于分红政策的表态。\\n\\n---\\n\\n📌 **温馨提示**:以上分析基于公开信息与合理推演,不构成任何投资建议。请以贵州茅台官方发布的2024年年度报告为准。\\n\\n如需我协助解读未来可能发布的年报内容,欢迎随时提问。","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":106,"node_id":"9a81c0be-a83e-4d35-81f0-b614299cff92","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"9a81c0be-a83e-4d35-81f0-b614299cff92","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2298452"},"借款人":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"","options":[],"password":false,"name":"借款 人","display_name":"借款人","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true},"是否展示":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"","options":[],"password":false,"name":"是否展示","display_name":"是否展示","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"is_start":true}}
      """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process_CustomTable_VerifyEffect111() throws Exception {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":true,"value":{"data":{"header":[],"main":[{"value":"↪","position":"df279006c7034a93822992c09e9800c6","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\"==\\"是\\" >","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"↩","position":"df279006c7034a93822992c09e9800c6","conditionDirection":"end","unlock":false},{"value":"\\n"},{"value":"\\n","unlock":false},{"value":"↪","position":"c6b382c2b87d4c71be42d76bf1421a6e","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"是\\" >","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"↩","position":"c6b382c2b87d4c71be42d76bf1421a6e","conditionDirection":"end","unlock":false},{"value":"\\n"},{"value":"\\n","unlock":false},{"value":"↪","position":"e619f9eabbec4b8e9a06a1155dd6ef78","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"是\\">","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"↩","position":"e619f9eabbec4b8e9a06a1155dd6ef78","conditionDirection":"end","unlock":false},{"value":"\\n"},{"value":"\\n","unlock":false},{"value":"↪","position":"12e6a0a087774506b32f3ad7b3f9678c","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"不\\" >","type":"text","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"\\n","unlock":false},{"value":"11","font":"宋体","size":14,"color":"#000000","unlock":false},{"value":"\\n","type":"text","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"↩","position":"12e6a0a087774506b32f3ad7b3f9678c","conditionDirection":"end","unlock":false},{"value":"\\n"},{"value":"\\n","unlock":false},{"value":"↪","position":"92495b69a2f243b5a7000612eeb5eab6","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"不\\" >","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"\\n","unlock":false},{"value":"22","font":"宋体","size":14,"color":"#000000","unlock":false},{"value":"\\n","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"↩","position":"92495b69a2f243b5a7000612eeb5eab6","conditionDirection":"end","unlock":false}],"footer":[]},"options":{"mode":"edit","defaultType":"TEXT","defaultColor":"#000000","defaultFont":"宋体","defaultSize":14,"minSize":5,"maxSize":72,"defaultRowMargin":1,"defaultBasicRowMarginHeight":8,"defaultTabWidth":32,"width":794,"height":8000,"scale":1,"pageGap":20,"underlineColor":"#000000","strikeoutColor":"#FF0000","rangeAlpha":0.6,"rangeColor":"#AECBFA","rangeMinWidth":5,"searchMatchAlpha":0.6,"searchMatchColor":"#FFFF00","searchNavigateMatchColor":"#AAD280","highlightAlpha":0.6,"resizerColor":"#4182D9","resizerSize":5,"marginIndicatorSize":35,"marginIndicatorColor":"#BABABA","margins":[100,120,100,120],"pageMode":"paging","renderMode":"speed","defaultHyperlinkColor":"#0000FF","paperDirection":"vertical","inactiveAlpha":0.6,"historyMaxRecordCount":4,"wordBreak":"break-word","printPixelRatio":3,"maskMargin":[60,0,30,0],"letterClass":["A-Za-z"],"contextMenuDisableKeys":[],"scrollContainerSelector":"","pageNumber":{"bottom":60,"size":12,"font":"Microsoft YaHei","color":"#000000","rowFlex":"center","format":"第{pageNo}页/共{pageCount}页","numberType":"arabic","disabled":true,"startPageNo":1,"fromPageNo":0},"placeholder":{"data":"请输入正文","color":"#DCDFE6","opacity":1,"size":16,"font":"Microsoft YaHei"},"zone":{"tipDisabled":false},"header":{"top":30,"maxHeightRadio":"half","disabled":true,"editable":true},"footer":{"bottom":30,"maxHeightRadio":"half","disabled":true,"editable":true},"table":{"tdPadding":[0,5,5,5],"defaultTrMinHeight":42,"defaultColMinWidth":40},"watermark":{"data":"","color":"#AEB5C0","opacity":0.3,"size":200,"font":"Microsoft YaHei","repeat":false,"gap":[10,10]},"control":{"placeholderColor":"#9c9b9b","bracketColor":"#000000","prefix":"{","postfix":"}","borderWidth":1,"borderColor":"#000000","activeBackgroundColor":""},"checkbox":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#ffffff","verticalAlign":"bottom"},"radio":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#000000","verticalAlign":"bottom"},"cursor":{"width":1,"color":"#000000","dragWidth":2,"dragColor":"#0000FF"},"title":{"defaultFirstSize":26,"defaultSecondSize":24,"defaultThirdSize":22,"defaultFourthSize":20,"defaultFifthSize":18,"defaultSixthSize":16},"group":{"opacity":0.1,"backgroundColor":"#E99D00","activeOpacity":0.5,"activeBackgroundColor":"#E99D00","disabled":false},"pageBreak":{"font":"Microsoft YaHei","fontSize":12,"lineDash":[3,1]},"background":{"color":"#FFFFFF","image":"","size":"cover","repeat":"no-repeat","applyPageNumbers":[]},"lineBreak":{"disabled":true,"color":"#CCCCCC","lineWidth":1.5},"separator":{"lineWidth":1,"strokeStyle":"#000000"},"lineNumber":{"size":12,"font":"Microsoft YaHei","color":"#000000","disabled":true,"right":20,"type":"continuity"},"pageBorder":{"color":"#000000","lineWidth":1,"padding":[0,5,0,5],"disabled":true},"hiddenGroupIds":true},"dpi":96,"pageMarginState":{"left":3.18,"top":2.54,"right":3.18,"bottom":2.54},"paperSizeState":{"width":794,"height":8000,"name":""}},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000},"是否ViP":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"不","options":[],"password":false,"name":"是否ViP","display_name":"是否ViP","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000 万","企业规模":"6000人","所属行业":"软件","经营范围":"软件服务，软件开发"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"c45562ea-f4bb-43e0-ada6-dafc871c5a8e","node_type":"sql_etl","is_output":true,"source_node_id":"c45562ea-f4bb-43e0-ada6-dafc871c5a8e","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2299583"}}
                """;
        WorkflowRunResult workflowRunResult = new WorkflowRunResult();
        workflowRunResult.setInputs(JSONObject.parseObject(json));
        Response<List<JSONObject>> outputs = docOutput.getOutputs(workflowRunResult);
        System.out.println(outputs.getData());
    }

    @Test
    void process_CustomTable_VerifyEffect12() {
        String json = """
            {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"a6ce2fb0-0ea3-43c9-bbc3-cd19a89175f2"}},{"type":"table","attrs":{"id":"dee20c66-8e0c-4cac-b700-70f318adaf74","width":557,"columnWidths":[278,278]},"content":[{"type":"tableRow","attrs":{"id":"58675c22-4f5c-4943-9335-0bc364511459"},"content":[{"type":"tableCell","attrs":{"id":"cb77e8c3-36a8-49ee-8e2c-a20a90d54da2","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"237e1c80-505a-4192-9201-be9f46e1ef23"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.5"}}],"text":"子工作流"}]}]},{"type":"tableCell","attrs":{"id":"0d0ccd6d-c6b8-49c9-b2d9-b715eacf1689","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"8e8b5db0-cad2-4020-bd01-7e8719ef3147"},"content":[{"type":"inlineTemplate","attrs":{"id":"533d2b3a-01dd-4a7f-bf78-7a16f2e46bd6","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{111#输出}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"22a876e0-cbae-4233-a934-555b9eb11ca3"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"111#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"667e9bae-91d8-4def-baf5-ffafecbf0cda","textAlign":"left"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25"}}],"text":"1234567890987654321"}]}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"docOutput":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"667e9bae-91d8-4def-baf5-ffafecbf0cda","textAlign":"left"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25"}}],"text":"1234567890987654321"}]}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"fileUrl":"http://10.0.0.22:10105/workflow-editor?workflowId=2516&workflowRunId=732651"},"options":[],"password":false,"name":"111#输出","display_name":"111#输出","type":"doc_output","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"43c5db24-bfe1-4daa-a482-8f53ba56888c","node_type":"sub_workflow","is_output":true,"source_node_id":"43c5db24-bfe1-4daa-a482-8f53ba56888c","source_node_type":"sub_workflow","source_node_handle":"output","source_node_name":"111","source_workflow_run_result_id":"2304647"}}
     """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process_CustomTable_VerifyEffect1111() throws Exception {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":true,"value":{"data":{"header":[],"main":[{"value":"↪","position":"df279006c7034a93822992c09e9800c6","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\"==\\"是\\" >","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"df279006c7034a93822992c09e9800c6","isCondition":"true","unlock":false},{"value":"↩","position":"df279006c7034a93822992c09e9800c6","conditionDirection":"end","unlock":false},{"value":"\\n","unlock":false},{"value":"\\n","unlock":false},{"value":"↪","position":"c6b382c2b87d4c71be42d76bf1421a6e","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"是\\" >","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"c6b382c2b87d4c71be42d76bf1421a6e","isCondition":"true","unlock":false},{"value":"↩","position":"c6b382c2b87d4c71be42d76bf1421a6e","conditionDirection":"end","unlock":false},{"value":"\\n","unlock":false},{"value":"\\n","unlock":false},{"value":"↪","position":"e619f9eabbec4b8e9a06a1155dd6ef78","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"是\\">","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋 体","size":14,"color":"#000000","position":"e619f9eabbec4b8e9a06a1155dd6ef78","isCondition":"true","unlock":false},{"value":"↩","position":"e619f9eabbec4b8e9a06a1155dd6ef78","conditionDirection":"end","unlock":false},{"value":"\\n","unlock":false},{"value":"\\n","unlock":false},{"value":"↪","position":"12e6a0a087774506b32f3ad7b3f9678c","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"不\\" >","type":"text","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"\\n","unlock":false},{"value":"11","font":"宋体","size":14,"color":"#000000","unlock":false},{"value":"\\n","type":"text","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"12e6a0a087774506b32f3ad7b3f9678c","isCondition":"true","unlock":false},{"value":"↩","position":"12e6a0a087774506b32f3ad7b3f9678c","conditionDirection":"end","unlock":false},{"value":"\\n","unlock":false},{"value":"\\n","unlock":false},{"value":"↪","position":"92495b69a2f243b5a7000612eeb5eab6","conditionDirection":"start","unlock":false},{"value":"<#if \\"{{开始输入#是否ViP}}\\" == \\"不\\" >","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"\\n","unlock":false},{"value":"22","font":"宋体","size":14,"color":"#000000","unlock":false},{"value":"\\n","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??","type":"text","font":"宋体","size":14,"color":"#000000","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"\\n","type":"text","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"</#if>","type":"text","font":"宋体","size":14,"color":"#000000","position":"92495b69a2f243b5a7000612eeb5eab6","isCondition":"true","unlock":false},{"value":"↩","position":"92495b69a2f243b5a7000612eeb5eab6","conditionDirection":"end","unlock":false}],"footer":[]},"options":{"mode":"edit","defaultType":"TEXT","defaultColor":"#000000","defaultFont":"宋体","defaultSize":14,"minSize":5,"maxSize":72,"defaultRowMargin":1,"defaultBasicRowMarginHeight":8,"defaultTabWidth":32,"width":794,"height":8000,"scale":1,"pageGap":20,"underlineColor":"#000000","strikeoutColor":"#FF0000","rangeAlpha":0.6,"rangeColor":"#AECBFA","rangeMinWidth":5,"searchMatchAlpha":0.6,"searchMatchColor":"#FFFF00","searchNavigateMatchColor":"#AAD280","highlightAlpha":0.6,"resizerColor":"#4182D9","resizerSize":5,"marginIndicatorSize":35,"marginIndicatorColor":"#BABABA","margins":[100,120,100,120],"pageMode":"paging","renderMode":"speed","defaultHyperlinkColor":"#0000FF","paperDirection":"vertical","inactiveAlpha":0.6,"historyMaxRecordCount":4,"wordBreak":"break-word","printPixelRatio":3,"maskMargin":[60,0,30,0],"letterClass":["A-Za-z"],"contextMenuDisableKeys":[],"scrollContainerSelector":"","pageNumber":{"bottom":60,"size":12,"font":"Microsoft YaHei","color":"#000000","rowFlex":"center","format":"第{pageNo}页/共{pageCount}页","numberType":"arabic","disabled":true,"startPageNo":1,"fromPageNo":0},"placeholder":{"data":"请输入正文","color":"#DCDFE6","opacity":1,"size":16,"font":"Microsoft YaHei"},"zone":{"tipDisabled":false},"header":{"top":30,"maxHeightRadio":"half","disabled":true,"editable":true},"footer":{"bottom":30,"maxHeightRadio":"half","disabled":true,"editable":true},"table":{"tdPadding":[0,5,5,5],"defaultTrMinHeight":42,"defaultColMinWidth":40},"watermark":{"data":"","color":"#AEB5C0","opacity":0.3,"size":200,"font":"Microsoft YaHei","repeat":false,"gap":[10,10]},"control":{"placeholderColor":"#9c9b9b","bracketColor":"#000000","prefix":"{","postfix":"}","borderWidth":1,"borderColor":"#000000","activeBackgroundColor":""},"checkbox":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#ffffff","verticalAlign":"bottom"},"radio":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#000000","verticalAlign":"bottom"},"cursor":{"width":1,"color":"#000000","dragWidth":2,"dragColor":"#0000FF"},"title":{"defaultFirstSize":26,"defaultSecondSize":24,"defaultThirdSize":22,"defaultFourthSize":20,"defaultFifthSize":18,"defaultSixthSize":16},"group":{"opacity":0.1,"backgroundColor":"#E99D00","activeOpacity":0.5,"activeBackgroundColor":"#E99D00","disabled":false},"pageBreak":{"font":"Microsoft YaHei","fontSize":12,"lineDash":[3,1]},"background":{"color":"#FFFFFF","image":"","size":"cover","repeat":"no-repeat","applyPageNumbers":[]},"lineBreak":{"disabled":true,"color":"#CCCCCC","lineWidth":1.5},"separator":{"lineWidth":1,"strokeStyle":"#000000"},"lineNumber":{"size":12,"font":"Microsoft YaHei","color":"#000000","disabled":true,"right":20,"type":"continuity"},"pageBorder":{"color":"#000000","lineWidth":1,"padding":[0,5,0,5],"disabled":true},"hiddenGroupIds":true},"dpi":96,"pageMarginState":{"left":3.18,"top":2.54,"right":3.18,"bottom":2.54},"paperSizeState":{"width":794,"height":8000,"name":""}},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000},"是否ViP":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"不","options":[],"password":false,"name":"是否ViP","display_name":"是否ViP","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}} 
                """;
        WorkflowRunResult workflowRunResult = new WorkflowRunResult();
        workflowRunResult.setInputs(JSONObject.parseObject(json));
        Response<List<JSONObject>> outputs = docOutput.getOutputs(workflowRunResult);
        System.out.println(outputs.getData());
    }

    @Test
    void process_CustomTable_VerifyEffec111t1111() throws Exception {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":true,"value":{"data":{"header":[],"main":[{"value":"名称：","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","unlock":false},{"value":"{{ETL#原始数据#$1#企业名称}}","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","isTemplate":"text","position":"8913af03245a4e3dbfa44cd183845a1e","unlock":false},{"value":"\\n","font":"宋体","size":14,"color":"#000000","highlight":"#22d3ee","rowFlex":"center"},{"value":"\\n","rowFlex":"center","unlock":false},{"value":"年限：","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","unlock":false},{"value":"{{ETL#原始数据#$1#法定代表人名称}}","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","isTemplate":"text","position":"6b7ad988ee854ffeaa64387b406c2ba1","unlock":false},{"value":"\\n","font":"宋体","size":14,"color":"#000000","highlight":"#22d3ee","rowFlex":"center"},{"value":"\\n","rowFlex":"center","unlock":false},{"value":"时间：","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","unlock":false},{"value":"{{ETL#原始数据#$1#成立时间}}","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","isTemplate":"text","position":"cdde339ae04c4297889658155b36e3b7","unlock":false},{"value":"\\n","font":"宋体","size":14,"color":"#000000","highlight":"#22d3ee","rowFlex":"center"},{"value":"\\n","rowFlex":"center","unlock":false},{"value":"年至：","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","unlock":false},{"value":"{{ETL#原始数 据#$1#持续经营年限}}","type":"text","font":"宋体","size":14,"color":"#000000","rowFlex":"center","isTemplate":"text","position":"3e513bcb83bd4403926df918e86c4408","unlock":false}],"footer":[]},"options":{"mode":"edit","defaultType":"TEXT","defaultColor":"#000000","defaultFont":"宋体","defaultSize":14,"minSize":5,"maxSize":72,"defaultRowMargin":1,"defaultBasicRowMarginHeight":8,"defaultTabWidth":32,"width":794,"height":8000,"scale":1,"pageGap":20,"underlineColor":"#000000","strikeoutColor":"#FF0000","rangeAlpha":0.6,"rangeColor":"#AECBFA","rangeMinWidth":5,"searchMatchAlpha":0.6,"searchMatchColor":"#FFFF00","searchNavigateMatchColor":"#AAD280","highlightAlpha":0.6,"resizerColor":"#4182D9","resizerSize":5,"marginIndicatorSize":35,"marginIndicatorColor":"#BABABA","margins":[100,120,100,120],"pageMode":"paging","renderMode":"speed","defaultHyperlinkColor":"#0000FF","paperDirection":"vertical","inactiveAlpha":0.6,"historyMaxRecordCount":4,"wordBreak":"break-word","printPixelRatio":3,"maskMargin":[60,0,30,0],"letterClass":["A-Za-z"],"contextMenuDisableKeys":[],"scrollContainerSelector":"","pageNumber":{"bottom":60,"size":12,"font":"Microsoft YaHei","color":"#000000","rowFlex":"center","format":"第{pageNo}页/共{pageCount}页","numberType":"arabic","disabled":true,"startPageNo":1,"fromPageNo":0},"placeholder":{"data":"请输入正文","color":"#DCDFE6","opacity":1,"size":16,"font":"Microsoft YaHei"},"zone":{"tipDisabled":false},"header":{"top":30,"maxHeightRadio":"half","disabled":true,"editable":true},"footer":{"bottom":30,"maxHeightRadio":"half","disabled":true,"editable":true},"table":{"tdPadding":[0,5,5,5],"defaultTrMinHeight":42,"defaultColMinWidth":40},"watermark":{"data":"","color":"#AEB5C0","opacity":0.3,"size":200,"font":"Microsoft YaHei","repeat":false,"gap":[10,10]},"control":{"placeholderColor":"#9c9b9b","bracketColor":"#000000","prefix":"{","postfix":"}","borderWidth":1,"borderColor":"#000000","activeBackgroundColor":""},"checkbox":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#ffffff","verticalAlign":"bottom"},"radio":{"width":14,"height":14,"gap":5,"lineWidth":1,"fillStyle":"#5175f4","strokeStyle":"#000000","verticalAlign":"bottom"},"cursor":{"width":1,"color":"#000000","dragWidth":2,"dragColor":"#0000FF"},"title":{"defaultFirstSize":26,"defaultSecondSize":24,"defaultThirdSize":22,"defaultFourthSize":20,"defaultFifthSize":18,"defaultSixthSize":16},"group":{"opacity":0.1,"backgroundColor":"#E99D00","activeOpacity":0.5,"activeBackgroundColor":"#E99D00","disabled":false},"pageBreak":{"font":"Microsoft YaHei","fontSize":12,"lineDash":[3,1]},"background":{"color":"#FFFFFF","image":"","size":"cover","repeat":"no-repeat","applyPageNumbers":[]},"lineBreak":{"disabled":true,"color":"#CCCCCC","lineWidth":1.5},"separator":{"lineWidth":1,"strokeStyle":"#000000"},"lineNumber":{"size":12,"font":"Microsoft YaHei","color":"#000000","disabled":true,"right":20,"type":"continuity"},"pageBorder":{"color":"#000000","lineWidth":1,"padding":[0,5,0,5],"disabled":true},"hiddenGroupIds":true},"dpi":96,"pageMarginState":{"left":3.18,"top":2.54,"right":3.18,"bottom":2.54},"paperSizeState":{"width":794,"height":8000,"name":""}},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛"," 经营地址":"香港中路","注册资本":"8000万","企业规模":"6000人","所属行业":"软件","经营范围":"软件服务，软件开发"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"a600c312-360a-4522-b0d2-dabdd9210814","node_type":"sql_etl","is_output":true,"source_node_id":"a600c312-360a-4522-b0d2-dabdd9210814","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2305114"}}
                """;
        WorkflowRunResult workflowRunResult = new WorkflowRunResult();
        workflowRunResult.setInputs(JSONObject.parseObject(json));
        Response<List<JSONObject>> outputs = docOutput.getOutputs(workflowRunResult);
        System.out.println(outputs.getData());
    }

    @Test
    void process_CustomTable_VerifyEffect1111112() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"8447a09f-e188-49d3-9ae8-35ab7bd370af"}},{"type":"table","attrs":{"id":"ecb83809-9367-419a-817f-5a092fd4b1c0","width":555,"columnWidths":[185,185,185]},"content":[{"type":"tableRow","attrs":{"id":"85bb9562-340c-4819-a9d9-0656a7b5952e"},"content":[{"type":"tableCell","attrs":{"id":"12693381-cead-4498-bf62-dba2808c2190","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"427e5efd-059f-49cb-b943-cce4fb1872ee"},"content":[{"type":"inlineTemplate","attrs":{"id":"cfe0eb7b-19a3-4dc2-98b7-027163540d55","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1#日期}}"}]}]},{"type":"paragraph","attrs":{"id":"9c33e87b-623c-4002-ac62-49c964127af5"},"content":[{"type":"inlineTemplate","attrs":{"id":"7423fe64-2033-4931-ad0c-edfcc8b56b31","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1#日期}}"}]}]},{"type":"paragraph","attrs":{"id":"1df35a70-dfbf-42ec-b802-4a15165413d8"},"content":[{"type":"inlineTemplate","attrs":{"id":"34e535ac-e2c4-4611-9f3a-0ecfab39ddc4","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1#日 期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"73bc85ee-3644-47ef-9714-3df196b9aaf7","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"e23299ba-ecfb-4b9e-aabf-9e9244d44f3a"},"content":[{"type":"inlineTemplate","attrs":{"id":"74a74cfe-f718-4d00-a418-1c9a29294bbf","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1#日期}}"}]}]},{"type":"paragraph","attrs":{"id":"151d9a15-4713-4385-b871-72ef4ba58a89"},"content":[{"type":"inlineTemplate","attrs":{"id":"ebd39848-8feb-4d98-95eb-01b23bc797f3","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1#日期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"74bf5a27-401f-4d17-b4be-8c30753c4883","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"6c6683d9-b19a-4e4f-85f5-1704af87bfae"},"content":[{"type":"inlineTemplate","attrs":{"id":"9c6efa4c-4ba7-4ee5-aaf5-d81b28f0ddd4","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL4#原始数据#$1# 日期}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"461ca64d-b222-477d-8c9e-4b8ce7e0992e"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"日期":"2020-01-01","经办机构":"青岛银行","客户名称":"北京融汇","客户关系":"良好"," 是否拟上市":"是","专业贷款分类":"企业 / 机构贷款","是否投资级":"是","业务发生方式":"线上","贷款投向":"企业贷款","主担保方式":"抵押担保","附加担保方式":"自然人连带责任担保","终审评级":"优质级","是否为当前有效评级":"是","系统测算限额情况":"历史逾期记录、行业周期性波动、政策监管限制","超限额申请的原因":"客户端实际经营 / 消费需求驱动"},{"日期":"2021-01-02","经办机构":"河北银行","客户名称":"客户q","客户关系":"188.88","是否 拟上市":"88.88","专业贷款分类":"120.33","是否投资级":"130.88","业务发生方式":"66.66","贷款投向":"55.55","主担保方式":"40.40","附加担保方式":"25.25","终审评级":"60.60","是否为当前有效评级":"72.72","系统测算限额情况":"82.82","超限额申请的原因":"96.96"},{"日期":"2021-01-02","经办机构":"河北银行","客户名称":"客户q","客户关系":"188.88","是否拟上市":"88.88","专业贷款分类":"120.33","是否投资级":"130.88","业务发生方式":"66.66","贷款投向":"55.55","主担保方式":"40.40","附加担保方式":"25.25","终审评级":"60.60","是否为当前有效评级":"72.72","系统测算限额情况":"82.82","超限额申请的原因":"96.96"},{"日期":"2021-01-02","经办机构":"河北银行","客户名称":"客户q","客户关系":"188.88","是否拟上市":"88.88","专业贷款分类":"120.33","是否投资级":"130.88","业务发生方式":"66.66","贷款投向":"55.55","主担保方式":"40.40","附加担保方式":"25.25","终审评级":"60.60","是否为当前有效评级":"72.72","系统测算限额情况":"82.82","超限额申请的原因":"96.96"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"cb622b53-7525-4844-9a59-120bfd6c6bcd","node_type":"sql_etl","is_output":true,"source_node_id":"cb622b53-7525-4844-9a59-120bfd6c6bcd","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2307333"},"图表#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"type":"chart_bar","title":"","xAxisTitle":"","yAxisTitle":"","colorMatch":"default","colors":["#5470c6","#91cc75","#fac858","#ee6666","#73c0de"],"showTitle":true,"showGrid":true,"showLegend":true,"showAxisLabel":true,"showAxis":true,"showDataLabel":false,"legend":["value"],"valueList":[{"name":"贵州茅台","value":[320.0]},{"name":"五粮液","value":[280.0]},{"name":"泸州老窖","value":[150.0]},{"name":"剑南春","value":[130.0]},{"name":"古井贡酒","value":[110.0]},{"name":"洋河股份","value":[160.0]},{"name":"汾酒","value":[95.0]},{"name":"郎酒","value":[88.0]},{"name":"西凤酒","value":[72.0]}]},"options":[],"password":false,"name":"图表#输出","display_name":"图表#输出","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"555522fb-a9e8-4453-a1d0-6f675e168aa6","node_type":"chart_setup","is_output":true,"source_node_id":"555522fb-a9e8-4453-a1d0-6f675e168aa6","source_node_type":"chart_setup","source_node_handle":"output","source_node_name":"图表","source_workflow_run_result_id":"2307343"},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"“北京融汇金信”可能指代一家位于北京的金融或信息服务类公司,但目前公开信息中没有权威、广泛认知的知 名企业或机构以“北京融汇金信”为全称。该名称可能属于以下几种情况:\\n\\n1. **小型或区域性公司**:可能是某家注册在北京、从事金融咨询、投资管理、信用服务、金融科技等领域的中小型公司。\\n\\n2. **名称相似或误写**:有可能是“融汇金信”、“融汇金服”、“融汇财富”、“金信融汇”等类似名称公司的误记或简称。\\n\\n3. **未备案或非正规机构**:需警惕是否存在虚假或非法金融活动。部分打着“金融信息服务”旗号的公司可能涉及非法集资、网络诈骗等风 险。\\n\\n⚠️ **重要提醒**:\\n- 若您是在寻找投资理财、贷款、征信服务等,建议通过国家企业信用信息公示系统(http://www.gsxt.gov.cn)查询该公司是否合法注册。\\n- 查看其经营范围是否包含金融相关业务(如“金融信息服务”、“资产管理”、“投资咨询”等),并确认是否有相关金融牌照。\\n- 警惕高收益承诺、快速返利、资金池运作等非法集资特征。\\n\\n✅ 建议操作:\\n1. 登录“国家企业信用信息公示系统”,输入“北京融汇金信”查询工商注册信息。\\n2. 通过天眼查、企查查等第三方平台查看企业背景、股东结构、法律诉讼、经营异常等情况。\\n3. 如涉及资金往来,请务必核实对方资质,避免财产损失。\\n\\n若您能提供更多上下文(如具体业务、联系方式、官网等),我可以进一步帮助判断其真实性 与合规性。\\n\\n📌 温馨提示:任何金融活动请优先选择银行、持牌金融机构或经过监管备案的平台,谨防诈骗!","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"node_id":"fcca59e8-a938-4e62-b4db-22ee76919b17","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"fcca59e8-a938-4e62-b4db-22ee76919b17","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2307337"},"普通提示2#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"授信方案是银行/机构据客户资质与需求,核定授信额度、期限、条件等,供融资支持并控险增效的整体安排。","options":[],"password":false,"name":"普通提示2#输出","display_name":"普通提示2#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"node_id":"3127f22e-db8f-42d7-a46d-6b7a6dec9cac","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"3127f22e-db8f-42d7-a46d-6b7a6dec9cac","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示2","source_workflow_run_result_id":"2307336"},"QDoc#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"","options":[],"password":false,"name":"QDoc#答案","display_name":"QDoc#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":105,"node_id":"7269eeae-8859-4c63-bd14-2d048df20566","node_type":"q_doc","tooltip":"输出为文本","is_output":true,"source_node_id":"7269eeae-8859-4c63-bd14-2d048df20566","source_node_type":"q_doc","source_node_handle":"output_content","source_node_name":"QDoc","source_workflow_run_result_id":"2307331"},"QCheck#合同类型":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[],"options":[],"password":false,"name":"QCheck#合同类型","display_name":"QCheck#合同类型","type":"qcheck_result","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":106,"node_id":"42c5d8d2-a1e0-46c9-b852-1ec48344782a","node_type":"q_check","tooltip":"","is_output":true,"source_node_id":"42c5d8d2-a1e0-46c9-b852-1ec48344782a","source_node_type":"q_check","source_node_handle":"output_合同类型","source_node_name":"QCheck","source_workflow_run_result_id":"2307341"},"QCheck#合同编号":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"content":"CCL2023SZPS0090","knowledgeId":"463f731acc6912ee6c0a379151e58914","knowledgeIndex":0,"name":"合同编号","type":"知识组装"}],"options":[],"password":false,"name":"QCheck#合同编 号","display_name":"QCheck#合同编号","type":"qcheck_result","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":107,"node_id":"42c5d8d2-a1e0-46c9-b852-1ec48344782a","node_type":"q_check","tooltip":"","is_output":true,"source_node_id":"42c5d8d2-a1e0-46c9-b852-1ec48344782a","source_node_type":"q_check","source_node_handle":"output_合同编号","source_node_name":"QCheck","source_workflow_run_result_id":"2307341"},"ETL4#原始数据":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"","options":[],"password":false,"name":"ETL4#原始数据","display_name":"ETL4#原始数据","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":108,"node_id":"27e98211-22c2-4999-87e5-83c870a4b65f","node_type":"sql_etl"},"if判断#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000万"," 企业规模":"6000人","所属行业":"软件","经营范围":"软件服务,软件开发"}],"options":[],"password":false,"name":"if判断#原始数据","display_name":"if判断#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":109,"node_id":"7b2200f5-6602-4901-8e91-c070b81ec3e7","node_type":"sql_etl","is_output":true,"source_node_id":"7b2200f5-6602-4901-8e91-c070b81ec3e7","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"if判断","source_workflow_run_result_id":"2307332"},"代码#原始数据":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"提示":"北京融汇金信","hello_key":"hellowoed","合同":"[{\\"uid\\":\\"vc-upload-1765268976579-32\\",\\"statusName\\":\\"正在抽取\\",\\"originName\\":\\"惠州26ˉ6油田开发项目海管涂敷合同.pdf\\",\\"filePath\\":\\"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf\\",\\"hashValue\\":\\"9c270c20e602b281d708e430e641e249\\",\\"taskId\\":\\"363096\\",\\"meta\\":{\\"taskId\\":\\"363096\\",\\"docId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\"},\\"evidenceId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\",\\"lastModified\\":1765273577735,\\"lastModifiedDate\\":\\"2025-12-09T09:46:17.735Z\\",\\"name\\":\\"惠州26ˉ6油田开发项目海管涂敷合同.pdf\\",\\"size\\":1657044,\\"type\\":\\"application/pdf\\",\\"percent\\":100,\\"originFileObj\\":{\\"uid\\":\\"vc-upload-1765268976579-32\\",\\"statusName\\":\\"正在抽取\\",\\"originName\\":\\"惠州26ˉ6油田开发项目海管涂敷合同.pdf\\",\\"filePath\\":\\"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf\\",\\"hashValue\\":\\"9c270c20e602b281d708e430e641e249\\",\\"taskId\\":\\"363096\\",\\"meta\\":{\\"taskId\\":\\"363096\\",\\"docId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\"},\\"evidenceId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\"},\\"status\\":\\"done\\",\\"xhr\\":{\\"uid\\":\\"vc-upload-1765268976579-32\\",\\"statusName\\":\\"正在抽取\\",\\"originName\\":\\"惠州26ˉ6油田开发项目海管涂敷合同.pdf\\",\\"filePath\\":\\"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf\\",\\"hashValue\\":\\"9c270c20e602b281d708e430e641e249\\",\\"taskId\\":\\"363096\\",\\"meta\\":{\\"taskId\\":\\"363096\\",\\"docId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\"},\\"evidenceId\\":\\"6c70f62df9b9c506ea7f4e0fadc19543\\"}}]"},"options":[],"password":false,"name":"代码#原始数据","display_name":"代码#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":110,"node_id":"fb0f701e-e43c-49ad-a432-015b737be059","node_type":"code_query","is_output":true,"source_node_id":"fb0f701e-e43c-49ad-a432-015b737be059","source_node_type":"code_query","source_node_handle":"output_standard_chart","source_node_name":"代码","source_workflow_run_result_id":"2307342"},"合同":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"uid":"vc-upload-1765268976579-32","statusName":"正在抽取","originName":"惠州26ˉ6油田开发项目海管涂敷合同.pdf","filePath":"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf","hashValue":"9c270c20e602b281d708e430e641e249","taskId":"363096","meta":{"taskId":"363096","docId":"6c70f62df9b9c506ea7f4e0fadc19543"},"evidenceId":"6c70f62df9b9c506ea7f4e0fadc19543","lastModified":1765273577735,"lastModifiedDate":"2025-12-09T09:46:17.735Z","name":"惠州26ˉ6油田开发项目海管涂敷合同.pdf","size":1657044,"type":"application/pdf","percent":100,"originFileObj":{"uid":"vc-upload-1765268976579-32","statusName":"正在抽取","originName":"惠州26ˉ6油田开发项目海管涂敷合同.pdf","filePath":"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf","hashValue":"9c270c20e602b281d708e430e641e249","taskId":"363096","meta":{"taskId":"363096","docId":"6c70f62df9b9c506ea7f4e0fadc19543"},"evidenceId":"6c70f62df9b9c506ea7f4e0fadc19543"},"status":"done","xhr":{"uid":"vc-upload-1765268976579-32","statusName":"正在抽取","originName":"惠州26ˉ6油田开发项目海管涂敷合同.pdf","filePath":"group1/M00/AE/68/CgAAcGk39f-AV7t3ABlI1IhNhCI342.pdf","hashValue":"9c270c20e602b281d708e430e641e249","taskId":"363096","meta":{"taskId":"363096","docId":"6c70f62df9b9c506ea7f4e0fadc19543"},"evidenceId":"6c70f62df9b9c506ea7f4e0fadc19543"}}],"options":[],"password":false,"name":"合同","display_name":"合同","type":"extractFiles","clear_after_run":false,"list":false,"field_type":"extract","is_added":true,"display_index":106,"is_start":true,"meta":{"appId":998,"appTaskName":"合同","appFileTypeId":4231,"appFileType":"合同","applicationId":1039}},"提示":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"北京融汇金信","options":[],"password":false,"name":"提示","display_name":"提示","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}}
                """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process_CustomTable_VerifyEffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"9cb58e54-3f21-4efe-b1a8-042a1dbc9634"}},{"type":"table","attrs":{"id":"7a0f58f0-b357-4870-a93d-ea5d3a0e47fb","width":557,"columnWidths":[557]},"content":[{"type":"tableRow","attrs":{"id":"2f193d7b-6779-4f4c-b996-d5264b880c4d"},"content":[{"type":"tableCell","attrs":{"id":"1680479a-33ed-427e-9c1b-ce1f0792902a","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"fcb7e8f9-1487-41c3-a86e-a1efefd56508"},"content":[{"type":"inlineTemplate","attrs":{"id":"d5c94575-15d3-4417-a7f5-ca296b991824","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{QAgent#答案}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"5c2cdc56-0f38-4c6f-895c-42a535643781"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"QAgent#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"### 八、发展前景预测\\n\\n#### (一) 基于财务分析的判断\\n\\n1. 经营性资产支撑能力分析\\n\\n合并经营性资产表 单位:万元\\n| 报告期 | 应收票据及应收账款 | 存货 | 固定资产 | 无形资产 | 经营性资产合计 |\\n|--------|---------------------|------|----------|----------|------------------|\\n| 202212 | 3,299.61            | 4,604.49 | 2,608.30 | 1,690.89 | 12,203.29        |\\n| 202312 | 3,840.67            | 4,733.93 | 3,093.80 | 1,845.77 | 13,514.17        |\\n| 202412 | 4,251.75            | 6,333.92 | 3,352.89 | 1,700.90 | 15,639.46        |\\n| 202509 | 5,253.86            | 4,804.66 | 4,311.98 | 2,165.62 | 16,536.12        |\\n\\n母公司经营性资产表 单位:万元\\n| 报告期 |  应收票据及应收账款 | 存货 | 固定资产 | 无形资产 | 经营性资产合计 |\\n|--------|---------------------|------|----------|----------|------------------|\\n| 202212 | 缺少数据            | 缺少数据 | 1.22     | 0.65     | 1.87             |\\n| 202312 | 缺少数据            | 缺少数据 | 1.30     | 0.58     | 1.88             |\\n| 202412 | 缺少数据            | 缺少数据 | 2.58     | 0.60     | 3.18             |\\n| 202509 | 缺 少数据            | 缺少数据 | 2.51     | 0.59     | 3.10             |\\n\\n分析结论:\\n合并报表经营性资产规模持续增长,202509期末达16,536.12万元,较202212增长35.50%,显示集团整体经营规模持续扩张。但母公司经营性资产 占比极低,核心业务资产主要分布在子公司。\\n\\n2. 盈利能力持续性分析\\n\\n合并盈利能力表 单位:万元\\n| 报告期 | 营业利润 | 净利润 | 营业利润率(%) | 净利率(%) |\\n|--------|----------|--------|---------------|-----------|\\n| 202212 | 347.63   | 298.10 | 10.05         | 8.62      |\\n| 202312 | 403.17   | 337.45 | 10.79         | 9.03      |\\n| 202412 | 463.94   | 387.57 | 11.34         | 9.47      |\\n| 202509 | 457.37   | 386.38 | 12.55         | 10.60     |\\n\\n母公司盈利能力表 单位:万元\\n| 报告期 | 营业利润 | 净利润 | 营业利润率(%) | 净利率(%) |\\n|--------|----------|--------|---------------|-----------|\\n| 202212 | 12.83    | 12.53  | 92.58         | 90.42     |\\n| 202312 | 17.43    | 17.33  | 169.53        | 168.47    |\\n| 202412 | 287.21   | 285.17 | 3,033.11      | 3,011.82  |\\n| 202509 | 1.43     | 1.34   | 19.76         | 18.55     |\\n\\n分析结论:\\n合并报表盈利能力保持稳定,营业利润率维持在10%-12%区间。母公司盈利波动剧烈,202412异常高收益主要来自投资收益(266.72亿元),202509回归正常水平,显示母公司盈利主要依赖对外投资而非主营业务。\\n\\n3. 资金满足度分析\\n\\n合并现金流量表 单位:万元\\n| 报告期 | 经营活动现金流 | 投资活动现金流 | 筹资活动现金流 | 现金净增加额 |\\n|--------|----------------|----------------|----------------|--------------|\\n| 202212 | 346.58         | -135.10        | -108.55        | 105.82       |\\n| 202312 | 579.03         | -312.20        | -179.10        | 87.55        |\\n| 202412 | 605.12         | -879.02        | 226.98         | -47.69       |\\n| 202509 | 570.66         | 188.51         | -688.43        | 75.18        |\\n\\n母公司现金流量表 单位:万元\\n| 报告期 | 经营活动现金流 | 投资活动现金流 | 筹资活动现金流 | 现金净增加额 |\\n|--------|----------------|----------------|----------------|--------------|\\n| 202212 | 149.46         | -16.50         | -73.50         | 59.47        |\\n| 202312 | 175.16         | -4.24          | -157.14        | 13.79        |\\n| 202412 | 46.46          | -327.82        | 57.36          | -224.00      |\\n| 202509 | 195.75         | 316.30         | -334.00        | 178.05       |\\n\\n分析结论:\\n合并层面经营性现金流持续为正(年均516.85亿 元),但投资活动波动较大,202412出现大额净流出。母公司202509投资活动现金流入316.30亿元,主要来自收回投资(773.85亿元),显示母公司承担集团资本运作平台功能。\\n\\n#### (二) 战略方向分析\\n\\n1. 资源配置特征\\n\\n合并投资性资 产表 单位:万元\\n| 报告期 | 长期股权投资 | 交易性金融资产 | 其他非流动金融资产 | 投资性资产合计 |\\n|--------|--------------|----------------|--------------------|----------------|\\n| 202212 | 51.89        | 39.50          | 106.25             | 197.64         |\\n| 202312 | 49.76        | 30.69          | 77.70              | 158.15         |\\n| 202412 | 52.23        | 93.86          | 48.80              | 194.89         |\\n| 202509 | 51.24        | 42.48          | 40.12              | 133.84         |\\n\\n母公司投资性资产表 单位:万元\\n| 报告期 | 长期股权投资 | 交易性金融资产 | 其他非流动金融资产 | 投资性资产合计 |\\n|--------|--------------|----------------|--------------------|----------------|\\n| 202212 | 73.10        | 0.27           | 0.35               | 73.72          |\\n| 202312 | 75.96        | 0.30           | 0.29               | 76.55          |\\n| 202412 | 108.34       | 4.52           | 0.30               | 113.16         |\\n| 202509 | 120.57       | 1.08           | 0.26               | 121.91         |\\n\\n分析结论:\\n集团采取\\"母公司投资平台+子公司运营实体\\"架构。母公司长期股权投资占比超98%,202509达1,205.69亿元;合并报表商誉规模持续增长(202509达343.66亿元),显示通过并购扩张业务版图。\\n\\n#### (三) 风险提示\\n\\n1. 财务结构风险\\n\\n合并资本 结构表 单位:万元\\n| 报告期 | 总负债 | 总资产 | 资产负债率(%) |\\n|--------|--------|--------|---------------|\\n| 202212 | 2,706.31 | 4,225.55 | 64.05         |\\n| 202312 | 3,117.39 | 4,860.38 | 64.14         |\\n| 202412 | 3,766.84 | 6,043.52 | 62.33         |\\n| 202509 | 3,592.05 | 5,933.13 | 60.54         |\\n\\n母公司资本结构表 单位:万元\\n| 报告期 | 总负债 | 总资产 | 资产负债率(%) |\\n|--------|--------|--------|---------------|\\n| 202212 | 1,825.94 | 2,409.00 | 75.80         |\\n| 202312 | 1,952.02 | 2,574.33 | 75.82         |\\n| 202412 | 1,936.46 | 3,001.51 | 64.52         |\\n| 202509 | 2,237.28 | 2,970.58 | 75.32         |\\n\\n分析结论:\\n母公司长期保持高杠杆(资产负债率>75%),合并层面虽呈下降趋势但仍超60%。需关注母公司短期偿债压力(202509短期借款100亿元)。\\n\\n2. 盈利质量风险\\n\\n合并盈利构成表 单位:万元\\n| 报告期 | 投资收益占比(%) | 公允价值变动收益占比(%) |\\n|--------|------------------|--------------------------|\\n| 202212 | 0.60             | -0.73                   |\\n| 202312 | 1.24             | -0.61                   |\\n| 202412 | 3.53             | 3.18                    |\\n| 202509 | 3.66             | -2.84                   |\\n\\n分析结论:\\n202412投资收益异常高企(144.29亿元,占利润总额30.90%),202509公允价值变动损失达10.35亿元,显示非经常性损益波动较大,主营业务利润稳定性有待加强。","options":[],"password":false,"name":"QAgent#答案","display_name":"QAgent#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"5fad6bae-0982-4f4c-ac13-d6646a6edddb","node_type":"q_agent","tooltip":"输出为文本","is_output":true,"source_node_id":"5fad6bae-0982-4f4c-ac13-d6646a6edddb","source_node_type":"q_agent","source_node_handle":"output_content","source_node_name":"QAgent","source_workflow_run_result_id":"2338738"}}
                """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process_CustomTable_VerifyE111ffect1111112111() {
        String json = """
       {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","loop":"true","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":[184.66666666666666],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"inlineTemplate","attrs":{"id":"71c3398dc47e404ba3ab6b167cfe3eeb","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","position":"71c3398dc47e404ba3ab6b167cfe3eeb"}}],"text":"{{ETL#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":[184.66666666666666],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"inlineTemplate","attrs":{"id":"2fe07858-1586-4d4a-a75f-39657aef27ad","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL2#原始数据#$1#test_url}}"}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":[184.66666666666666],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"inlineTemplate","attrs":{"id":"22cf72cbb26b467ea2809589d037f437","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","position":"22cf72cbb26b467ea2809589d037f437"}}],"text":"{{ETL#原始数据#$1#报告期}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","node_type":"sql_etl","is_output":true,"source_node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2353968"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","node_type":"sql_etl","is_output":true,"source_node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2353967"}}
       """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void processRegularExpressions()  {
        String strValue = "{{空白文档输出}}";
        String pattern = "\\{\\{(.*?)(#(.*?)){1,}\\}\\}";
        boolean matches = strValue.matches(pattern);

        System.out.println(matches);
    }

    @Test
    void process_CustomTable_VerifyE111111ffect1111112111() {
        String json = """
                {
                	"content": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": {
                			"showLock": true,
                			"data": {
                				"type": "doc",
                				"content": [{
                					"type": "paragraph",
                					"attrs": {
                						"id": "e4a1e450-7d55-4932-bbac-8c848aa2f1e5"
                					}
                				}, {
                					"type": "conditionTemplate",
                					"attrs": {
                						"id": "5abce521-f319-4c76-aca8-9b13072038c8",
                						"backgroundColor": "#f9f0ff"
                					},
                					"content": [{
                						"type": "paragraph",
                						"attrs": {
                							"id": "b0e8e70a-607e-48c6-a096-9fbc6d71585a"
                						},
                						"content": [{
                							"type": "text",
                							"text": "<#if \\""
                						}, {
                							"type": "inlineTemplate",
                							"attrs": {
                								"id": "7340394c-5867-4006-86b5-18d8b4634410",
                								"isTemplate": "text",
                								"backgroundColor": "#e6f7ff",
                								"width": 557
                							},
                							"content": [{
                								"type": "text",
                								"text": "{{普通提示#输出}}"
                							}]
                						}, {
                							"type": "text",
                							"text": "\\"?contains(\\""
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "你好"
                						}, {
                							"type": "text",
                							"text": "\\") >"
                						}, {
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": "成功识别到"
                						}]
                					}, {
                						"type": "paragraph",
                						"attrs": {
                							"id": "b325cba0-5fc2-45b6-b04c-0b82d4c5151a"
                						},
                						"content": [{
                							"type": "text",
                							"marks": [{
                								"type": "textStyle",
                								"attrs": {
                									"fontFamily": "宋体",
                									"fontSize": "10.5",
                									"lineHeight": "1"
                								}
                							}],
                							"text": " <#else> 也支持else "
                						}, {
                							"type": "text",
                							"text": "</#if>"
                						}]
                					}]
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "467ffcec-84ef-4427-bebc-81698ec61e3b"
                					}
                				}, {
                					"type": "paragraph",
                					"attrs": {
                						"id": "270eb6b1-913b-4add-9488-95e279fbcb33"
                					}
                				}]
                			},
                			"pageMarginState": {
                				"top": 2.54,
                				"bottom": 2.54,
                				"left": 3.17,
                				"right": 3.17
                			},
                			"dpi": 96,
                			"paperSizeState": {
                				"width": 794,
                				"height": 1123,
                				"name": "A4"
                			},
                			"annotations": [],
                			"conditionArr": []
                		},
                		"password": false,
                		"name": "content",
                		"display_name": "输出内容",
                		"type": "dict",
                		"clear_after_run": true,
                		"list": false,
                		"field_type": "doc_editor",
                		"hide_show": true,
                		"hide_copy": true,
                		"display_index": 1000,
                		"conditionArr": []
                	},
                	"普通提示#输出": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": "你好呀!😊 很高兴见到你!有什么我可以帮你的吗?",
                		"options": [],
                		"password": false,
                		"name": "普通提示#输出",
                		"display_name": "普通提示#输出",
                		"type": "str",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 101,
                		"node_id": "2e497804-fee6-4fe1-ad93-7f3b692e5062",
                		"node_type": "simple_prompt",
                		"tooltip": "输出为文本",
                		"is_output": true,
                		"source_node_id": "2e497804-fee6-4fe1-ad93-7f3b692e5062",
                		"source_node_type": "simple_prompt",
                		"source_node_handle": "output",
                		"source_node_name": "普通提示",
                		"source_workflow_run_result_id": "2361154"
                	},
                	"QAgent#答案": {
                		"required": true,
                		"placeholder": "",
                		"show": true,
                		"multiline": false,
                		"value": "### 辽宁澎辉铸业有限公司,位于符合《钢铁行业规范条件》企业名单(第六批),第49序列",
                		"options": [],
                		"password": false,
                		"name": "QAgent#答案",
                		"display_name": "QAgent#答案",
                		"type": "str",
                		"clear_after_run": false,
                		"list": false,
                		"field_type": "textarea",
                		"is_added": true,
                		"display_index": 102,
                		"node_id": "535e29dc-372b-481e-8370-2c512ae142d5",
                		"node_type": "q_agent",
                		"tooltip": "输出为文本",
                		"is_output": true,
                		"source_node_id": "535e29dc-372b-481e-8370-2c512ae142d5",
                		"source_node_type": "q_agent",
                		"source_node_handle": "output_content",
                		"source_node_name": "QAgent",
                		"source_workflow_run_result_id": "2361156"
                	}
                }
                """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process1111_CustomTable_VerifyE111ffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":true,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"e4a1e450-7d55-4932-bbac-8c848aa2f1e5"}},{"type":"conditionTemplate","attrs":{"id":"5abce521-f319-4c76-aca8-9b13072038c8","backgroundColor":"#f9f0ff"},"content":[{"type":"paragraph","attrs":{"id":"b0e8e70a-607e-48c6-a096-9fbc6d71585a"},"content":[{"type":"text","text":"<#if \\""},{"type":"inlineTemplate","attrs":{"id":"3fa50936-e2bc-40da-8d44-cf92337a608b","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]},{"type":"text","text":"\\"?contains(\\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"辽宁澎辉铸业有限公司"},{"type":"text","text":"\\") >"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" "}]},{"type":"paragraph","attrs":{"id":"41953fe7-b997-45b1-92e8-5700f13abf51"},"content":[{"type":"radio","attrs":{"id":"149fbd9a-e5ee-480f-b7dd-f062f5564b7a","checked":true}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"是"}]},{"type":"paragraph","attrs":{"id":"b325cba0-5fc2-45b6-b04c-0b82d4c5151a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" <#else> 也支持else "},{"type":"text","text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"467ffcec-84ef-4427-bebc-81698ec61e3b"}},{"type":"paragraph","attrs":{"id":"270eb6b1-913b-4add-9488-95e279fbcb33"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"你好呀!😊 很高兴见到你!有什么我可以帮你的吗?","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"2e497804-fee6-4fe1-ad93-7f3b692e5062","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"2e497804-fee6-4fe1-ad93-7f3b692e5062","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2364277"},"QAgent#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"### 辽宁澎辉铸业有限公司,位于符合《钢铁行业规范条件》企业名单(第六批),第49序列","options":[],"password":false,"name":"QAgent#答案","display_name":"QAgent#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"535e29dc-372b-481e-8370-2c512ae142d5","node_type":"q_agent","tooltip":"输出为文本","is_output":true,"source_node_id":"535e29dc-372b-481e-8370-2c512ae142d5","source_node_type":"q_agent","source_node_handle":"output_content","source_node_name":"QAgent","source_workflow_run_result_id":"2364279"}}
      """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void process1111_Cust111omTable_VerifyE111ffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":true,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"e4a1e450-7d55-4932-bbac-8c848aa2f1e5"},"content":[{"type":"inlineTemplate","attrs":{"id":"dd5a7e35-658d-4152-838e-9bc10c7f7724","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{新表格#输出}}"}]}]},{"type":"conditionTemplate","attrs":{"id":"5abce521-f319-4c76-aca8-9b13072038c8","backgroundColor":"#f9f0ff"},"content":[{"type":"paragraph","attrs":{"id":"b0e8e70a-607e-48c6-a096-9fbc6d71585a"},"content":[{"type":"text","text":"<#if \\""},{"type":"inlineTemplate","attrs":{"id":"a79fe979-e620-4b95-a802-48d49435c9b8","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{开始输入#1111}}"}]},{"type":"text","text":"\\"?contains(\\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"是"},{"type":"text","text":"\\") >"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" "}]},{"type":"paragraph","attrs":{"id":"41953fe7-b997-45b1-92e8-5700f13abf51"},"content":[{"type":"inlineTemplate","attrs":{"id":"aa305bce-c6e0-496b-ae22-ed2565adefc8","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{新表格#输出}}"}]}]},{"type":"paragraph","attrs":{"id":"b325cba0-5fc2-45b6-b04c-0b82d4c5151a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" <#else> 也支持else "},{"type":"text","text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"270eb6b1-913b-4add-9488-95e279fbcb33"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"新表格#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"4cb31b0823894950b0e2d1a60f078373"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"be4a121dbece4d07ad4bc765dd1773ad"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"0b9d5c9468664fc8921d6d0467878b12"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"51ce95e8f5324cf7b13f016bb333efa1"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"c4f60615858540d2ba8f07d7e750b5d6"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"5f7c5b75f25e460dad93a0cda41aa286"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"e5489b6c5f80485b8fcd1e2db4cd9209"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"21cf682d22844b70beb95b119f24260c"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"707436e452604e3a9d48c07ed2fecb54"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","node_type":"sql_etl","is_output":true,"source_node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2364722"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","node_type":"sql_etl","is_output":true,"source_node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2364721"},"docOutput":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"4cb31b0823894950b0e2d1a60f078373"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"be4a121dbece4d07ad4bc765dd1773ad"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"0b9d5c9468664fc8921d6d0467878b12"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"51ce95e8f5324cf7b13f016bb333efa1"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"c4f60615858540d2ba8f07d7e750b5d6"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"5f7c5b75f25e460dad93a0cda41aa286"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"e5489b6c5f80485b8fcd1e2db4cd9209"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2364721","position":"21cf682d22844b70beb95b119f24260c"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364722","position":"707436e452604e3a9d48c07ed2fecb54"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"fileUrl":"http://10.0.0.22:10105/workflow-editor?workflowId=2701&workflowRunId=753637"},"options":[],"password":false,"name":"新表格#输出","display_name":"新表格#输出","type":"doc_output","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","node_type":"sub_workflow","is_output":true,"source_node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","source_node_type":"sub_workflow","source_node_handle":"output","source_node_name":"新表格","source_workflow_run_result_id":"2364718"},"1111":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"是","options":[],"password":false,"name":"1111","display_name":"1111","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}}
      """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void proces1111s1111_Cust111omTable_VerifyE111ffect1111112111() {
        String json = """
      {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":true,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"e4a1e450-7d55-4932-bbac-8c848aa2f1e5"}},{"type":"conditionTemplate","attrs":{"id":"5abce521-f319-4c76-aca8-9b13072038c8","backgroundColor":"#f9f0ff"},"content":[{"type":"paragraph","attrs":{"id":"b0e8e70a-607e-48c6-a096-9fbc6d71585a"},"content":[{"type":"text","text":"<#if \\""},{"type":"inlineTemplate","attrs":{"id":"a79fe979-e620-4b95-a802-48d49435c9b8","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{开始输入#1111}}"}]},{"type":"text","text":"\\"?contains(\\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"是"},{"type":"text","text":"\\") >"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" "}]},{"type":"paragraph","attrs":{"id":"a007e2f4-fcb2-44e5-9a08-6f9fb77bb890"},"content":[{"type":"inlineTemplate","attrs":{"id":"d3ba294b-834a-4d91-b48f-5698832298f1","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]}]},{"type":"paragraph","attrs":{"id":"4612469e-2100-4a3a-9b50-edb831a90bd0"},"content":[{"type":"inlineTemplate","attrs":{"id":"e4fc44b0-d0e5-408a-b9bf-9bd5878dd041","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{普通提示#输出}}"}]},{"type":"hardBreak","attrs":{"id":"a7a3c6d9-5c52-44ed-913a-5a14d58736c9"}},{"type":"inlineTemplate","attrs":{"id":"d9cf294e-d8f5-48fc-8efd-acd153eedb56","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]},{"type":"paragraph","attrs":{"id":"a45e0b86-37cf-4a24-8871-23d8e4d2a968"},"content":[{"type":"inlineTemplate","attrs":{"id":"531ab65d-bdcf-4ed4-8c1e-7583ac17ab7d","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{开始输入#1111}}"}]}]},{"type":"table","attrs":{"id":"f8f94d1c-41b9-432d-b141-84ef0f6b4115","width":520,"columnWidths":[260,260]},"content":[{"type":"tableRow","attrs":{"id":"cc92cf9e-9fc9-44f9-85c5-512615a0d344","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"525b81e7-d649-4749-89eb-d5d0170e2c04","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"fb056fad-3cd9-4826-ad6e-8d6ce2e05924"},"content":[{"type":"inlineTemplate","attrs":{"id":"e93194ca-f1cb-4b2b-b3a6-d909a5231f23","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"d5ae1566-dc05-4620-925e-686d5aa99796","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"43c22135-83ad-452b-ada6-897aec09abc6"},"content":[{"type":"inlineTemplate","attrs":{"id":"96210b4c-2d4d-40af-acdd-97106d3bd9e0","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#项目}}"}]}]}]}]}]},{"type":"image","attrs":{"id":"50bd16ae-c996-419c-be59-ef218c018ec7","src":"group1/M00/B0/45/CgAAcGlqAqKAF1DsAAAVMcsyfls838.png","alt":"chart-1768555168520.png","title":"chart-1768555168520.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}},{"type":"paragraph","attrs":{"id":"b325cba0-5fc2-45b6-b04c-0b82d4c5151a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" <#else> 也支持else "},{"type":"text","text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"270eb6b1-913b-4add-9488-95e279fbcb33"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"新表格#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d5d5f0b74a034ec4a1e2e57f5d629166"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"b831c32b12124ab589121e0e920aacc4"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"2e360664587e4758846f26aa7fc6ab78"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d33827843b56410b9336dff0478fb908"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"c7745420027a40e2af6a8d732bdc4151"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"389ee779cd2544999cabf9cc04529df6"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"bbcf999fbf1146f48014dda4db1360ca"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"d529d44093434041aca452ca54e95799"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d43784049a174126900bbc16c947c813"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","node_type":"sql_etl","is_output":true,"source_node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2364890"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","node_type":"sql_etl","is_output":true,"source_node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2364889"},"docOutput":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d5d5f0b74a034ec4a1e2e57f5d629166"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"b831c32b12124ab589121e0e920aacc4"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"2e360664587e4758846f26aa7fc6ab78"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d33827843b56410b9336dff0478fb908"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"c7745420027a40e2af6a8d732bdc4151"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"389ee779cd2544999cabf9cc04529df6"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"bbcf999fbf1146f48014dda4db1360ca"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2364889","position":"d529d44093434041aca452ca54e95799"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2364890","position":"d43784049a174126900bbc16c947c813"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"fileUrl":"http://10.0.0.22:10105/workflow-editor?workflowId=2701&workflowRunId=753682"},"options":[],"password":false,"name":"新表格#输出","display_name":"新表格#输出","type":"doc_output","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","node_type":"sub_workflow","is_output":true,"source_node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","source_node_type":"sub_workflow","source_node_handle":"output","source_node_name":"新表格","source_workflow_run_result_id":"2364882"},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","node_type":"sql_etl","is_output":true,"source_node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2364887"},"图表#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"type":"chart_bar","title":"","xAxisTitle":"","yAxisTitle":"","colorMatch":"default","colors":["#5470c6","#91cc75","#fac858","#ee6666","#73c0de"],"showTitle":true,"showGrid":true,"showLegend":true,"showAxisLabel":true,"showAxis":true,"showDataLabel":false,"legend":["项目"],"valueList":[{"name":"张三111111111111111111111111","value":[1.8122132234132537E19]},{"name":"李四22222222222222222222","value":[3.1324354635243123E19]},{"name":"王武22222222222222222222222222","value":[2.123431212345467E16]}]},"options":[],"password":false,"name":"图表#输出","display_name":"图表#输出","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","node_type":"chart_setup","is_output":true,"source_node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","source_node_type":"chart_setup","source_node_handle":"output","source_node_name":"图表","source_workflow_run_result_id":"2364886"},"QAgent#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"### 辽宁澎辉铸业有限公司,位于符合《钢铁行业规范条件》企业名单(第六批),第49序列","options":[],"password":false,"name":"QAgent#答案","display_name":"QAgent#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","node_type":"q_agent","tooltip":"输出为文本","is_output":true,"source_node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","source_node_type":"q_agent","source_node_handle":"output_content","source_node_name":"QAgent","source_workflow_run_result_id":"2364883"},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"你好!很高兴见到你!😊\\n\\n我是DeepSeek,一个AI助手,由深度求索公司创造。我可以帮你解答问题、进行对话、协助处理各种任务。无论你想聊天、学习、工作还是需要创意灵感,我都很乐意帮助你!\\n\\n有什么我可以为你做的吗?比如:\\n- 回答你的问题\\n- 帮你分析或解释某个概念\\n- 协助写作或创作\\n- 解决学习或工作中的难题\\n- 或者就是随便聊聊天\\n\\n请随时告诉我你需要什么帮助!✨","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":105,"node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2364885"},"1111":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"是","options":[],"password":false,"name":"1111","display_name":"1111","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}}
      """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }


    @Test
    void proces1111s1111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":true,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"e4a1e450-7d55-4932-bbac-8c848aa2f1e5"}},{"type":"conditionTemplate","attrs":{"id":"5abce521-f319-4c76-aca8-9b13072038c8","backgroundColor":"#f9f0ff"},"content":[{"type":"paragraph","attrs":{"id":"b0e8e70a-607e-48c6-a096-9fbc6d71585a"},"content":[{"type":"text","text":"<#if \\""},{"type":"inlineTemplate","attrs":{"id":"575ee291-11a2-4b87-89dd-5cad8aa33aef","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]},{"type":"text","text":"\\"?contains(\\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"辽宁"},{"type":"text","text":"\\") >"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" "}]},{"type":"paragraph","attrs":{"id":"3896b687-f8b1-4b39-93c0-7ebbaf8a1180"},"content":[{"type":"radio","attrs":{"id":"d5d14ff4-7145-4685-bf81-3211de581c8a","checked":false}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"2未封堵我是想邪恶的楚雄1"}]},{"type":"paragraph","attrs":{"id":"0d19b871-6b35-4cfb-b607-112b25a626c0"},"content":[{"type":"inlineTemplate","attrs":{"id":"0458d7c2-7836-4b1e-80d0-1cce65147ad5","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]}]},{"type":"paragraph","attrs":{"id":"c1bc6c8a-9799-471c-a432-3fedd3f2ad24"},"content":[{"type":"checkbox","attrs":{"id":"5e1c18ad-120d-488a-867c-d9e62a7ab0e1","checked":false}},{"type":"checkbox","attrs":{"id":"e30d577b-2149-4e0d-b10d-c19e35077f4d","checked":false}},{"type":"checkbox","attrs":{"id":"bcb8e54b-2c66-4a9d-b001-dc477f4cfe85","checked":false}}]},{"type":"paragraph","attrs":{"id":"6ec31fb6-5141-4a1b-b7b3-261ab1c62664"},"content":[{"type":"inlineTemplate","attrs":{"id":"4273f043-c8cd-410f-8707-71da3621aa1b","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{新表格#输出}}"}]}]},{"type":"paragraph","attrs":{"id":"0a76d408-2d4a-4495-82fa-a869207125fb"},"content":[{"type":"inlineTemplate","attrs":{"id":"7733a009-6c53-4da3-8195-2491e3fa5b7e","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]},{"type":"table","attrs":{"id":"f8f94d1c-41b9-432d-b141-84ef0f6b4115","width":520,"columnWidths":[260,260]},"content":[{"type":"tableRow","attrs":{"id":"cc92cf9e-9fc9-44f9-85c5-512615a0d344","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"525b81e7-d649-4749-89eb-d5d0170e2c04","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"fb056fad-3cd9-4826-ad6e-8d6ce2e05924"},"content":[{"type":"inlineTemplate","attrs":{"id":"e93194ca-f1cb-4b2b-b3a6-d909a5231f23","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"d5ae1566-dc05-4620-925e-686d5aa99796","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"43c22135-83ad-452b-ada6-897aec09abc6"},"content":[{"type":"inlineTemplate","attrs":{"id":"96210b4c-2d4d-40af-acdd-97106d3bd9e0","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#项目}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"6773eb0e-ae0d-48f4-bfb1-a42c8e19afe8"},"content":[{"type":"inlineTemplate","attrs":{"id":"0a217b96-e68c-41c3-b541-fc31b26d52b9","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{普通提示#输出}}"}]}]},{"type":"paragraph","attrs":{"id":"9a94c54f-67c6-4717-9387-1dea7f08f14b"},"content":[{"type":"inlineTemplate","attrs":{"id":"f2e2bc23-47ef-44a4-9d39-0c1e75f3229e","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{开始输入#1111}}"}]}]},{"type":"image","attrs":{"id":"24b2b4d4-da26-41d4-9027-f6b206b7835a","src":"group1/M00/B0/48/CgAAcGltjaWAdbjgAAAVMcsyfls794.png","alt":"chart-1768787362749.png","title":"chart-1768787362749.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}},{"type":"paragraph","attrs":{"id":"b325cba0-5fc2-45b6-b04c-0b82d4c5151a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" <#else> 也支持else "},{"type":"text","text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"270eb6b1-913b-4add-9488-95e279fbcb33"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"新表格#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"e67df97efeaa45e9b4f7660159c6d940"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"9d9c8676572046f49453703e520fe057"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"ee2c86717b5d4a68a77975715b3808e3"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"b5d11a3f4410415087ffbb0d791c771b"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"e8106ab43c254d1ca81a86d755a73e4d"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"12bb5038bddb4a8ebd3177b681a4d035"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"be180825bc5141c98877580adddd357b"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"2287ce33c8134e88a896c2fa4d119b08"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"4be5ff8864f946cfa8c1ef1598e65984"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","node_type":"sql_etl","is_output":true,"source_node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2373299"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","node_type":"sql_etl","is_output":true,"source_node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2373298"},"docOutput":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"e67df97efeaa45e9b4f7660159c6d940"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"9d9c8676572046f49453703e520fe057"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"ee2c86717b5d4a68a77975715b3808e3"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"b5d11a3f4410415087ffbb0d791c771b"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"e8106ab43c254d1ca81a86d755a73e4d"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"12bb5038bddb4a8ebd3177b681a4d035"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"be180825bc5141c98877580adddd357b"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2373298","position":"2287ce33c8134e88a896c2fa4d119b08"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2373299","position":"4be5ff8864f946cfa8c1ef1598e65984"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"fileUrl":"http://10.0.0.22:10105/workflow-editor?workflowId=2701&workflowRunId=757559"},"options":[],"password":false,"name":"新表格#输出","display_name":"新表格#输出","type":"doc_output","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","node_type":"sub_workflow","is_output":true,"source_node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","source_node_type":"sub_workflow","source_node_handle":"output","source_node_name":"新表格","source_workflow_run_result_id":"2373291"},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","node_type":"sql_etl","is_output":true,"source_node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2373296"},"图表#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"type":"chart_bar","title":"","xAxisTitle":"","yAxisTitle":"","colorMatch":"default","colors":["#5470c6","#91cc75","#fac858","#ee6666","#73c0de"],"showTitle":true,"showGrid":true,"showLegend":true,"showAxisLabel":true,"showAxis":true,"showDataLabel":false,"legend":["项目"],"valueList":[{"name":"张三111111111111111111111111","value":[1.8122132234132537E19]},{"name":"李四22222222222222222222","value":[3.1324354635243123E19]},{"name":"王武22222222222222222222222222","value":[2.123431212345467E16]}]},"options":[],"password":false,"name":"图表#输出","display_name":"图表#输出","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","node_type":"chart_setup","is_output":true,"source_node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","source_node_type":"chart_setup","source_node_handle":"output","source_node_name":"图表","source_workflow_run_result_id":"2373295"},"QAgent#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"### 辽宁澎辉铸业有限公司,位于符合《钢铁行业规范条件》企业名单(第六批),第49序列","options":[],"password":false,"name":"QAgent#答案","display_name":"QAgent#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","node_type":"q_agent","tooltip":"输出为文本","is_output":true,"source_node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","source_node_type":"q_agent","source_node_handle":"output_content","source_node_name":"QAgent","source_workflow_run_result_id":"2373292"},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"你好!很高兴见到你!😊\\n\\n我是DeepSeek,一个AI助手,由深度求索公司创造。我可以帮你解答问题、进行对话、协助处理各种任务。无论你想聊天、学习、工作还是需要创意灵感,我都很乐意帮助你!\\n\\n有什么我可以为你做的吗?比如:\\n- 回答你的问题\\n- 帮你分析或解释某个概念\\n- 协助写作或创作\\n- 解决学习或工作中的难题\\n- 或者就是随便聊聊天\\n\\n请随时告诉我你需要什么帮助!✨","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":105,"node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2373294"},"1111":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"是","options":[],"password":false,"name":"1111","display_name":"1111","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}}
     """;  List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void proces1111s1111111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":true,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"e4a1e450-7d55-4932-bbac-8c848aa2f1e5"}},{"type":"conditionTemplate","attrs":{"id":"5abce521-f319-4c76-aca8-9b13072038c8","backgroundColor":"#f9f0ff"},"content":[{"type":"paragraph","attrs":{"id":"b0e8e70a-607e-48c6-a096-9fbc6d71585a"},"content":[{"type":"text","text":"<#if \\""},{"type":"inlineTemplate","attrs":{"id":"575ee291-11a2-4b87-89dd-5cad8aa33aef","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]},{"type":"text","text":"\\"?contains(\\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"辽宁"},{"type":"text","text":"\\") >"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" "}]},{"type":"paragraph","attrs":{"id":"dcaa3013-2472-4115-8443-2a4657fa2366"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是QAgent"}]},{"type":"paragraph","attrs":{"id":"0d19b871-6b35-4cfb-b607-112b25a626c0"},"content":[{"type":"inlineTemplate","attrs":{"id":"0458d7c2-7836-4b1e-80d0-1cce65147ad5","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{QAgent#答案}}"}]}]},{"type":"paragraph","attrs":{"id":"9ee5bf3e-3883-4ae8-9ddb-92b8001d893a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是etl"}]},{"type":"paragraph","attrs":{"id":"0a76d408-2d4a-4495-82fa-a869207125fb"},"content":[{"type":"inlineTemplate","attrs":{"id":"7733a009-6c53-4da3-8195-2491e3fa5b7e","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]},{"type":"paragraph","attrs":{"id":"0b94b328-154e-4624-8af4-96767724ca20"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是大模型结果"}]},{"type":"paragraph","attrs":{"id":"6773eb0e-ae0d-48f4-bfb1-a42c8e19afe8"},"content":[{"type":"inlineTemplate","attrs":{"id":"0a217b96-e68c-41c3-b541-fc31b26d52b9","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{普通提示#输出}}"}]}]},{"type":"paragraph","attrs":{"id":"9e58b60d-5032-4087-b415-5d8d510097c7"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是开始输入"}]},{"type":"paragraph","attrs":{"id":"9a94c54f-67c6-4717-9387-1dea7f08f14b"},"content":[{"type":"inlineTemplate","attrs":{"id":"f2e2bc23-47ef-44a4-9d39-0c1e75f3229e","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{开始输入#1111}}"}]}]},{"type":"paragraph","attrs":{"id":"135fdc58-6214-4ef5-ad8a-6f5022d8019a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是单选"}]},{"type":"paragraph","attrs":{"id":"9ac94526-9a40-4ab9-8709-8740406f52fc"},"content":[{"type":"radio","attrs":{"id":"11db0516-f4e8-42ad-8489-6ddbefab4323","checked":false}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"是\\t"},{"type":"radio","attrs":{"id":"542c268f-c69a-45f4-b63b-d0348c6f6643","checked":false}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"不是"}]},{"type":"paragraph","attrs":{"id":"6450709d-1193-4381-89be-41379fe0a664"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是复选"}]},{"type":"paragraph","attrs":{"id":"3c462c82-7427-4085-99a7-1d603f175f76"},"content":[{"type":"checkbox","attrs":{"id":"5816edda-0c6b-4560-aade-4a5f17d11dd0","checked":false}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"数学"},{"type":"checkbox","attrs":{"id":"f865a9b0-3605-4713-b671-30b789918690","checked":true}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"语文"},{"type":"checkbox","attrs":{"id":"17dc3ebb-6dd9-40d1-9284-5b5b761e4ba3","checked":false}},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"英语"}]},{"type":"paragraph","attrs":{"id":"0a77eabd-8b50-4c27-8ab3-1fd81792354c"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是表格"}]},{"type":"table","attrs":{"id":"d4312887-a612-4eae-9b87-ac0fd4dbf8b4","width":520,"columnWidths":[260,260]},"content":[{"type":"tableRow","attrs":{"id":"b27a7159-b089-442f-b849-798a4bd297ee","loop":"true","startLoopIndex":0,"endLoopIndex":1},"content":[{"type":"tableCell","attrs":{"id":"15c72091-2c64-49b5-af98-2808c6d386b2","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"95bacd21-d1e0-4297-90de-312792eb0fbd"},"content":[{"type":"inlineTemplate","attrs":{"id":"0d332f97-9f5e-478e-99ed-027ac6580ed7","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#报告期}}"}]}]}]},{"type":"tableCell","attrs":{"id":"1bbc3ccd-d629-4b6e-b10c-201ec67c7d70","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"d7f1c3bb-e3aa-4731-97e2-966f413983c9"},"content":[{"type":"inlineTemplate","attrs":{"id":"95f9ccaf-a998-4242-ad6f-975c50a7dc08","isTemplate":"text","backgroundColor":"#e6f7ff","width":243},"content":[{"type":"text","text":"{{ETL#原始数据#$1#项目}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"b325cba0-5fc2-45b6-b04c-0b82d4c5151a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"下面是图表"}]},{"type":"image","attrs":{"id":"162d9e54-f33b-4d2f-abe2-138a05c1df32","src":"group1/M00/B0/50/CgAAcGlt3CSAUU1QAAAVMcsyfls261.png","alt":"chart-1768807458420.png","title":"chart-1768807458420.png","width":320,"height":180,"chartValue":"{{图表#输出}}"}},{"type":"paragraph","attrs":{"id":"56202328-91e2-4cb5-9d46-9e9ea398fd00"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" 下面是子工作流"}]},{"type":"paragraph","attrs":{"id":"b4a0fc32-4e7f-4bce-a5d0-13ce7d822dc8"},"content":[{"type":"inlineTemplate","attrs":{"id":"b23f088c-74bc-4a5b-bc4a-2c2c38629515","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{新表格#输出}}"}]}]},{"type":"paragraph","attrs":{"id":"2593c844-81e2-4c5a-b410-cf9788a38b72"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"<#else> 也支持else "},{"type":"text","text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"270eb6b1-913b-4add-9488-95e279fbcb33"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"新表格#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"4ad17a6c7006438283339006330bedcc"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"4e11848584384f1295f374da82ce25c7"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"44c750e4d319499eaaebc52ff3b7c527"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"98ff6cd0314245d9a9a01264fc6f2dc2"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"cf1beea4586d46daa14cb14236add63d"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"adb7c8524db84d9e90255ef2bd90a991"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"8336040dcdcd4973acec049c7ffb1f39"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"a2dd14b75dee41cfbfd65ed6d11b82ed"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"f35450a71c3143139e3f27ccdb0843a0"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","node_type":"sql_etl","is_output":true,"source_node_id":"0988164c-9bd8-4061-9e8c-21ff06b38de6","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2374053"},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"test_id":"TEST-001","author_name":"张三","author_email":"zhangsan@example.com","test_datetime":"2024-01-15 10:30:00","editor_name":"Markdown 渲染引擎 v2.0","editor_version":"2.0.1","test_url":"https://example.com/test","github_url":"https://github.com/test/repo","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image","test_status":"通过","test_score":"95.50","test_result":true,"is_completed":true,"markdown_flavor":"GitHub Flavored Markdown","dynamic_content":"这是一个动态插入的**粗体**文本内容","alert_title":"重要提示","alert_message":"这是通过ETL动态生成的提示信息","code_language":"Python","code_snippet":"print(\\"Hello from ETL!\\")","feature_name":"功能A","feature_status":"支持","feature_coverage":"100%"},{"test_id":"TEST-002","author_name":"李四","author_email":"lisi@example.com","test_datetime":"2024-01-16 14:20:00","editor_name":"Markdown 渲染引擎 v2.1","editor_version":"2.1.0","test_url":"https://example.com/test2","github_url":"https://github.com/test/repo2","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+2","test_status":"失败","test_score":"78.30","test_result":false,"is_completed":false,"markdown_flavor":"CommonMark","dynamic_content":"这是第二条动态内容,包含_斜体_文本","alert_title":"警告","alert_message":"这是第二条动态提示信息","code_language":"JavaScript","code_snippet":"console.log(\\"Hello from ETL!\\");","feature_name":"功能B","feature_status":"部分支持","feature_coverage":"75%"},{"test_id":"TEST-003","author_name":"王五","author_email":"wangwu@example.com","test_datetime":"2024-01-17 09:15:00","editor_name":"Markdown 渲染引擎 v2.2","editor_version":"2.2.0","test_url":"https://example.com/test3","github_url":"https://github.com/test/repo3","image_url":"https://via.placeholder.com/600x300.png?text=Test+Image+3","test_status":"进行中","test_score":"88.70","test_result":false,"is_completed":false,"markdown_flavor":"MultiMarkdown","dynamic_content":"这是第三条动态内容,包含~~删除线~~文本","alert_title":"信息","alert_message":"这是第三条动态提示信息","code_language":"Java","code_snippet":"System.out.println(\\"Hello from ETL!\\");","feature_name":"功能C","feature_status":"支持","feature_coverage":"95%"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","node_type":"sql_etl","is_output":true,"source_node_id":"d1c80ff3-990e-4093-8b6c-2c2fd4334108","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2374052"},"docOutput":{"showLock":false,"data":{"type":"doc","content":[{"type":"table","attrs":{"id":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","position":"faa16735-dfa2-4fa8-d6f2-5d855ecefb00","width":555,"columnWidths":[184.66666666666666,184.66666666666666,184.66666666666666]},"content":[{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"4ad17a6c7006438283339006330bedcc"}}],"text":"张三111111111111111111111111"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"4e11848584384f1295f374da82ce25c7"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"44c750e4d319499eaaebc52ff3b7c527"}}],"text":"张三111111111111111111111111"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"98ff6cd0314245d9a9a01264fc6f2dc2"}}],"text":"李四22222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test2","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"cf1beea4586d46daa14cb14236add63d"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"adb7c8524db84d9e90255ef2bd90a991"}}],"text":"李四22222222222222222222"}]}]}]},{"type":"tableRow","attrs":{"id":"1f679f84-66a3-47b9-9400-d0fa23fd085d","startLoopIndex":0,"endLoopIndex":2},"content":[{"type":"tableCell","attrs":{"id":"68a4d15a-deee-44e4-9182-fd5123410555","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"cb441e5f-258b-4039-b1c2-c1a8a7972761","position":"71c3398dc47e404ba3ab6b167cfe3eeb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"8336040dcdcd4973acec049c7ffb1f39"}}],"text":"王武22222222222222222222222222"}]}]},{"type":"tableCell","attrs":{"id":"a2e8f0d7-bc59-4b93-93e4-ceaeab448ed9","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"ce708aff-0ce3-4b6d-9ccc-95a2f87d16a4","position":"d834d392959740b6a40d1d39b5b3b0e8"},"content":[{"type":"text","text":"https://example.com/test3","marks":[{"type":"textStyle","attrs":{"sourceId":"2374052","position":"a2dd14b75dee41cfbfd65ed6d11b82ed"}}]}]}]},{"type":"tableCell","attrs":{"id":"50219536-9f4f-4597-8995-9decaee84b74","colspan":1,"rowspan":1,"colwidth":["184.66666666666666"],"markInsert":"false","inLoopRange":true},"content":[{"type":"paragraph","attrs":{"id":"07c93966-1239-494d-888b-c345ce4aca97","position":"22cf72cbb26b467ea2809589d037f437"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"#000000","fontFamily":"宋体","fontSize":"10.5","lineHeight":"1.25","sourceId":"2374053","position":"f35450a71c3143139e3f27ccdb0843a0"}}],"text":"王武22222222222222222222222222"}]}]}]}]},{"type":"paragraph","attrs":{"id":"0aaf1bd7-1371-4789-9ef9-42b30e4822f6"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"fileUrl":"http://10.0.0.22:10105/workflow-editor?workflowId=2701&workflowRunId=757871"},"options":[],"password":false,"name":"新表格#输出","display_name":"新表格#输出","type":"doc_output","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","node_type":"sub_workflow","is_output":true,"source_node_id":"96bcc8a0-7597-482a-b6b7-9af6f1757c57","source_node_type":"sub_workflow","source_node_handle":"output","source_node_name":"新表格","source_workflow_run_result_id":"2374045"},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","node_type":"sql_etl","is_output":true,"source_node_id":"b06fcd19-3582-49cf-a32a-3e0751ac1119","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2374050"},"图表#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"type":"chart_bar","title":"","xAxisTitle":"","yAxisTitle":"","colorMatch":"default","colors":["#5470c6","#91cc75","#fac858","#ee6666","#73c0de"],"showTitle":true,"showGrid":true,"showLegend":true,"showAxisLabel":true,"showAxis":true,"showDataLabel":false,"legend":["项目"],"valueList":[{"name":"张三111111111111111111111111","value":[1.8122132234132537E19]},{"name":"李四22222222222222222222","value":[3.1324354635243123E19]},{"name":"王武22222222222222222222222222","value":[2.123431212345467E16]}]},"options":[],"password":false,"name":"图表#输出","display_name":"图表#输出","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","node_type":"chart_setup","is_output":true,"source_node_id":"7feb31e1-3ba2-45f7-a437-39013b2068bb","source_node_type":"chart_setup","source_node_handle":"output","source_node_name":"图表","source_workflow_run_result_id":"2374049"},"QAgent#答案":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"### 匹配结果:辽宁澎辉铸业有限公司,位于《符合《钢铁行业规范条件》企业名单(第六批)》文件,第49序列。","options":[],"password":false,"name":"QAgent#答案","display_name":"QAgent#答案","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","node_type":"q_agent","tooltip":"输出为文本","is_output":true,"source_node_id":"12a08d40-02a7-4e1a-b786-b1cfeb54d3d8","source_node_type":"q_agent","source_node_handle":"output_content","source_node_name":"QAgent","source_workflow_run_result_id":"2374046"},"普通提示#输出":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"你好!很高兴见到你!😊\\n\\n我是DeepSeek,一个AI助手,由深度求索公司创造。我可以帮你解答问题、进行对话、协助处理各种任务。无论你想聊天、学习、工作还是需要创意灵感,我都很乐意帮助你!\\n\\n有什么我可以为你做的吗?比如:\\n- 回答你的问题\\n- 帮你分析或解释某个概念\\n- 协助写作或创作\\n- 解决学习或工作中的难题\\n- 或者就是随便聊聊天\\n\\n请随时告诉我你需要什么帮助!✨","options":[],"password":false,"name":"普通提示#输出","display_name":"普通提示#输出","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":105,"node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","node_type":"simple_prompt","tooltip":"输出为文本","is_output":true,"source_node_id":"af9889ce-d630-485b-8aa2-5dcffdcd6e66","source_node_type":"simple_prompt","source_node_handle":"output","source_node_name":"普通提示","source_workflow_run_result_id":"2374048"},"1111":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"是","options":[],"password":false,"name":"1111","display_name":"1111","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true}}
     """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void proces1111111s1111111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"6b129063-2724-4f28-b8fa-6f2f575ab245"}},{"type":"table","attrs":{"id":"1a72b47f-6ef5-4972-a512-c4e414af6faa","width":557,"columnWidths":[61,61,61,61,61,61,61,61,61]},"content":[{"type":"tableRow","attrs":{"id":"0a7af76e-acf2-46a3-82fd-0e6b5513abaf"},"content":[{"type":"tableCell","attrs":{"id":"e1dd5436-dc7b-4ce2-8052-9cf6987205fc","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"5c52dd91-8399-40bf-9f64-232ec46bd136"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"行名"}]}]},{"type":"tableCell","attrs":{"id":"80024544-43e8-4b50-9ff5-0cec2a3b3ed0","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"5e6b60b4-2a02-446c-8582-8c2204de3f74"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"22"}]}]},{"type":"tableCell","attrs":{"id":"08dad5b2-ab18-4634-8df8-cd4fcf0c97e2","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"618aac14-f1c7-4163-84bf-92b8108beea0"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"23"}]}]},{"type":"tableCell","attrs":{"id":"4f0624ff-3b7c-4254-bcac-6fb27cee9cec","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"f2f7e167-4d4c-44eb-9c3b-abc7c4f1cea4"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"增加"}]}]},{"type":"tableCell","attrs":{"id":"8e17cf89-789c-44c5-9164-4959c0e74bd6","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"681efca6-0167-410a-a434-85f7d21cc6f2"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"24"}]}]},{"type":"tableCell","attrs":{"id":"81a7d683-91af-4c12-8a2a-bbae9e1d39dc","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"96f17b43-9595-4dcb-937c-d9c00ab43fff"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"增加"}]}]},{"type":"tableCell","attrs":{"id":"4f451623-6df6-4f2e-a940-d86116c5905f","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"511b24e9-d2ad-45c8-a3fd-7e158e9f2663"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"24"}]}]},{"type":"tableCell","attrs":{"id":"70f42db5-d88c-448a-a9eb-0939e3b86612","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"92eac824-f8a1-4032-a358-a12de6e0d667"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"25"}]}]},{"type":"tableCell","attrs":{"id":"79e40372-6f64-4a4a-b800-425e9e8b578b","colspan":1,"rowspan":1},"content":[{"type":"paragraph","attrs":{"id":"b630d3ac-7679-4f7b-8832-8bfd5bec8aec"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"增加"}]}]}]},{"type":"tableRow","attrs":{"id":"b472ab02-1e53-46af-9409-8422f264cdee","loop":"true","startLoopIndex":0,"endLoopIndex":8},"content":[{"type":"tableCell","attrs":{"id":"942462c9-06e6-4c7b-b450-ebccdc1216a1","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"aa0a53c1-3525-4542-bfc8-1c498129e628"},"content":[{"type":"inlineTemplate","attrs":{"id":"05fbe770-bc96-462e-85ce-2f2ddf699b48","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#行名}}"}]}]}]},{"type":"tableCell","attrs":{"id":"a2a9139e-e075-4660-8d44-c73f0fce9626","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"a44176bd-debb-42f5-8425-17e20d56da37"},"content":[{"type":"inlineTemplate","attrs":{"id":"0a548e59-a54a-4cd5-8294-94731d49e3bd","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#2022-12}}"}]}]}]},{"type":"tableCell","attrs":{"id":"cc95c69d-f29e-4ac2-a732-059d3abb7544","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"28e8f467-a9cd-437b-a1f9-a50bbf619765"},"content":[{"type":"inlineTemplate","attrs":{"id":"eb72475f-b074-44ef-a771-787f2d16a060","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#2023-12}}"}]}]}]},{"type":"tableCell","attrs":{"id":"391b77e8-df86-4300-b765-2d4b34a2b9ce","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"38a69ab8-2288-47b8-bd29-1eaa40f20242"},"content":[{"type":"inlineTemplate","attrs":{"id":"ed382cff-7f3f-4f36-80c7-42dca8080284","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#增长率}}"}]}]}]},{"type":"tableCell","attrs":{"id":"0f7f9e7c-0e1f-4f34-8061-62c91e9ed333","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"3a9bc3b2-6d97-4782-bfc1-8719d0faefa5"},"content":[{"type":"inlineTemplate","attrs":{"id":"9ed64675-7008-4647-93bb-277940be3c19","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#2024-12}}"}]}]}]},{"type":"tableCell","attrs":{"id":"0ef27ae8-08a2-4076-8951-db883aa3e4b2","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"4483309e-5d24-49f5-9916-77428f681aed"},"content":[{"type":"inlineTemplate","attrs":{"id":"c9a793c5-dcea-430f-a6de-30c62ea19d61","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#增长率_2}}"}]}]}]},{"type":"tableCell","attrs":{"id":"d38e0920-c105-44e2-a71a-a2cb20db6078","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"cc7aa4f8-0e48-4ab3-91b0-b9fc674e7909"},"content":[{"type":"inlineTemplate","attrs":{"id":"665f3e80-34cf-42a4-b431-0f78a684a7c9","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#2024-11}}"}]}]}]},{"type":"tableCell","attrs":{"id":"835a0c33-a133-4b56-887b-28b9a87cf4fe","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"34378acb-a3c1-456d-baf2-719abd1ed639"},"content":[{"type":"inlineTemplate","attrs":{"id":"60aa51d2-e96b-4d40-8d15-fd49c6d39ffc","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#2025-11}}"}]}]}]},{"type":"tableCell","attrs":{"id":"fcc705c5-9806-4394-9321-99c69fc969ac","colspan":1,"rowspan":1,"inLoopRange":"true"},"content":[{"type":"paragraph","attrs":{"id":"63ee75fe-a2f7-445a-b0b9-574b15281861"},"content":[{"type":"inlineTemplate","attrs":{"id":"df519551-8367-4a3f-ad81-520d2c11a351","isTemplate":"text","backgroundColor":"#e6f7ff"},"content":[{"type":"text","text":"{{ETL#原始数据#$1#增长率_3}}"}]}]}]}]}]},{"type":"paragraph","attrs":{"id":"8d5a0bfe-0dcb-4724-8474-665e355d620c"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"增长率_2":"-4.07%","增长率":"13.93%","增长率_3":"-9.03%","行名":"营业收入","2023-12":"6,752,477","2024-12":"6,477,452","2024-11":"6,037,722","2025-11":"5,492,399","2022-12":"5,927,043"},{"增长率_2":"-4.87%","增长率":"13.76%","增长率_3":"-12.35%","行名":"营业成本","2023-12":"6,268,056","2024-12":"5,963,110","2024-11":"5,567,351","2025-11":"4,879,629","2022-12":"5,509,756"},{"增长率_2":"-12.56%","增长率":"11.64%","增长率_3":"-13.21%","行名":"销售费用","2023-12":"86,604","2024-12":"75,725","2024-11":"71,900","2025-11":"62,404","2022-12":"77,576"},{"增长率_2":"1.39%","增长率":"-1.63%","增长率_3":"15.13%","行名":"管理费用","2023-12":"244,252","2024-12":"247,647","2024-11":"223,296","2025-11":"257,086","2022-12":"248,310"},{"增长率_2":"9.77%","增长率":"2.85%","增长率_3":"42.27%","行名":"营业利润","2023-12":"99,175","2024-12":"108,867","2024-11":"97,659","2025-11":"138,943","2022-12":"96,429"},{"增长率_2":"36.32%","增长率":"80.20%","增长率_3":"-68.12%","行名":"营业外收入","2023-12":"21,696","2024-12":"29,577","2024-11":"28,498","2025-11":"9,086","2022-12":"12,040"},{"增长率_2":"329.60%","增长率":"-60.95%","增长率_3":"-86.33%","行名":"营业 外支出","2023-12":"4,933","2024-12":"21,194","2024-11":"20,640","2025-11":"2,821","2022-12":"12,633"},{"增长率_2":"1.13%","增长率":"20.98%","增长率_3":"37.62%","行名":"利润总额","2023-12":"115,938","2024-12":"117,250","2024-11":"105,517","2025-11":"145,208","2022-12":"95,837"},{"增长率_2":"1.13%","增长率":"20.98%","增长率_3":"37.62%","行名":"净利润","2023-12":"98,548","2024-12":"99,663","2024-11":"105,517","2025-11":"145,208","2022-12":"81,461"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"7caf79c0-5eb6-460d-80ac-152664342f02","node_type":"sql_etl","is_output":true,"source_node_id":"7caf79c0-5eb6-460d-80ac-152664342f02","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2470475"}}
    """;
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }


    @Test
    void proc111es1111111s1111111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
        {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"7200a523-9434-43ce-9a48-2faccf7c57a5"}},{"type":"conditionTemplate","attrs":{"id":"32601455-429b-4e6e-ab79-2272572030cb","backgroundColor":"rgb(249, 240, 255)"},"content":[{"type":"paragraph","attrs":{"id":"13eef516-fe26-4e49-b07a-86af6ce213af"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"<#if "},{"type":"inlineTemplate","attrs":{"id":"0c7bbff6-fa96-47da-815f-ab527b084488","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":" == \\""},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"0"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"\\" >"}]},{"type":"paragraph","attrs":{"id":"52471fea-3074-4171-86da-c6923fe25158"}},{"type":"paragraph","attrs":{"id":"72439f11-9196-4808-8c32-a785f444dbd6"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"0"}]},{"type":"paragraph","attrs":{"id":"ccf4ee4d-374b-4651-82d6-0ad38a8270ff"}},{"type":"paragraph","attrs":{"id":"a3bc89a9-240d-4732-b6c7-a4fbe8a09a6d"}},{"type":"paragraph","attrs":{"id":"4dce5cde-f44f-4c02-b23a-8be2df3879c6"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"<#else if  \\""},{"type":"inlineTemplate","attrs":{"id":"4f041f3c-4713-402e-ac33-a47aee2b6c87","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType1}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\" "},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"== \\"2\\" >"}]},{"type":"paragraph","attrs":{"id":"24736094-aa26-4171-9e51-a584e7d1c7ba"}},{"type":"paragraph","attrs":{"id":"f98e4b8a-a369-487b-81a9-d4f455eab516"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"1"}]},{"type":"paragraph","attrs":{"id":"e7e512ab-f6b9-4e6f-995a-364310862d2e"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"rgb(0, 0, 0)","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"</#if>"}]},{"type":"paragraph","attrs":{"id":"7ec03ae2-9217-4020-a87f-8a534e45216f"}}]},{"type":"paragraph","attrs":{"id":"53d5be0c-7b2f-4907-99b4-1b06277d9bc9"}},{"type":"conditionTemplate","attrs":{"id":"d2bd8fb2-0ade-4747-870c-d39aa2f6c812","backgroundColor":"rgb(249, 240, 255)"},"content":[{"type":"paragraph","attrs":{"id":"b98f36ce-72e0-46e6-88aa-f5890a810904"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"<#if "},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\""},{"type":"inlineTemplate","attrs":{"id":"a4b76280-2761-4615-be61-91875173aacc","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\"  == \\"0\\" ||  \\""},{"type":"inlineTemplate","attrs":{"id":"640f5510-cd85-4361-afed-b00a969041cf","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\" == \\"0\\" > "}]},{"type":"paragraph","attrs":{"id":"f091ac63-4fa9-4de4-b06d-7732c1cb4aa4"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"if开始输出入1"}]},{"type":"paragraph","attrs":{"id":"322cd8d8-544d-4a2a-b156-68261d047d51"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":" <#else if \\""},{"type":"inlineTemplate","attrs":{"id":"8c7420ae-5823-4548-a40d-884ec4169cff","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType1}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\" == \\"2\\" || \\""},{"type":"inlineTemplate","attrs":{"id":"04de74a4-ed3e-43ec-85b4-d8796329cf3e","isTemplate":"text","backgroundColor":"rgb(230, 247, 255)"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"{{开始输入#custType1}}"}],"marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"","backgroundColor":"rgb(230, 247, 255)"}}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"\\" == \\"2\\" > "}]},{"type":"paragraph","attrs":{"id":"e7af7ee0-db4e-4ef1-8b92-4a4d028c3b4c"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"else if 输入2"}]},{"type":"paragraph","attrs":{"id":"3b2c6413-2c29-4ffc-9ad9-dc0644b9c21f"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":" <#else> "}]},{"type":"paragraph","attrs":{"id":"cdd08b79-de28-4c83-86e6-8032eb918838"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"color":"","fontFamily":"宋体","fontSize":"10.5","backgroundColor":"","lineHeight":"1"}}],"text":"也支持else "}]},{"type":"paragraph","attrs":{"id":"4e62f402-40aa-4829-9bfd-8d527ec6af08"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"</#if>"}]}]},{"type":"paragraph","attrs":{"id":"aae42059-7d8f-4494-ac94-d39e65822577"}}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.18,"right":3.18},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL2#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"报告期":"张三111111111111111111111111","项目":"18122132234132536478"},{"报告期":"李四22222222222222222222","项目":"31324354635243123452"},{"报告期":"王武22222222222222222222222222","项目":"21234312123454671"}],"options":[],"password":false,"name":"ETL2#原始数据","display_name":"ETL2#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"83f8fdf7-0162-4e5a-a47e-fd7046a1e6f9","node_type":"sql_etl","is_output":true,"source_node_id":"83f8fdf7-0162-4e5a-a47e-fd7046a1e6f9","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL2","source_workflow_run_result_id":"2497487"},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000万","企业规模":"6000人","所属行业":"软件","经营范围":"软件服务,软件开发"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"node_id":"982b3911-1044-4ff4-a25a-8296e7a33795","node_type":"sql_etl","is_output":true,"source_node_id":"982b3911-1044-4ff4-a25a-8296e7a33795","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"2497486"},"姓名":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"c","options":[],"password":false,"name":"姓名","display_name":"姓名","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"is_start":true},"姓名2":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"1111","options":[],"password":false,"name":"姓名2","display_name":"姓名2","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":102,"is_start":true},"custType":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"张三","options":[],"password":false,"name":"custType","display_name":"custType","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":103,"is_start":true},"custType1":{"required":true,"placeholder":"","show":true,"multiline":false,"value":"张三","options":[],"password":false,"name":"custType1","display_name":"custType1","type":"str","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":104,"is_start":true}}
""";
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }


    @Test
    void proc111es1111111111111s1111111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
{"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"86f0fcf9-d1d5-43f4-9b2e-2562d2562445"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"企  业 名 称:"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"  "},{"type":"inlineTemplate","attrs":{"id":"0f166986-47da-47cf-8c3f-49647ea80bc0","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","marks":[{"type":"underline"}],"text":"{{ETL#原始数据#$1#企业名称}}"}],"marks":[{"type":"underline"}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"    "}]},{"type":"paragraph","attrs":{"id":"e6d350b8-c32a-4761-b451-0fdeb2e873cb"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"法定代表名称:"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "},{"type":"inlineTemplate","attrs":{"id":"3dabce83-2022-4c38-844c-1cd6509792df","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","marks":[{"type":"underline"}],"text":"{{ETL#原始数据#$1#法定代表人名称}}"}],"marks":[{"type":"underline"}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "}]},{"type":"paragraph","attrs":{"id":"194fc4b7-0706-4bf6-af28-dbf28b1cb05a"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"成 立 时  间:"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "},{"type":"inlineTemplate","attrs":{"id":"2b39847b-35f0-4741-bb26-2ac2a14783d1","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","marks":[{"type":"underline"}],"text":"{{ETL#原始数据#$1#成立时间}}"}],"marks":[{"type":"underline"}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "}]},{"type":"paragraph","attrs":{"id":"9be3cd71-6cab-4188-9901-ddf4fd7371d1"},"content":[{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}}],"text":"企  业 规 模:"},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "},{"type":"inlineTemplate","attrs":{"id":"afb10d5f-5419-4271-aff9-b300ec4b2721","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","marks":[{"type":"underline"}],"text":"{{ETL#原始数据#$1#企业规模}}"}],"marks":[{"type":"underline"}]},{"type":"text","marks":[{"type":"textStyle","attrs":{"fontFamily":"宋体","fontSize":"10.5","lineHeight":"1"}},{"type":"underline"}],"text":"   "}]}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"企业名称":"青岛xx公司","法定代表人名称":"张三","成立时间":"2020年","持续经营年限":"25","注册地址":"山东青岛","经营地址":"香港中路","注册资本":"8000万","企业规 模":"6000人","所属行业":"软件","经营范围":"软件服务,软件开发"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"cf824bec-9e9e-422c-96cf-b160eedbec76","node_type":"sql_etl","is_output":true,"source_node_id":"cf824bec-9e9e-422c-96cf-b160eedbec76","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"20231"}}
""";
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }

    @Test
    void proc111es111111111111111111s1111111_Cust111omTab11111le_VerifyE111ffect1111112111() {
        String json = """
                {"content":{"required":true,"placeholder":"","show":true,"multiline":false,"value":{"showLock":false,"data":{"type":"doc","content":[{"type":"paragraph","attrs":{"id":"491d5894-43e1-4d4e-a790-c0f0858593d9"},"content":[{"type":"inlineTemplate","attrs":{"id":"89ddf7e4-cd6d-4fa7-b1ee-7bd8ad1a599d","isTemplate":"text","backgroundColor":"#e6f7ff","width":557},"content":[{"type":"text","text":"{{ETL#原始数据#$1#result}}"}]}]}]},"pageMarginState":{"top":2.54,"bottom":2.54,"left":3.17,"right":3.17},"dpi":96,"paperSizeState":{"width":794,"height":1123,"name":"A4"},"annotations":[],"conditionArr":[]},"password":false,"name":"content","display_name":"输出内容","type":"dict","clear_after_run":true,"list":false,"field_type":"doc_editor","hide_show":true,"hide_copy":true,"display_index":1000,"conditionArr":[]},"ETL#原始数据":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"result":"1、产能置换已在上述经营合规合法性处描述。\\n\\n2、收购英钢:敬业集团在获悉英国钢铁有限公司于2019年5月22日宣布破产,正在寻求全球战略合作企业的消息后,即组织集团生产、技术、采购、销售等相关部门人员及外部顾问机构到英国三个生产基地、法国、荷兰基地进行实地考察。\\n\\n经过5个多月的考察,对英国钢铁的历史遗留问题、当地的用工制度、碳排放指标、英国脱欧带来的不确定性及工会对企业的制约等诸多因素进行深度的考虑,并制定了相应的防范措施。通过聘请麦肯锡咨询公司、年利达律师事务所、普华永道会计师事务所、伊尔姆环境资源管理咨询公司四家专业机构对英国钢铁有限公司进行现场尽调,掌握了英国钢铁有限公司财税、法律、环境等方面的详尽信息,并了解到英国、法国政府对于商业的高度支持,为企业创造了透明和自由的市场经济以及良好的投资环境,有利于境外并购的实施。该项目的实施,是敬业集团全球布局,多元化发展的重要一步,将提升敬业技术研发水平,增强敬业全球钢铁生产与加工能力,拓宽海外营销网络,且对深化两国产业发展,整合两国优势资源,开拓欧洲市场,将产生积极的推动作用。双方于2019年11月10日签订并购协议,2020年3月,敬业集团出资3000万英镑收购,由集团总经理李慧明主持工作,经过一系列去外包、降成本、控采购、以及对生产模式的改革,目前公司经营正常。其主营产品钢轨、异型材、重型型钢和高端线材,市场覆盖英国和欧洲、北非、中东等国家和地区。主要设备有英钢拥有2台291m2烧结机、2座1255m3高炉、2座1580m3高炉;3座300吨转炉;配套7条轧钢线,钢材产能450万吨。\\n\\n截至2023年末,两座1580m3高炉正常生产,一座1255m3高炉已经废弃,另外一座处于热焖炉状态;两座1580m3高炉日产量7600吨,高炉炉料结构为70%烧结矿+30%球团,综合入炉品位58.5%。3座300吨转炉,停产1座,其余2座轮流组织生产。\\n\\n3、收购广东敬业钢铁实业股份有限公司:成立于2006年12月,注册资本12.1亿元,地处广东省揭阳市空港经济区,占地面积1000余亩,在册员工约1500人,是广东省第二批获得国家工信部认可的符合《钢铁行业规范条件》企业,是揭阳市重点民营钢铁企业和纳税大户,也是广东省四大钢铁企业之一。\\n\\n公司拥有一台96m2烧结机;一座530m3高炉;3座小型石灰竖窑;一座50吨转炉;一条18机架螺纹钢轧线;一座3000m3制氧站,钢铁产能100万吨/年。广东敬业水陆交通方便,距广梅油铁路揭阳站23公里,地处相江水道,自有两个5000吨级的专业钢铁货运码头,年吞吐量超400万吨,尽可能降低库存备料,实现“精确生产”。企业是华南地区最大的优质建筑用钢材生产基地之一。\\n\\n收购原因:广东敬业股东出现分歧,部分股东拟从实体行业退出,部分股东要求分红套现,造成总体经营受影响,企业停产。2020年9月,敬业集团出资21.43亿元(全部自筹)收购广东泰都钢铁实业股份有限公司(原名称),此次收购后,有助于敬业集团开拓华南业务市场。近三年及近期主要产品产量、销量及主要财务数据如下:\\n\\n单位:吨、万元\\n\\n| 广东敬业 | 产量 | 销售量 | 总资产 | 总负债 | 营业收入 | 净利润 |\\n|---|---|---|---|---|---|---|\\n| 2024.06 | 552933 | 553158 | 257628 | 75006 | 195975 | 4689 |\\n| 2023年 | 1107251 | 1099937 | 275130 | 92543 | 385675 | 6413 |\\n| 2022年 | 981871 | 975089 | 255496 | 84562 | 406391 | 10062 |\\n| 2021年 | 680098 | 681307 | 246843 | 85971 | 317961 | 9953 |\\n\\n截至2024年6月末,广东敬业钢铁在九江银行有流贷余额1亿元;银承敞口0.5亿元。\\n\\n4、收购广东粤北联合钢铁有限公司:成立于2010年7月8日,注册地址为英德市桥头镇五石熊屋村,注册资金1.2亿元,总占地817.36亩,员工1800人。公司现有90m2烧结机线一条、630m3高炉一座、75t转炉一座、50t电炉三座、高线60万吨两条、高线80万吨一条、双高棒180万吨1条。主要产品为螺纹钢、盘螺及高速线材。\\n\\n公司距广州160公里,韶关90公里,南北向分别有京港澳高速公路、昆汕高速公路、广乐高速公路,106国道连接珠三角地区及北部内地;东西向有省道S347线连接粤东、粤西。离京港澳、昆汕高速、广乐高速公路入口3公里,距北江明珠码头53公里,离新南华火车站28公里,北江与珠江交汇、连接珠三角,北江通航能力为1000-3000吨。\\n\\n2022年10月,敬业集团出资25亿元(全部自筹)收购广东粤北联合钢铁有限公司,是敬业集团继2020年收购广东泰都钢铁(后更名广东敬业)之后,华南战略布局又一重大决策。敬业集团并购粤北联合钢铁后,将与广东敬业钢铁形成产品、区域战略互补,增加敬业产品华南市场投放量,提升敬业品牌在华南地区的竞争力和影响力,同时借助敬业集团全球战略布局销售网络,依托广阔的海运优势销往东南亚等区域,提高国际市场占有率。近期主要产品产量、销量及主要财务数据如下:\\n\\n单位:吨、万元\\n\\n| 广东粤北 | 产量 | 销售量 | 总资产 | 总负债 | 营业收入 | 净利润 |\\n|---|---|---|---|---|---|---|\\n| 2024.06 | 358590 | 361643 | 320053 | 79131 | 121436 | 493 |\\n| 2023年 | 1051757 | 1086779 | 310671 | 70243 | 385256 | 2018 |\\n\\n截至2024年6月末,广东粤北钢铁在银行有短期借款余额1.04亿元,融资租赁余额1.42亿元,银承敞口1.4亿元。\\n\\n5、收购河北华西特种钢铁有限公司(简称“华西特钢”):成立于2019年3月,注册资金26.7亿元,位于唐山市海港开发区,距离京唐港约5公里,总占地2154.22亩,公司员工1537人。现有2座600t白灰双膛窑;1台320m2烧结机;1座2300m3高炉;1座170t转炉;1台12机12流方坯连铸机;2台制氧机(12000m3,25000m3各1台);1座85MW超高压亚临界发电机组;1座污水处理厂;1座80000m3转炉煤气柜,1座150000m3高炉煤气柜。\\n\\n公司原属于江苏新华西钢铁集团有限公司下属的国有控股企业,根据唐山市《关于加快建设环渤海地区新型工业化基地的意见(唐发[2018]19号)》、《唐山市钢铁工业转型升级发展规划》文件精神,河北华西钢铁有限公司属退城进园项目,新置换的产能由河北华西特种钢铁有限公司承建,最新备案产能:铁水176万吨,炼钢170万吨,棒材90万吨、线材73万吨。\\n\\n华西特钢成立后,由于经营状况较差,一直处于亏损状态,因此无锡市政府决定出售该企业,根据2023年7月4日无锡市人民政府发布的《河北华西特种钢铁有限公司100%股权》产权交易公告,本次收购价格为30亿元,敬业钢铁于2023年8月22日支付了9亿元交易价款,于2024年4月支付完成所有并购款项。\\n\\n并购前,2023年6月敬业集团已向华西特钢派驻管理团队,进行试运营后,认为亏损原因主要在于管理及原材料采购等方面,决定并购该企业。敬业钢铁收购华西特钢股权是为了敬业集团在唐山地区布局,华西特钢紧邻港口,运费方面有较大优势,且华西特钢设备均为2019年以后购置,设备在国内处于先进水平,符合相关政策要求。近期主要产品产量、销量及主要财务数据如下:\\n\\n单位:吨、万元\\n\\n| 华西特钢 | 产量 | 销售量 | 总资产 | 总负债 | 营业收入 | 净利润 |\\n|---|---|---|---|---|---|---|\\n| 2024.06 | 1317230 | 1320168 | 646625 | 493915 | 416028 | 4533 |\\n| 2023年 | 603902 | 824585 | 548725 | 401144 | 773278 | -12433 |\\n\\n注:2023年产量为敬业钢铁收购后开始统计,只统计了9月至12月,财务报表为全年数据。\\n\\n截至2024年6月末,华西特钢在银行有短期借款余额7.49亿元,票据敞口1.8亿元;融资租赁余额17.44亿元。\\n\\n6.连云港兴鑫钢铁有限公司:成立于2003年10月;位于江苏连云港灌南县堆沟港镇船舶工业园区;注册资本32160万元;现有180m2烧结机三台、1080m3高炉3座、120T转炉2座,主要产品螺纹钢,产能300万吨;敬业钢铁收购价格43.6亿元,资金来源为自有资金。股权交割已完成,团队已入驻。\\n\\n2024年6月4日敬业钢铁有限公司签订了连云港兴鑫钢铁股权收购协议,并于2024年6月20日正式完成连云港兴鑫钢铁100%股权变更及交割。该项目总交易价款43.6亿元,其中,股权转让价款35亿元(已完成首期款5亿元及二期款28亿元支付,剩余2亿元保证金一年后支付),同时承接公司负债。\\n\\n7.日钢营口中板有限公司:成立于2002年06月,注册资本18.49亿元,位于辽宁营口老边区,占地7266亩,职工6530人;公司拥有4座450m3高炉、2座2300m3高炉、4座120T转炉,2800mm、3800mm、5000mm中厚板生产线各1条及高速线材生产线3条,主要产品为中厚板,产能800万吨(板材600万吨、线材200万吨)。敬业钢铁收购价格176亿元,目前已支付96亿元,完成股权交割交割,一年内再支付剩余80亿元,拟融资100亿元。\\n\\n截至2024年6月,资产303.01亿元、负债100.43亿元,实现营业收入110.29亿元、净利润2.43亿元。\\n\\n收购原因:\\n\\n(1)原股东出售原因:营口中板原股东为日照钢铁,目前日钢的战略发展规划为打造薄板行业的标杆,而营口中板主要产品为中厚板,与其发展战略不符,无法实现战略协同。另外,日钢收购营口中板的初始原因为山钢有收购日钢的规划,日钢实际控制人杜双华将营口中厚板作为其被收购后的退路选择。目前山东地方政府组建钢铁集团的计划已终止,日照钢铁可集中精力在日照发展薄板行业,因此出售营口中板。\\n\\n(2)收购符合敬业集团发展战略:集团整体发展规划为“一棒三板”产品结构,棒指各类螺纹钢等线材。板指宽厚板、热轧卷板、冷轧板。收购完成后,与平山本部宽板和唐山华西宽板形成合力,产能达到1500万吨。\\n\\n(3)营口中板资产优质:营业中板目前仍处于盈利状态,在中厚板领域经营20余年,装备水平同行业处于先进水平,积累了大量的人才和技术,多年来中厚板出口量居全国前列,尤其是在船板领域,拥有十国船级社船板生产资质。除此之外,营口周边铁矿资源丰富,同时紧邻港口,区位优势非常明显。\\n\\n(4)敬业当前转型需要:敬业目前拳头产品为附加值较低的螺纹钢,普遍应用于基建、房地产领域,随着国内经济发展的放缓,需求量逐年下降,因此逐步开展附加值较高的板材板块,板材广泛应用于船舶、容器、风电等领域,受经济发展放缓影响较小。根据对当前经济形势的研判,敬业决定保持螺纹钢等棒材生产线的规模,同时逐步增加板材生产线。一方面,国内长期低迷的需求势必导致大部分钢企放弃螺纹钢等低附加值的产品,而敬业保持住当前的规模优势在将来必会转化为利润;另一方面,板材的需求在扩大,尤其是营口中板生产的船板。近年日韩等传统造船大国产业规模萎缩,我国已逐步成为造船第一大国,船板的需求势必逐年增长。"}],"options":[],"password":false,"name":"ETL#原始数据","display_name":"ETL#原始数据","type":"dict","clear_after_run":false,"list":false,"field_type":"textarea","is_added":true,"display_index":101,"node_id":"148bc14c-9053-463c-a99a-934a94d3aea4","node_type":"sql_etl","is_output":true,"source_node_id":"148bc14c-9053-463c-a99a-934a94d3aea4","source_node_type":"sql_etl","source_node_handle":"output_standard_chart","source_node_name":"ETL","source_workflow_run_result_id":"495679"},"调用内容":{"required":true,"placeholder":"","show":true,"multiline":true,"value":[{"uid":"vc-upload-1774572471586-4","statusName":"正在索引","originName":"调研内容(2).docx","filePath":"group1/M00/B2/95/CgAAcGnF09SAL8NvAAhulrcOS1I94.docx","hashValue":"ac625f2182ecc2373c575b9c0435f487","taskId":"365190","meta":{"taskId":"365190","docId":"ddd8eeca58f6eb0c68fc6dfc19373f8d"},"evidenceId":"ddd8eeca58f6eb0c68fc6dfc19373f8d","lastModified":1774340562843,"lastModifiedDate":"2026-03-24T08:22:42.843Z","name":"调研内容(2).docx","size":552598,"type":"application/vnd.openxmlformats-officedocument.wordprocessingml.document","percent":100,"originFileObj":{"uid":"vc-upload-1774572471586-4","statusName":"正在索引","originName":"调研内容(2).docx","filePath":"group1/M00/B2/95/CgAAcGnF09SAL8NvAAhulrcOS1I94.docx","hashValue":"ac625f2182ecc2373c575b9c0435f487","taskId":"365190","meta":{"taskId":"365190","docId":"ddd8eeca58f6eb0c68fc6dfc19373f8d"},"evidenceId":"ddd8eeca58f6eb0c68fc6dfc19373f8d"},"status":"done","xhr":{"uid":"vc-upload-1774572471586-4","statusName":"正在索引","originName":"调研内容(2).docx","filePath":"group1/M00/B2/95/CgAAcGnF09SAL8NvAAhulrcOS1I94.docx","hashValue":"ac625f2182ecc2373c575b9c0435f487","taskId":"365190","meta":{"taskId":"365190","docId":"ddd8eeca58f6eb0c68fc6dfc19373f8d"},"evidenceId":"ddd8eeca58f6eb0c68fc6dfc19373f8d"}}],"options":[],"password":false,"name":"调用内容","display_name":"调用内容","type":"uploadFiles","clear_after_run":false,"list":false,"field_type":"file","is_added":true,"display_index":101,"is_start":true}}""";
        List<JSONObject> processResult = tiptapDocumentProcessor.process(JSONObject.parseObject(json));
        System.out.println(processResult);
    }







}