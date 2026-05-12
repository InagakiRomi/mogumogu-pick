package com.romi.mogumogu.scripts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.romi.mogumogu.logging.JulLoggerFactory;

public class ImportGeneratedSql {

    private static final Logger log = new JulLoggerFactory().printToolLog();
    /** 產生 SQL 的目錄 */
    private static final String GENERATED_SQL_DIR = "sql/generated";
    /** MySQL 匯入來源檔 */
    private static final String MYSQL_SQL_FILE = "data-mysql.sql";
    /** H2 匯入來源檔 */
    private static final String H2_SQL_FILE = "data-h2.sql";

    private static final String DEFAULT_MYSQL_HOST = "localhost";
    private static final String DEFAULT_MYSQL_PORT = "3306";
    private static final String DEFAULT_MYSQL_DB = "mogumogu";
    private static final String DEFAULT_MYSQL_USER = "root";
    private static final String DEFAULT_MYSQL_PASSWORD = "";

    private static final String DEFAULT_H2_URL = "jdbc:h2:file:./data/mogumogu;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE";
    private static final String DEFAULT_H2_USER = "sa";
    private static final String DEFAULT_H2_PASSWORD = "";
    private static final String DEFAULT_ACTIVE_PROFILE = "h2";

    private static final Set<String> EXCLUDED_TABLES = Set.of("flyway_schema_history");
    private static final Pattern SIMPLE_INSERT_PATTERN = Pattern.compile(
            "(?is)^INSERT\\s+INTO\\s+([`\"]?[A-Za-z0-9_]+[`\"]?)\\s*\\(([^)]*)\\)\\s+VALUES\\b");

    /** 依 SPRING_PROFILES_ACTIVE 選擇 MySQL 或 H2 匯入 */
    public static void main(String[] args) throws Exception {
        ImportTarget target = resolveTarget(env("SPRING_PROFILES_ACTIVE", DEFAULT_ACTIVE_PROFILE));
        try {
            runImport(target);
        } finally {
            shutdownMysqlCleanupThread();
        }
    }

    /** 單一資料庫匯入流程：驗證 SQL -> 清空表 -> 執行 SQL */
    private static void runImport(ImportTarget target) {
        try {
            try (Connection connection = DriverManager.getConnection(target.url(), target.user(), target.password())) {
                List<String> statements = loadSqlStatements(target.sqlPath());
                connection.setAutoCommit(false);
                try {
                    validateGeneratedSqlAgainstConnection(connection, statements, target.label(), target.mysql());
                    clearTables(connection, target);
                    executeSqlStatements(connection, statements, target.sqlPath());
                    connection.commit();
                    log.info("[" + target.label() + "] 清空並匯入完成");
                } catch (Exception ex) {
                    rollbackQuietly(connection);
                    throw ex;
                }
            }
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (target.mysql() && isConnectivityError(ex)) {
                log.warning("[警告] MySQL 連線失敗。原因：" + message);
            } else {
                log.severe("[" + target.label() + "] 匯入失敗：" + message);
            }
            throw new IllegalStateException(formatFailureMessage(target.label(), message), ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            log.log(Level.SEVERE, "[" + target.label() + "] 匯入失敗：" + ex.getMessage(), ex);
            throw new IllegalStateException(formatFailureMessage(target.label(), ex.getMessage()), ex);
        } finally {
            if (!target.mysql()) {
                shutdownH2Database(target);
            }
        }
    }

    /** 明確通知 H2 關閉資料庫，避免背景執行緒殘留 */
    private static void shutdownH2Database(ImportTarget target) {
        try (Connection connection = DriverManager.getConnection(target.url(), target.user(), target.password());
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException ex) {
            log.fine("H2 SHUTDOWN 指令略過：" + ex.getMessage());
        }
    }

    /** 發生例外時盡力 rollback，避免部分資料殘留 */
    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            log.fine("Rollback 略過：" + rollbackEx.getMessage());
        }
    }

    /** Maven exec classloader 下，主動關閉 MySQL 清理執行緒 */
    private static void shutdownMysqlCleanupThread() {
        try {
            Class<?> cleanupThread = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            cleanupThread.getMethod("checkedShutdown").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // 未使用 MySQL driver 時可忽略
        } catch (Exception ex) {
            log.fine("無法關閉 MySQL cleanup thread：" + ex.getMessage());
        }
    }

    /** 清空目標資料庫實體表，保留排除名單 */
    private static void clearTables(Connection connection, ImportTarget target) throws SQLException {
        List<String> tableNames = loadTableNames(connection, target.tableQuerySql());
        try (Statement statement = connection.createStatement()) {
            statement.execute(target.disableConstraintsSql());
            for (String tableName : tableNames) {
                if (!isExcludedTable(tableName)) {
                    statement.execute("TRUNCATE TABLE " + target.quoteIdentifier(tableName));
                }
            }
            statement.execute(target.enableConstraintsSql());
        }
    }

    /** 依查詢結果載入表名清單 */
    private static List<String> loadTableNames(Connection connection, String sql) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
            }
        }
        return tableNames;
    }

    /** INSERT 語句的最小結構資訊（表名與欄位） */
    private record InsertStatementSpec(String tableName, List<String> columnNames) {
    }

    /** 目標資料庫設定與差異化 SQL */
    private record ImportTarget(
            String label,
            boolean mysql,
            String url,
            String user,
            String password,
            Path sqlPath,
            String tableQuerySql,
            String disableConstraintsSql,
            String enableConstraintsSql) {
        private String quoteIdentifier(String identifier) {
            return mysql ? quoteMysqlIdentifier(identifier) : quoteH2Identifier(identifier);
        }
    }

    /** 比對 SQL 檔內容與目前資料庫結構是否相容 */
    private static void validateGeneratedSqlAgainstConnection(
            Connection connection,
            List<String> statements,
            String dialectLabel,
            boolean mysql) throws Exception {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < statements.size(); i++) {
            String stmt = statements.get(i).trim();
            if (stmt.isEmpty()) {
                continue;
            }
            int oneBased = i + 1;
            InsertStatementSpec spec = parseInsertStatement(stmt);
            if (spec == null) {
                errors.add("第 " + oneBased + " 條不是可辨識的 INSERT INTO ... VALUES 格式。");
                continue;
            }
            errors.addAll(validateInsertSpecAgainstDb(connection, spec, mysql, oneBased));
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    dialectLabel + "：產生的 SQL 與目前資料庫結構不符：\n - " + String.join("\n - ", errors));
        }
    }

    /** 檢查 INSERT 的表名與欄位是否存在於 DB */
    private static List<String> validateInsertSpecAgainstDb(
            Connection connection,
            InsertStatementSpec spec,
            boolean mysql,
            int statementIndex) {
        List<String> errors = new ArrayList<>();
        String table = spec.tableName();
        List<String> fileCols = spec.columnNames();

        List<String> dbColumns;
        try {
            dbColumns = loadDbColumnNames(connection, table, mysql);
        } catch (SQLException ex) {
            errors.add(
                    "第 "
                            + statementIndex
                            + " 條（表「"
                            + table
                            + "」）：查詢資料欄失敗："
                            + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            return errors;
        }

        if (dbColumns.isEmpty()) {
            errors.add(
                    "第 "
                            + statementIndex
                            + " 條（資料表「"
                            + table
                            + "」在目前資料庫中不存在。");
            return errors;
        }

        Set<String> dbLower = new HashSet<>();
        for (String d : dbColumns) {
            dbLower.add(d.toLowerCase(Locale.ROOT));
        }
        for (String fc : fileCols) {
            if (!dbLower.contains(fc.toLowerCase(Locale.ROOT))) {
                errors.add(
                        "第 "
                                + statementIndex
                                + " 條（表「"
                                + table
                                + "」）：找不到欄位「"
                                + fc
                                + "」。此表欄位："
                                + String.join(", ", dbColumns));
            }
        }
        return errors;
    }

    /** 讀取資料表欄位（依 ordinal_position） */
    private static List<String> loadDbColumnNames(Connection connection, String tableName, boolean mysql)
            throws SQLException {
        String schemaCondition = mysql ? "table_schema = DATABASE()" : "table_schema = 'PUBLIC'";
        String sql = "SELECT column_name FROM information_schema.columns WHERE "
                + schemaCondition
                + " AND LOWER(table_name) = LOWER(?) ORDER BY ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            List<String> cols = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cols.add(rs.getString(1));
                }
            }
            return cols;
        }
    }

    /** 解析 INSERT INTO ... (cols) VALUES ... 的表名與欄位 */
    private static InsertStatementSpec parseInsertStatement(String statement) {
        Matcher matcher = SIMPLE_INSERT_PATTERN.matcher(statement.trim());
        if (!matcher.find()) {
            return null;
        }
        String tableName = normalizeSqlIdentifier(matcher.group(1));
        String rawColumns = matcher.group(2);
        List<String> columns = new ArrayList<>();
        for (String part : rawColumns.split(",")) {
            String column = normalizeSqlIdentifier(part.trim());
            if (!column.isEmpty()) {
                columns.add(column);
            }
        }
        if (columns.isEmpty()) {
            return null;
        }
        return new InsertStatementSpec(tableName, columns);
    }

    /** 去除識別字外層 ` 或 " */
    private static String normalizeSqlIdentifier(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.charAt(0) == '`' && t.charAt(t.length() - 1) == '`') {
            return t.substring(1, t.length() - 1).replace("``", "`");
        }
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
            return t.substring(1, t.length() - 1).replace("\"\"", "\"");
        }
        return t;
    }

    /** 逐條執行 SQL 語句 */
    private static void executeSqlStatements(Connection connection, List<String> statements, Path sqlFilePath)
            throws Exception {
        if (statements.isEmpty()) {
            throw new IllegalStateException("SQL 檔案沒有可執行語句：" + sqlFilePath.toAbsolutePath());
        }

        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    /** 讀取 SQL 檔並切分語句 */
    private static List<String> loadSqlStatements(Path sqlFilePath) throws Exception {
        if (!Files.exists(sqlFilePath)) {
            throw new IllegalStateException("找不到 SQL 檔案：" + sqlFilePath.toAbsolutePath());
        }
        return splitSqlStatements(Files.readString(sqlFilePath, StandardCharsets.UTF_8));
    }

    /** 以分號切分 SQL 語句 */
    private static List<String> splitSqlStatements(String rawSql) {
        List<String> statements = new ArrayList<>();
        for (String part : rawSql.split(";")) {
            String statement = part.trim();
            if (!statement.isBlank()) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /** 判斷是否為 JDBC 連線層級錯誤（SQLState 08xxx） */
    private static boolean isConnectivityError(SQLException ex) {
        String state = ex.getSQLState();
        return state != null && state.startsWith("08");
    }

    /** 由 active profiles 決定匯入目標資料庫 */
    private static ImportTarget resolveTarget(String activeProfiles) {
        if (ScriptProfileUtil.containsProfile(activeProfiles, "mysql")) {
            return mysqlTarget();
        }
        if (ScriptProfileUtil.containsProfile(activeProfiles, "h2")) {
            return h2Target();
        }
        throw new IllegalStateException(
                "不支援的 SPRING_PROFILES_ACTIVE：" + activeProfiles + "（僅支援 mysql 或 h2）");
    }

    /** 是否為保留表（大小寫不敏感） */
    private static boolean isExcludedTable(String tableName) {
        return EXCLUDED_TABLES.contains(tableName.toLowerCase(Locale.ROOT));
    }

    /** MySQL 識別字引用 */
    private static String quoteMysqlIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /** H2 識別字引用 */
    private static String quoteH2Identifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /** 統一組裝錯誤訊息 */
    private static String formatFailureMessage(String targetLabel, String message) {
        return targetLabel + " 失敗：" + message;
    }

    /** 讀系統環境變數或專案根 {@code .env}，空值回退預設 */
    private static String env(String key, String fallback) {
        return ScriptRuntimeEnv.lookup(key, fallback);
    }

    /** 建立 MySQL 匯入目標配置 */
    private static ImportTarget mysqlTarget() {
        String dbHost = env("DB_HOST", DEFAULT_MYSQL_HOST);
        String dbPort = env("DB_PORT", DEFAULT_MYSQL_PORT);
        String dbName = env("DB_NAME", DEFAULT_MYSQL_DB);
        String dbUser = env("DB_USERNAME", DEFAULT_MYSQL_USER);
        String dbPassword = env("DB_PASSWORD", DEFAULT_MYSQL_PASSWORD);
        String mysqlUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                + "?serverTimezone=Asia/Taipei&characterEncoding=utf-8&allowMultiQueries=true";
        return new ImportTarget(
                "MySQL",
                true,
                mysqlUrl,
                dbUser,
                dbPassword,
                Path.of(GENERATED_SQL_DIR, MYSQL_SQL_FILE),
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                "SET FOREIGN_KEY_CHECKS = 0",
                "SET FOREIGN_KEY_CHECKS = 1");
    }

    /** 建立 H2 匯入目標配置 */
    private static ImportTarget h2Target() {
        return new ImportTarget(
                "H2",
                false,
                DEFAULT_H2_URL,
                DEFAULT_H2_USER,
                DEFAULT_H2_PASSWORD,
                Path.of(GENERATED_SQL_DIR, H2_SQL_FILE),
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'",
                "SET REFERENTIAL_INTEGRITY FALSE",
                "SET REFERENTIAL_INTEGRITY TRUE");
    }
}
