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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportGeneratedSql {
    /** ExcelToSql 輸出的 SQL 目錄；MySQL/H2 都從這裡讀取 */
    private static final String GENERATED_SQL_DIR = "sql/generated";
    /** MySQL 匯入來源檔名 */
    private static final String MYSQL_SQL_FILE = "data-mysql.sql";
    /** H2 匯入來源檔名 */
    private static final String H2_SQL_FILE = "data-h2.sql";

    /** 未設定 DB_HOST 時使用的 MySQL 主機 */
    private static final String DEFAULT_MYSQL_HOST = "localhost";
    /** 未設定 DB_PORT 時使用的 MySQL 連線埠 */
    private static final String DEFAULT_MYSQL_PORT = "3306";
    /** 未設定 DB_NAME 時使用的 MySQL 資料庫名稱 */
    private static final String DEFAULT_MYSQL_DB = "mogumogu";
    /** 未設定 DB_USERNAME 時使用的 MySQL 帳號 */
    private static final String DEFAULT_MYSQL_USER = "root";
    /** 未設定 DB_PASSWORD 時使用的 MySQL 密碼 */
    private static final String DEFAULT_MYSQL_PASSWORD = "";

    /** 固定使用檔案型 H2，讓重建資料能保留在 ./data 供本機檢查 */
    private static final String DEFAULT_H2_URL =
        "jdbc:h2:file:./data/mogumogu;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    /** H2 預設帳號（未額外覆寫時沿用） */
    private static final String DEFAULT_H2_USER = "sa";
    /** H2 預設密碼（本機開發通常為空） */
    private static final String DEFAULT_H2_PASSWORD = "";

    /** 清空資料時需保留的系統表，避免破壞 migration 歷史 */
    private static final Set<String> EXCLUDED_TABLES = Set.of("flyway_schema_history");
    /** 僅接受 generated SQL 會產生的 INSERT 樣式，避免誤判任意 SQL */
    private static final Pattern SIMPLE_INSERT_PATTERN = Pattern.compile(
        "(?is)^INSERT\\s+INTO\\s+([`\"]?[A-Za-z0-9_]+[`\"]?)\\s*\\(([^)]*)\\)\\s+VALUES\\b"
    );

    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();

        // MySQL/H2 分開執行，單邊失敗時另一邊仍可完成
        runMysqlImport(failures);
        runH2Import(failures);

        // 全部流程跑完再彙總失敗，讓批次腳本可一次看完所有問題
        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                "匯入完成，但有失敗項目：\n - " + String.join("\n - ", failures)
            );
        }

        System.out.println("MySQL、H2 匯入皆成功。");
    }

    /** 匯入 MySQL：驗證 generated SQL、清空目標表，再重新寫入資料 */
    private static void runMysqlImport(List<String> failures) {
        String dbHost = env("DB_HOST", DEFAULT_MYSQL_HOST);
        String dbPort = env("DB_PORT", DEFAULT_MYSQL_PORT);
        String dbName = env("DB_NAME", DEFAULT_MYSQL_DB);
        String dbUser = env("DB_USERNAME", DEFAULT_MYSQL_USER);
        String dbPassword = env("DB_PASSWORD", DEFAULT_MYSQL_PASSWORD);

        String mysqlUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
            + "?serverTimezone=Asia/Taipei&characterEncoding=utf-8&allowMultiQueries=true";

        Path mysqlSqlPath = Path.of(GENERATED_SQL_DIR, MYSQL_SQL_FILE);

        try (Connection connection = DriverManager.getConnection(mysqlUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            // 先驗證 SQL 與當前資料庫 schema 相容，避免清空後才發現欄位不一致
            validateGeneratedSqlAgainstConnection(connection, mysqlSqlPath, "MySQL", true);
            // 先清空再匯入，確保資料庫內容完全由 generated SQL 決定
            clearMysqlTables(connection);
            executeSqlFile(connection, mysqlSqlPath);
            connection.commit();
            System.out.println("[MySQL] 清空並匯入完成：" + mysqlSqlPath.toAbsolutePath());
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            // 連線層級問題視為可略過，讓 H2 匯入仍可往下執行
            if (isConnectivityError(ex)) {
                System.err.println("[警告] MySQL 連線失敗，已略過 MySQL 匯入，改繼續執行 H2。原因：" + message);
            } else {
                System.err.println("[MySQL] 匯入失敗：" + message);
            }
            failures.add("MySQL 失敗：" + message);
        } catch (Exception ex) {
            System.err.println("[MySQL] 匯入失敗：" + ex.getMessage());
            failures.add("MySQL 失敗：" + ex.getMessage());
        }
    }

    /** 匯入 H2：流程與 MySQL 對齊，確保兩種方言的資料一致性 */
    private static void runH2Import(List<String> failures) {
        String h2Url = DEFAULT_H2_URL;
        String h2User = DEFAULT_H2_USER;
        String h2Password = DEFAULT_H2_PASSWORD;

        Path h2SqlPath = Path.of(GENERATED_SQL_DIR, H2_SQL_FILE);

        try (Connection connection = DriverManager.getConnection(h2Url, h2User, h2Password)) {
            connection.setAutoCommit(false);
            // 先確認 SQL 能對應目前 schema，再做清空與匯入
            validateGeneratedSqlAgainstConnection(connection, h2SqlPath, "H2", false);
            // 與 MySQL 採相同策略：清空後重匯，避免殘留舊資料
            clearH2Tables(connection);
            executeSqlFile(connection, h2SqlPath);
            connection.commit();
            System.out.println("[H2] 清空並匯入完成：" + h2SqlPath.toAbsolutePath());
        } catch (Exception ex) {
            System.err.println("[H2] 匯入失敗：" + ex.getMessage());
            failures.add("H2 失敗：" + ex.getMessage());
        }
    }

    /** 清空 MySQL 實體表；暫停 FK 檢查以避免 TRUNCATE 受相依順序影響 */
    private static void clearMysqlTables(Connection connection) throws SQLException {
        // 讀出目前 schema 的 BASE TABLE，避免動到 view/system object
        List<String> tableNames = loadTableNames(
            connection,
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'"
        );

        try (Statement statement = connection.createStatement()) {
            // 關閉 FK 檢查，讓 TRUNCATE 不受外鍵相依順序限制
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tableName : tableNames) {
                if (isExcludedTable(tableName)) {
                    continue;
                }
                statement.execute("TRUNCATE TABLE " + quoteMysqlIdentifier(tableName));
            }
            // 還原 FK 檢查，避免後續連線狀態異常
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    /** 清空 H2 PUBLIC schema 下的實體表，保留 migration 系統表 */
    private static void clearH2Tables(Connection connection) throws SQLException {
        // 僅處理 PUBLIC schema 的 BASE TABLE，避免誤清其他物件
        List<String> tableNames = loadTableNames(
            connection,
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'"
        );

        try (Statement statement = connection.createStatement()) {
            // 暫停參照完整性，避免外鍵阻擋 TRUNCATE
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            for (String tableName : tableNames) {
                if (isExcludedTable(tableName)) {
                    continue;
                }
                statement.execute("TRUNCATE TABLE " + quoteH2Identifier(tableName));
            }
            // 還原參照完整性，維持資料庫正常約束行為
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    /** 依查詢語句載入表名，讓 MySQL/H2 的清空流程共用同一段邏輯 */
    private static List<String> loadTableNames(Connection connection, String sql) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        // 使用傳入 SQL 讀取第 1 欄表名
        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
            }
        }
        return tableNames;
    }

    /** 單一 INSERT 語句解析結果（表名、欄位順序） */
    private record InsertStatementSpec(String tableName, List<String> columnNames) {}

    /**
     * 在清空／執行前比對產生檔中的 INSERT 與目前連線的資料庫結構（表是否存在、欄位名是否都存在）。
     *
     * @throws IllegalStateException 有不符處時帶入繁中說明
     */
    private static void validateGeneratedSqlAgainstConnection(
        Connection connection,
        Path sqlFilePath,
        String dialectLabel,
        boolean mysql
    ) throws Exception {
        if (!Files.exists(sqlFilePath)) {
            throw new IllegalStateException("找不到 SQL 檔案：" + sqlFilePath.toAbsolutePath());
        }

        String rawSql = Files.readString(sqlFilePath, StandardCharsets.UTF_8);
        List<String> statements = splitSqlStatements(rawSql);
        List<String> errors = new ArrayList<>();

        // 逐條檢查 INSERT 格式、表名、欄位是否可在目標資料庫找到
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
                dialectLabel + "：產生的 SQL 與目前資料庫結構不符：\n - " + String.join("\n - ", errors)
            );
        }
    }

    private static List<String> validateInsertSpecAgainstDb(
        Connection connection,
        InsertStatementSpec spec,
        boolean mysql,
        int statementIndex
    ) {
        List<String> errors = new ArrayList<>();
        String table = spec.tableName();
        List<String> fileCols = spec.columnNames();

        List<String> dbColumns;
        try {
            // 先讀出目標資料表欄位，後續用不分大小寫方式比對
            dbColumns = loadDbColumnNames(connection, table, mysql);
        } catch (SQLException ex) {
            errors.add(
                "第 "
                    + statementIndex
                    + " 條（表「"
                    + table
                    + "」）：查詢資料欄失敗："
                    + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
            );
            return errors;
        }

        if (dbColumns.isEmpty()) {
            errors.add(
                "第 "
                    + statementIndex
                    + " 條：資料表「"
                    + table
                    + "」在目前資料庫中不存在。"
            );
            return errors;
        }

        // 以 lower-case 集合處理不同資料庫大小寫規則差異
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
                        + String.join(", ", dbColumns)
                );
            }
        }
        return errors;
    }

    /** 查詢資料表欄位清單（依 ordinal_position），用於驗證 INSERT 欄位相容性 */
    private static List<String> loadDbColumnNames(Connection connection, String tableName, boolean mysql)
        throws SQLException {
        String schemaCondition =
            mysql ? "table_schema = DATABASE()" : "table_schema = 'PUBLIC'";
        String sql =
            "SELECT column_name FROM information_schema.columns WHERE "
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

    /** 解析 INSERT INTO ... (col1, col2, ...) VALUES ...，僅取表名與欄位順序 */
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

    /** 去除 SQL 識別字外層引號（`...` 或 "..."），保留實際名稱 */
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

    /** 讀取 SQL 檔後逐條執行；檔案不存在或無語句時直接拋錯 */
    private static void executeSqlFile(Connection connection, Path sqlFilePath) throws Exception {
        if (!Files.exists(sqlFilePath)) {
            throw new IllegalStateException("找不到 SQL 檔案：" + sqlFilePath.toAbsolutePath());
        }

        // 讀檔後切成多條語句，逐條執行
        String rawSql = Files.readString(sqlFilePath, StandardCharsets.UTF_8);
        List<String> statements = splitSqlStatements(rawSql);
        if (statements.isEmpty()) {
            throw new IllegalStateException("SQL 檔案沒有可執行語句：" + sqlFilePath.toAbsolutePath());
        }

        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    /** 依 ';' 切分 generated SQL；目前檔案內容僅包含 INSERT 語句 */
    private static List<String> splitSqlStatements(String rawSql) {
        List<String> statements = new ArrayList<>();
        // 目前 generated SQL 無程序語法或觸發器，分號切分已足夠
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

    /** 是否為需保留的系統表（大小寫不敏感） */
    private static boolean isExcludedTable(String tableName) {
        return EXCLUDED_TABLES.contains(tableName.toLowerCase());
    }

    /** 以 MySQL 反引號包裹識別字，避免名稱含保留字或特殊字元 */
    private static String quoteMysqlIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /** 以 H2 雙引號包裹識別字，確保大小寫與特殊字元可正確處理 */
    private static String quoteH2Identifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /** 讀取環境變數；空值視同未設定並回退到預設值 */
    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
