package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Edge;
import com.gemantic.gpt.support.workflow.Node;
import com.gemantic.gpt.support.workflow.OutputTable;
import com.gemantic.gpt.support.workflow.OutputTableDict;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * tableOperation
 */
@Component("tableOperation")
public class TableOperation extends BaseConsumer {
    private static final String SELECT_MAIN_TABLE="selectMainTable";
    private static final String UP_DOWN_MERGE="upDownMerge";
    private static final String LEFT_RIGHT_MERGE="leftRightMerge";
    private static final String LEFT_RIGHT_MERGE_LEFT="left";
    private static final String LEFT_RIGHT_MERGE_RIGHT="right";
    private static final String LEFT_RIGHT_MERGE_UNION="union";
    private static final String LEFT_RIGHT_MERGE_INNER="intersection";
    private static final String SORT="sorter";
    private static final String REMOVE_COLUMN="deleteColumn";
    private static final String SORT_ASC="asc";
    private static final String SORT_DESC="desc";

    @Value("${CONSUMER_TABLE_OPERATION_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }


    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();
        JSONObject originalInputs = WorkflowUtil.getInputsByTemplate(inputs);
        if (MapUtils.isNotEmpty(workflowRunResult.getData()) && CollectionUtils.isNotEmpty(workflowRunResult.getData().getJSONArray(WorkflowJSONKey.nodesHandle.name()))) {
            originalInputs = WorkflowUtil.getInputsByTemplate(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate());
        }
        JSONObject parameters = DataParametersUtil.mergeParameters(originalInputs, inputs);

        //获取边信息，建立表传入端口与表id的映射关系
        JSONObject edgeHandlesObj = new JSONObject();
        List<Edge> edgeHandles = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(),Edge.class);
        for (Edge edgeHandle : edgeHandles) {
            edgeHandlesObj.put(edgeHandle.getTargetHandle(), edgeHandle.getSource() + edgeHandle.getSourceHandle());
        }

        //获取表信息，通过表id映射
        JSONObject tables = new JSONObject();
        for (String key : parameters.keySet()) {
            JSONObject data = parameters.getJSONObject(key);
            String dataName = data.getString("name");
            if (data.getString("type").equals("table") || data.getString("type").equals("table_llm")) {
                for (int i = 0; i < data.getList("value", JSONObject.class).size(); i ++) {
                    if (edgeHandlesObj.getString(dataName) != null) {
                        tables.put(edgeHandlesObj.getString(dataName) + i, data.getList("value", JSONObject.class).get(i));
                    }
                }
            }
        }

        //获取操作流程
        JSONArray tableOperations = parameters.getJSONObject("table_merge").getJSONObject("merge_config").getJSONArray("operationRecord");

        String id;
        String mainTableId = "";
        JSONObject currTable;
        List<OutputTableDict> resultColumnsData = Lists.newArrayList();
        List<JSONObject> resultTableData = Lists.newArrayList();

        for (Object tableOperation : tableOperations) {
            JSONObject operation = ((JSONObject) tableOperation);
            String actionCode = operation.getString("action");
            List<JSONObject> finalTables;

            switch (actionCode) {
                case SELECT_MAIN_TABLE:
                    //获取主表信息并记录主表id
                    id = operation.getJSONObject("table").getString("id");
                    currTable = tables.getJSONObject(id);
                    resultColumnsData = currTable.getList("data_dict", OutputTableDict.class);
                    resultTableData = currTable.getList("data_list", JSONObject.class);
                    mainTableId = id;
                    break;
                case UP_DOWN_MERGE:
                    //单次操作步骤
                    finalTables = operation.getList("finalTables", JSONObject.class);

                    for (JSONObject table : finalTables) {
                        if (table.getString("name").equals("合并结果")) {
                            //准备表头
                            List<JSONObject> currColumnsData = table.getList("editColumns", JSONObject.class);
                            resultColumnsData = prepareColumnData(currColumnsData);
                        } else {
                            id = table.getString("id");
                            if (id != null && !id.isEmpty() && !id.equals(mainTableId)) {
                                //获取当前表数据
                                currTable = tables.getJSONObject(id);
                                List<JSONObject> currTableData = currTable.getList("data_list", JSONObject.class);

                                //非主表且未合并过时，组装数据
                                resultTableData = upDownMerge(resultColumnsData, resultTableData, currTableData);
                            } else {
                                //是主表或是已合并表时，格式化数据
                                resultTableData = upDownMerge(resultColumnsData, null, resultTableData);
                            }
                        }
                    }
                    break;
                case LEFT_RIGHT_MERGE:
                    //单次操作步骤
                    finalTables = operation.getList("finalTables", JSONObject.class);

                    for (JSONObject table : finalTables) {
                        if (table.getString("name").equals("合并结果")) {
                            //准备表头
                            List<JSONObject> currColumnsData = table.getList("editColumns", JSONObject.class);
                            resultColumnsData = prepareColumnData(currColumnsData);
                        } else {
                            id = table.getString("id");
                            if (id != null && !id.isEmpty() && !id.equals(mainTableId)) {
                                //获取当前表数据
                                currTable = tables.getJSONObject(id);
                                List<JSONObject> currTableData = currTable.getList("data_list", JSONObject.class);

                                List<String> mergeByList = Lists.newArrayList();
                                List<JSONObject> mergeColumns = table.getList("leftRightMergeColumn", JSONObject.class);
                                for (JSONObject mergeColumn : mergeColumns) {
                                    mergeByList.add(mergeColumn.getString("columnName"));
                                }

                                //组装数据
                                String subMergeType = operation.getString("mergeType");
                                switch (subMergeType) {
                                    case LEFT_RIGHT_MERGE_LEFT:
                                        resultTableData = leftJoin(resultColumnsData, resultTableData, currTableData, mergeByList);
                                        break;
                                    case LEFT_RIGHT_MERGE_RIGHT:
                                        resultTableData = rightJoin(resultColumnsData, resultTableData, currTableData, mergeByList);
                                        break;
                                    case LEFT_RIGHT_MERGE_UNION:
                                        resultTableData = union(resultColumnsData, resultTableData, currTableData, mergeByList);
                                        break;
                                    case LEFT_RIGHT_MERGE_INNER:
                                        resultTableData = innerJoin(resultColumnsData, resultTableData, currTableData, mergeByList);
                                        break;
                                }
                            }
                        }
                    }
                    break;
                case REMOVE_COLUMN:
                    //当前一次删除一列，预留多列模式
                    String removedColumn = operation.getJSONObject("column").getString("name");
                    List<String> removedColumns = Collections.singletonList(removedColumn);
//                    List<String> removedColumns = operation.getList("column", String.class);
                    removeColumnsFromColumnList(resultColumnsData, removedColumns);
                    removeColumnsFromDataList(resultTableData, removedColumns);
                    break;
                case SORT:
                    String sortBy = operation.getString("sorterField");
                    String sortType = operation.getString("sorterOrder");
                    resultTableData = sortData(resultTableData, sortBy, sortType);
                    break;
            }
        }
        //组装输出
        OutputTable outputTable = new OutputTable();
        outputTable.setData_list(resultTableData);
        outputTable.setData_dict(resultColumnsData);
        outputTable.setTables(null);
        outputTable.setIs_fiance_table(false);
        outputTable.setType(WorkflowJSONKey.table.name());
        if (StringUtils.isBlank(outputTable.getName())) {
            outputTable.setName("0");
        }

        List<JSONObject> outputs = Lists.newArrayList();
        List<OutputTable> output_table_value = Lists.newArrayList();
        output_table_value.add(outputTable);
        JSONObject output_table = WorkflowUtil.createOutput(WorkflowJSONKey.output_table.name(), WorkflowJSONKey.table.name(), output_table_value);
        outputs.add(output_table);
        return Response.ok(outputs);
    }

    /**
     * 获取结果表头(直接获取)
     *
     * @param columnData
     * @return
     */
    private static List<OutputTableDict> prepareColumnData(List<JSONObject> columnData) {
        List<OutputTableDict> resultDataList = Lists.newArrayList();
        for (JSONObject column : columnData) {
            OutputTableDict oneRowData = new OutputTableDict();
            oneRowData.setColumnName(column.getString("columnName"));
            oneRowData.setColumnComment(column.getString("columnName"));
            oneRowData.setUnit("");
            resultDataList.add(oneRowData);
        }
        return resultDataList;
    }

    /**
     * 获取结果表头(原表拼装) 预留
     *
     * @param masterColumnData
     * @param slaveColumnData
     * @return
     */
    private static List<OutputTableDict> prepareColumnData(List<OutputTableDict> masterColumnData, List<OutputTableDict> slaveColumnData, List<String> mergeByList) {
        List<OutputTableDict> resultDataList;
        List<OutputTableDict> duplicateColumnList = Lists.newArrayList();
        List<String> masterColumnList = Lists.newArrayList();
        //获取主表字段名数组
        for (OutputTableDict column : masterColumnData) {
            masterColumnList.add(column.getColumnName());
        }
        for (OutputTableDict slaveColumn : slaveColumnData) {
            boolean matched = false;
            for (OutputTableDict masterColumn : masterColumnData) {
                if (slaveColumn.getColumnName().equals(masterColumn.getColumnName())) {
                    if (mergeByList != null && !mergeByList.isEmpty()) {
                        //副表表头命中主表表头时，检查是否为合并字段，为否时需要增加字段名后缀防止冲突
                        if (!mergeByList.contains(masterColumn.getColumnName())) {
                            OutputTableDict oneRowData = new OutputTableDict();
                            String columnName = slaveColumn.getColumnName();
                            //获取新字段名
                            String newColumnName = getNewColumnName(masterColumnList, columnName, columnName, 0);
                            oneRowData.setColumnName(newColumnName);
                            oneRowData.setColumnComment(columnName);
                            oneRowData.setUnit("");
                            duplicateColumnList.add(oneRowData);
                        }
                    }
                    matched = true;
                    break;
                }
            }
            //副表表头未命中主表表头，追加到主表表头列表中
            if (!matched) {
                OutputTableDict oneRowData = new OutputTableDict();
                oneRowData.setColumnName(slaveColumn.getColumnName());
                oneRowData.setColumnComment(slaveColumn.getColumnComment());
                oneRowData.setUnit("");
                masterColumnData.add(oneRowData);
            }
        }
        //重名字段增加后缀后追加到主表表头列表中
        masterColumnData.addAll(duplicateColumnList);
        resultDataList = masterColumnData;
        return resultDataList;
    }

    /**
     * 上下合并
     *
     * @param columnList
     * @param masterDataList
     * @param slaveDataList
     * @return
     */
    private static List<JSONObject> upDownMerge(List<OutputTableDict> columnList, List<JSONObject> masterDataList, List<JSONObject> slaveDataList) {
        List<JSONObject> resultDataList = Lists.newArrayList();
        if (masterDataList != null) {
            resultDataList = masterDataList;
        }
        for (JSONObject slaveData : slaveDataList) {
            JSONObject oneLineData = new JSONObject();
            for (OutputTableDict column : columnList) {
                boolean matched = false;
                for (String slaveDataKey : slaveData.keySet()) {
                    Object value = slaveData.get(slaveDataKey);
                    if (slaveDataKey.equals(column.getColumnName())) {
                        oneLineData.put(slaveDataKey, value);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    oneLineData.put(column.getColumnName(), null);
                }
            }
            resultDataList.add(oneLineData);
        }
        return resultDataList;
    }

    /**
     * 左合并
     *
     * @param columnList
     * @param leftDataList
     * @param rightDataList
     * @param mergeByList
     * @return
     */
    private static List<JSONObject> leftJoin(List<OutputTableDict> columnList, List<JSONObject> leftDataList, List<JSONObject> rightDataList, List<String> mergeByList) {
        return leftRightMerge(columnList, leftDataList, rightDataList, mergeByList, true);
    }

    /**
     * 右合并
     *
     * @param columnList
     * @param leftDataList
     * @param rightDataList
     * @param mergeByList
     * @return
     */
    private static List<JSONObject> rightJoin(List<OutputTableDict> columnList, List<JSONObject> leftDataList, List<JSONObject> rightDataList, List<String> mergeByList) {
        return leftRightMerge(columnList, rightDataList, leftDataList, mergeByList, true);
    }

    /**
     * 并集合并
     *
     * @param columnList
     * @param leftDataList
     * @param rightDataList
     * @param mergeByList
     * @return
     */
    private static List<JSONObject> union(List<OutputTableDict> columnList, List<JSONObject> leftDataList, List<JSONObject> rightDataList, List<String> mergeByList) {
        List<JSONObject> unionData = Lists.newArrayList();
        List<JSONObject> leftJoinData = leftRightMerge(columnList, leftDataList, rightDataList, mergeByList, true);
        unionData.addAll(leftJoinData);
        List<JSONObject> rightJoinData = leftRightMerge(columnList, rightDataList, leftDataList, mergeByList, true);
        for (JSONObject rightData : rightJoinData) {
            if (!unionData.contains(rightData)){
                unionData.add(rightData);
            }
        }
        return unionData;
    }

    /**
     * 交集合并
     *
     * @param columnList
     * @param leftDataList
     * @param rightDataList
     * @param mergeByList
     * @return
     */
    private static List<JSONObject> innerJoin(List<OutputTableDict> columnList, List<JSONObject> leftDataList, List<JSONObject> rightDataList, List<String> mergeByList) {
        return leftRightMerge(columnList, leftDataList, rightDataList, mergeByList, false);
    }

    /**
     * 左右合并
     *
     * @param columnList
     * @param masterDataList
     * @param slaveDataList
     * @param mergeByList
     * @param withNotMatched
     * @return
     */
    private static List<JSONObject> leftRightMerge(List<OutputTableDict> columnList, List<JSONObject> masterDataList, List<JSONObject> slaveDataList, List<String> mergeByList, boolean withNotMatched) {
        List<JSONObject> resultDataList = Lists.newArrayList();
        //合并字段的数量
        int matchedColumnsCount = mergeByList.size();
        for (JSONObject masterData : masterDataList) {
            List<JSONObject> matchedData = Lists.newArrayList();
            for (JSONObject slaveData : slaveDataList) {
                int matchedCount = 0;
                for (String masterKey : masterData.keySet()) {
                    Object masterValue = masterData.get(masterKey);
                    for (String slaveKey : slaveData.keySet()) {
                        Object slaveValue = slaveData.get(slaveKey);
                        if (masterKey.equals(slaveKey)) {
                            //字段值匹配且为字段为合并字段
                            if (masterValue.equals(slaveValue) && mergeByList.contains(masterKey)) {
                                matchedCount ++;
                                //所有合并字段值完全匹配，记录当前行副表数据用于与对应主表数据合并
                                if (matchedCount == matchedColumnsCount) {
                                    matchedData.add(slaveData);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (!matchedData.isEmpty()) {
                //开始合并对应数据
                for (JSONObject slaveData : matchedData) {
                    JSONObject oneLineData = new JSONObject();
                    for (OutputTableDict column : columnList) {
                        if (masterData.containsKey(column.getColumnName())) {
                            //写入主表数据
                            oneLineData.put(column.getColumnName(), masterData.get(column.getColumnName()));
                        }
                        //当前列数据为空时才准许副表写入，防止复写主表数据
                        if (oneLineData.getString(column.getColumnName()) == null) {
                            if (slaveData.containsKey(column.getColumnName())) {
                                //写入副表数据
                                oneLineData.put(column.getColumnName(), slaveData.get(column.getColumnName()));
                            } else {
                                //数据中出现重复字段时重复字段名会被加入数字后缀，给这类字段填入对应副表数据
                                for (String slaveKey : slaveData.keySet()) {
                                    if (column.getColumnName().startsWith(slaveKey)) {
                                        String duplicateColumnNumber = column.getColumnName().replace(slaveKey, "");
                                        if (isDigit(duplicateColumnNumber)) {
                                            oneLineData.put(column.getColumnName(), slaveData.get(slaveKey));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    resultDataList.add(oneLineData);
                }
            } else {
                //是否显示不匹配数据(是:左/右/并集 否:交集)
                if (withNotMatched) {
                    JSONObject oneLineData = new JSONObject();
                    for (OutputTableDict column : columnList) {
                        //记录主表不匹配数据
                        oneLineData.put(column.getColumnName(), masterData.get(column.getColumnName()));
                    }
                    resultDataList.add(oneLineData);
                }
            }
        }
        return resultDataList;
    }

    /**
     * 删除表头中的指定字段
     *
     * @param columnList
     * @param removedColumns
     * @return
     */
    private static List<OutputTableDict> removeColumnsFromColumnList(List<OutputTableDict> columnList, List<String> removedColumns) {
        columnList.removeIf(column -> removedColumns.contains(column.getColumnName()));
        return columnList;
    }

    /**
     * 删除数据中的指定字段
     *
     * @param dataList
     * @param removedColumns
     * @return
     */
    private static List<JSONObject> removeColumnsFromDataList(List<JSONObject> dataList, List<String> removedColumns) {
        for (JSONObject data : dataList) {
            for (String removedColumn : removedColumns) {
                data.remove(removedColumn);
            }
        }
        return dataList;
    }

    /**
     * 删除数据中的指定字段
     *
     * @param dataList
     * @param sortBy
     * @param sortType
     * @return
     */
    public static List<JSONObject> sortData(List<JSONObject> dataList, String sortBy, String sortType) {
        if (dataList == null || dataList.size() < 2) {
            return dataList;
        }
        dataList.sort(new DataComparator(sortBy, sortType));
        return dataList;
    }

    /**
     * 检查字符串是否为数字串
     *
     * @param content
     * @return
     */
    private static boolean isDigit(String content) {
        if (StringUtils.isBlank(content)) {
            return Boolean.FALSE;
        } else {
            Pattern p = Pattern.compile("^[0-9]+$");
            Matcher m = p.matcher(content);
            return m.matches();
        }
    }

    /**
     * 列名重复时获取新列名(向原有列名后追加数字序号)
     *
     * @param columnList
     * @param currColumnName
     * @param originColumnName
     * @param number
     * @return
     */
    private static String getNewColumnName(List<String> columnList, String currColumnName, String originColumnName, int number) {
        String newColumnName = currColumnName;
        if (columnList.contains(newColumnName)) {
            number++;
            newColumnName = originColumnName + number;
            newColumnName = getNewColumnName(columnList, newColumnName, originColumnName, number);
        }
        return newColumnName;
    }

    private static class DataComparator implements Comparator<JSONObject>, Serializable {
        private static final long serialVersionUID = 1L;
        private static final Integer TYPE_NUMBER = 0;
        private static final Integer TYPE_CHARACTER = 1;
        private final String sortBy;
        private final String sortType;

        public DataComparator(String sortBy, String sortType) {
            this.sortBy = sortBy;
            this.sortType = sortType;
        }

        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            int compare = 0;
            switch (sortType) {
                case SORT_ASC:
                    compare = doCompare(o1, o2);
                    break;
                case SORT_DESC:
                    compare = doCompare(o2, o1);
                    break;
            }
            return compare;
        }

        private int doCompare(JSONObject o1, JSONObject o2) {
            String o1s = o1.getString(sortBy);
            String o2s = o2.getString(sortBy);

            String[] o1Chars = o1s != null ? o1s.split("") : new String[0];
            String[] o2Chars = o2s != null ? o2s.split("") : new String[0];

            List<List<Object>> o1CharList = getCharList(o1Chars);
            List<List<Object>> o2CharList = getCharList(o2Chars);

            int max = Math.max(o1CharList.size(), o2CharList.size());
            while (o1CharList.size() < max) {
                o1CharList.add(Lists.newArrayList());
            }
            while (o2CharList.size() < max) {
                o2CharList.add(Lists.newArrayList());
            }

            int compare = 0;
            for (int i = 0;i < max;i ++) {
                List<Object> o1List = o1CharList.get(i);
                List<Object> o2List = o2CharList.get(i);

                if (o1List.isEmpty()) {
                    compare = -1;
                    break;
                }
                if (o2List.isEmpty()) {
                    compare = 1;
                    break;
                }

                Integer o1Type = (Integer) o1List.getFirst();
                Integer o2Type = (Integer) o2List.getFirst();
                int typeCompare = Integer.compare(o1Type, o2Type);
                if (typeCompare != 0) {
                    compare = typeCompare;
                    break;
                } else {
                    if (TYPE_NUMBER.equals(o1Type)) {
                        BigDecimal o1Content = new BigDecimal(o1List.get(1).toString());
                        BigDecimal o2Content = new BigDecimal(o2List.get(1).toString());
                        compare = o1Content.compareTo(o2Content);
                    } else if (TYPE_CHARACTER.equals(o1Type)) {
                        String o1Content = (String) o1List.get(1);
                        String o2Content = (String) o2List.get(1);
                        compare = Collator.getInstance(Locale.CHINESE).compare(o1Content, o2Content);
                    }
                    if (compare != 0) {
                        break;
                    }
                }
            }
            return compare;
        }

        private List<List<Object>> getCharList(String[] chars) {
            List<List<Object>> charList = Lists.newArrayList();
            List<Object> list;
            for (int i = 0;i < chars.length;i ++) {
                char c = (chars[i].toCharArray())[0];
                list = Lists.newArrayList();
                if (((int) c >= '0' && (int) c <= '9')) {
                    StringBuilder str = new StringBuilder();
                    do {
                        str.append(c);
                        if (i + 1 < chars.length) {
                            c = (chars[++i].toCharArray())[0];
                        } else {
                            break;
                        }
                    } while ((int) c >= '0' && (int) c <= '9');
                    if (!(i + 1 == chars.length) || !(((int) c >= '0' && (int) c <= '9'))) {
                        i --;
                    }
                    list.add(TYPE_NUMBER);
                    list.add(str.toString());
                } else {
                    list.add(TYPE_CHARACTER);
                    list.add(String.valueOf(c));
                }
                charList.add(list);
            }
            return charList;
        }
    }
}
