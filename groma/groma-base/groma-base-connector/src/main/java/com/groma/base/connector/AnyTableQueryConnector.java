package com.groma.base.connector;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 受控外部数据库直连查询器（落实 BE-EXT-005：预配置数据源清单 + 标识符正则白名单 + 参数绑定 + 默认只读 + 行数与超时上限）
 */
public final class AnyTableQueryConnector {

    private static final Pattern SAFE_SQL_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    public record DataSourceRegistration(
            String name,
            DataSource dataSource,
            boolean readOnly,
            int queryTimeoutSeconds,
            int maxRowsLimit
    ) {}

    private final ConcurrentHashMap<String, DataSourceRegistration> configuredSources = new ConcurrentHashMap<>();

    public void registerConfiguredDataSource(DataSourceRegistration reg) {
        configuredSources.put(reg.name(), reg);
    }

    public List<Map<String, Object>> selectSafely(
            String dataSourceName,
            String tableName,
            List<String> selectColumns,
            Map<String, Object> equalsFilters,
            String orderByColumn,
            boolean ascending,
            int requestedLimit) throws Exception {

        DataSourceRegistration reg = configuredSources.get(dataSourceName);
        if (reg == null) {
            throw new SecurityException("[EXT-DB-001] 未在管理员清单中配置的数据源名称: " + dataSourceName);
        }
        validateIdentifier("tableName", tableName);

        List<String> cols = (selectColumns == null || selectColumns.isEmpty()) ? List.of("*") : selectColumns;
        if (!List.of("*").equals(cols)) {
            cols.forEach(c -> validateIdentifier("column", c));
        }

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", cols))
                .append(" FROM ").append(tableName);

        List<Object> boundParams = new ArrayList<>();
        if (equalsFilters != null && !equalsFilters.isEmpty()) {
            sql.append(" WHERE ");
            int idx = 0;
            for (Map.Entry<String, Object> f : equalsFilters.entrySet()) {
                validateIdentifier("filterColumn", f.getKey());
                if (idx++ > 0) {
                    sql.append(" AND ");
                }
                sql.append(f.getKey()).append(" = ?");
                boundParams.add(f.getValue());
            }
        }

        if (orderByColumn != null && !orderByColumn.isBlank()) {
            validateIdentifier("orderBy", orderByColumn);
            sql.append(" ORDER BY ").append(orderByColumn).append(ascending ? " ASC" : " DESC");
        }

        int effectiveMaxRows = Math.min(
                requestedLimit <= 0 ? reg.maxRowsLimit() : requestedLimit,
                reg.maxRowsLimit()
        );

        try (Connection conn = reg.dataSource().getConnection()) {
            if (reg.readOnly()) {
                conn.setReadOnly(true);
            }
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setQueryTimeout(Math.max(1, reg.queryTimeoutSeconds()));
                ps.setMaxRows(Math.max(1, effectiveMaxRows));
                for (int i = 0; i < boundParams.size(); i++) {
                    ps.setObject(i + 1, boundParams.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (rs.next() && rows.size() < effectiveMaxRows) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int c = 1; c <= colCount; c++) {
                            row.put(meta.getColumnLabel(c), rs.getObject(c));
                        }
                        rows.add(row);
                    }
                    return rows;
                }
            }
        }
    }

    public int executeWriteIfPermitted(String dataSourceName, String tableName, Map<String, Object> insertValues) throws Exception {
        DataSourceRegistration reg = configuredSources.get(dataSourceName);
        if (reg == null) {
            throw new SecurityException("[EXT-DB-001] 未配置的数据源: " + dataSourceName);
        }
        if (reg.readOnly()) {
            throw new SecurityException("[EXT-DB-003] 数据源 [" + dataSourceName + "] 为默认只读模式，禁止执行写入操作");
        }
        validateIdentifier("tableName", tableName);
        List<String> cols = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : insertValues.entrySet()) {
            validateIdentifier("insertColumn", e.getKey());
            cols.add(e.getKey());
            placeholders.add("?");
            params.add(e.getValue());
        }
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", placeholders) + ")";
        try (Connection conn = reg.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(Math.max(1, reg.queryTimeoutSeconds()));
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    private static void validateIdentifier(String label, String identifier) {
        if (identifier == null || !SAFE_SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("[EXT-DB-002] 非法的 SQL 标识符 (" + label + "): " + identifier);
        }
    }
}
