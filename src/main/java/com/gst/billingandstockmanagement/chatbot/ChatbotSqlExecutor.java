package com.gst.billingandstockmanagement.chatbot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * Owns its OWN connection pool (read-only MySQL user). It is deliberately NOT exposed as a
 * DataSource/JdbcTemplate bean, so it can't interfere with JPA, Flyway or existing code.
 */
@Component
public class ChatbotSqlExecutor {

    public static class QueryFailedException extends RuntimeException {
        public QueryFailedException(String message) { super(message); }
    }

    private static final int MAX_ROWS = 200;
    private static final int TIMEOUT_SECONDS = 5;

    private final HikariDataSource pool;

    public ChatbotSqlExecutor(@Value("${spring.datasource.url}") String url,
                              @Value("${chatbot.datasource.username:}") String username,
                              @Value("${chatbot.datasource.password:}") String password) {
        HikariConfig c = new HikariConfig();
        c.setPoolName("chatbot-readonly");
        c.setJdbcUrl(url);
        c.setUsername(username);
        c.setPassword(password);
        c.setMaximumPoolSize(3);
        c.setMinimumIdle(0);
        c.setReadOnly(true);
        c.setConnectionInitSql("SET SESSION MAX_EXECUTION_TIME=5000");
        c.setInitializationFailTimeout(-1);   // a missing/wrong chatbot user must never stop the app booting
        this.pool = new HikariDataSource(c);
    }

    /** userId MUST come from the JWT (SecurityUtils), never from the request. */
    public List<Map<String, Object>> run(Long userId, String sql, List<?> params) {
        return run(userId, sql, params, MAX_ROWS);
    }

    public List<Map<String, Object>> run(Long userId, String sql, List<?> params, int maxRows) {
        if (userId == null) throw new IllegalArgumentException("userId is required");

        try (Connection conn = pool.getConnection()) {
            try {
                try (PreparedStatement set = conn.prepareStatement("SET @chatbot_uid = ?")) {
                    set.setLong(1, userId);
                    set.execute();
                }
                try (PreparedStatement st = conn.prepareStatement(sql)) {
                    st.setQueryTimeout(TIMEOUT_SECONDS);
                    st.setMaxRows(maxRows);
                    for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
                    try (ResultSet rs = st.executeQuery()) {
                        return read(rs);
                    }
                }
            } finally {
                try (Statement clear = conn.createStatement()) {
                    clear.execute("SET @chatbot_uid = NULL");
                } catch (SQLException ignored) { /* next run sets its own id anyway */ }
            }
        } catch (SQLException e) {
            throw new QueryFailedException(oneLine(e.getMessage()));
        }
    }

    private static List<Map<String, Object>> read(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), normalize(rs.getObject(i)));
            rows.add(row);
        }
        return rows;
    }

    private static Object normalize(Object v) {
        if (v instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (v instanceof java.sql.Timestamp t) return t.toLocalDateTime().toString();
        if (v instanceof java.time.temporal.TemporalAccessor) return v.toString();
        return v;
    }

    private static String oneLine(String msg) {
        if (msg == null) return "query failed";
        String s = msg.replaceAll("\\s+", " ").trim();
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    @PreDestroy
    void close() { pool.close(); }
}