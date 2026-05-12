package com.romi.mogumogu.scripts;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.romi.mogumogu.logging.JulLoggerFactory;
import com.romi.mogumogu.MogumoguApplication;

public class ResetDb {

    private static final Logger log = new JulLoggerFactory().printToolLog();

    private static final String DEFAULT_DB_NAME = "mogumogu";
    private static final String DEFAULT_MYSQL_HOST = "localhost";
    private static final String DEFAULT_MYSQL_PORT = "3306";
    private static final String DEFAULT_MYSQL_USER = "root";

    /** 固定使用檔案型 H2，方便本機檢查重建後資料 */
    private static final String H2_URL = "jdbc:h2:file:./data/mogumogu;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=0";

    /** 依 profile 決定重建 MySQL 或 H2，最後啟動 Spring Boot */
    public static void main(String[] args) throws Exception {
        log.info("ResetDb 開始");
        Map<String, String> env = new HashMap<>(System.getenv());
        ScriptRuntimeEnv.mergeDotEnvInto(env);

        if (ScriptProfileUtil.containsProfile(env.getOrDefault("SPRING_PROFILES_ACTIVE", ""), "mysql")) {
            resetMysql(env);
            return;
        }
        resetH2(env);
    }

    /** 重建 MySQL 資料庫後，以 mysql profile 啟動應用程式 */
    private static void resetMysql(Map<String, String> env) throws Exception {
        String dbName = env.getOrDefault("DB_NAME", DEFAULT_DB_NAME);
        String mysqlHost = env.getOrDefault("DB_HOST", DEFAULT_MYSQL_HOST).trim();
        String dbPort = env.get("DB_PORT");
        String mysqlPort = (dbPort != null && !dbPort.isBlank()) ? dbPort.trim() : DEFAULT_MYSQL_PORT;
        String mysqlUser = env.getOrDefault("DB_USERNAME", DEFAULT_MYSQL_USER).trim();
        String mysqlPassword = env.getOrDefault("DB_PASSWORD", "").trim();

        String sql = "DROP DATABASE IF EXISTS " + dbName + "; CREATE DATABASE " + dbName
                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";

        List<String> mysqlCommand = new ArrayList<>();
        mysqlCommand.add("mysql");
        mysqlCommand.add("-h");
        mysqlCommand.add(mysqlHost);
        mysqlCommand.add("-P");
        mysqlCommand.add(mysqlPort);
        mysqlCommand.add("-u");
        mysqlCommand.add(mysqlUser);
        mysqlCommand.add("-e");
        mysqlCommand.add(sql);

        ProcessBuilder mysqlProcessBuilder = new ProcessBuilder(mysqlCommand)
                .directory(new File("."))
                .inheritIO();
        mysqlProcessBuilder.environment().putAll(env);
        if (!mysqlPassword.isEmpty()) {
            mysqlProcessBuilder.environment().put("MYSQL_PWD", mysqlPassword);
        }
        Process process = mysqlProcessBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", mysqlCommand));
        }

        runSpringBootWithProfile("mysql", null);
    }

    /** 清除 H2 檔案後，以 h2 profile 啟動應用程式 */
    private static void resetH2(Map<String, String> env) throws Exception {
        deleteH2DataFiles();
        runSpringBootWithProfile("h2", "--spring.datasource.url=" + H2_URL);
    }

    /** 直接啟動主程式，行為與 IDE 按三角形一致 */
    private static void runSpringBootWithProfile(String profile, String additionalArgument)
            throws Exception {
        List<String> springArgs = new ArrayList<>();
        springArgs.add("--spring.profiles.active=" + profile);
        if (additionalArgument != null && !additionalArgument.isBlank()) {
            springArgs.add(additionalArgument);
        }
        MogumoguApplication.main(springArgs.toArray(String[]::new));
    }

    /** 刪除 H2 檔案型資料庫（mogumogu*） */
    private static void deleteH2DataFiles() throws Exception {
        Path dataDir = Path.of("data");
        if (!Files.isDirectory(dataDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "mogumogu*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }

}
