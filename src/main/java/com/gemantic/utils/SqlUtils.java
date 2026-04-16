package com.gemantic.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * @author Yezhiwei
 * @Description SQL 解析工具，jsqlparser 版本为 4.9； 5.0 及以上版本需要 JDK 11
 * @date 2024/11/12 15:04
 */
public class SqlUtils {

    public static void validateJoin(String sql) throws JSQLParserException {
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new JSQLParserException("不支持的语法，忽略检查", e);
        }

        if (statement instanceof Select) {
            Select selectStatement = (Select) statement;
            // 检查JOIN条件
            checkJoinConditions(selectStatement);
        } else {
            throw new JSQLParserException("非 Select，禁止执行");
        }

    }

    private static void checkJoinConditions(Select statement) throws JSQLParserException {
        if (statement instanceof PlainSelect) {
            PlainSelect selectBody = (PlainSelect) statement;
            List<Join> joins = selectBody.getJoins();
            if (CollectionUtils.isEmpty(joins)) {
                return;
            }
            for (Join join : joins) {
                if (CollectionUtils.isEmpty(join.getOnExpressions())) {
                    throw new JSQLParserException("JOIN 操作缺少 ON 条件，可能会导致全表扫描");
                } else {
                    Expression onExpr = join.getOnExpression();
                    if (isAlwaysTrue(onExpr) || isAlwaysFalse(onExpr)) {
                        throw new JSQLParserException("JOIN 条件没有实际字段或没有意义，请检查 SQL");
                    } else if (isJoiningSameTable(join)) {
                        throw new JSQLParserException("JOIN 的表相同，JOIN 生效的没有意义，请检查 SQL");
                    } else {
                        // 可以在这里添加更多的JOIN条件检查逻辑
                    }
                }
            }
        }

    }

    private static boolean isAlwaysTrue(Expression expr) {
        if (expr instanceof EqualsTo) {
            EqualsTo equalsTo = (EqualsTo) expr;
            if (equalsTo.getLeftExpression() instanceof LongValue && equalsTo.getRightExpression() instanceof LongValue) {
                LongValue left = (LongValue) equalsTo.getLeftExpression();
                LongValue right = (LongValue) equalsTo.getRightExpression();
                return String.valueOf(left.getValue()).equals(String.valueOf(right.getValue()));
            }
        }
        return false;
    }

    private static boolean isAlwaysFalse(Expression expr) {
        if (expr instanceof EqualsTo) {
            EqualsTo equalsTo = (EqualsTo) expr;
            if (equalsTo.getLeftExpression() instanceof LongValue && equalsTo.getRightExpression() instanceof LongValue) {
                LongValue left = (LongValue) equalsTo.getLeftExpression();
                LongValue right = (LongValue) equalsTo.getRightExpression();
                return !String.valueOf(left.getValue()).equals(String.valueOf(right.getValue()));
            }
        }
        return false;
    }

    private static boolean isJoiningSameTable(Join join) {
        Expression onExpression = join.getOnExpression();
        if (onExpression instanceof EqualsTo) {
            EqualsTo equalsTo = (EqualsTo) onExpression;
            Expression leftExpression = equalsTo.getLeftExpression();
            Expression rightExpression = equalsTo.getRightExpression();
            if (leftExpression instanceof Column && rightExpression instanceof Column) {
                Column left = (Column) leftExpression;
                Column right = (Column) rightExpression;
                if (Objects.nonNull(left.getTable()) && Objects.nonNull(right.getTable())) {
                    return left.getTable().getName().equals(right.getTable().getName());
                }
            }
        }
        return false;
    }

    public static String removeComments(String sql) {
        // 1. 保护单引号和双引号字符串
        List<String> strings = new ArrayList<>();
        // 支持单、双引号
        Pattern stringPattern = Pattern.compile("(['\"])(?:''|\"\"|(?!\\1).)*?\\1");
        Matcher matcher = stringPattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            strings.add(matcher.group());
            matcher.appendReplacement(sb, "__STRING_" + (strings.size() - 1) + "__");
        }
        matcher.appendTail(sb);
        String processed = sb.toString();

        // 2. 移除注释
        // 步骤2：移除多行注释（启用DOTALL模式匹配跨行注释）
        Pattern multiLinePattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        processed = multiLinePattern.matcher(processed).replaceAll("");

        // 3：移除单行注释（--和#开头）
        processed = processed.replaceAll("--[^\\r\\n]*", "");
        processed = processed.replaceAll("#[^\\r\\n]*", "");

        // 4. 还原字符串
        for (int i = 0; i < strings.size(); i++) {
            processed = processed.replace("__STRING_" + i + "__", strings.get(i));
        }

        return processed;
    }

    // public static void main(String[] args) {
    //     String sql = "SELECT * FROM users -- 注释\n" +
    //             "WHERE name = 'John''s /*名字*/' /* 多行\n注释 */ AND age > 20;";
    //
    //     sql = "SELECT * FROM users -- 注释\n" +
    //             "WHERE name = \"John''s /*名字*/\" /* 多行\n注释 */ AND age > 20 and age = {{变量}}" +
    //             " -- {{变量1}};";
    //
    //     // 用例1：双引号字符串包含注释符号
    //     // sql = "SELECT \"This is -- not a comment\" FROM \"my_table\";";
    //
    //     // 用例2：混合引号转义
    //     // sql = "INSERT INTO table /*abcd*/ VALUES ('John''s \"data\"', \"Jane\"s 'record'\");";
    //
    //     // 用例3：注释出现在字符串外
    //     // sql = "UPDATE users SET name = \"Maria\" -- 这里需要注释\n"+
    //     // "WHERE id = 1;";
    //
    //     System.out.println("原始SQL：\n" + sql);
    //     System.out.println("\n处理后SQL：\n" + removeComments(sql));
    // }
}
