package com.romi.mogumogu.scripts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.romi.mogumogu.logging.JulLoggerFactory;

public class ResetDb {

    private static final Logger log = new JulLoggerFactory().printToolLog();
    /** 本腳本支援在未設定環境變數時，仍可用本機預設值快速重建資料庫 */
    private static final String DEFAULT_DB_NAME = "mogumogu";
    /** 預設 MySQL 主機位置 */
    private static final String DEFAULT_MYSQL_HOST = "localhost";
    /** 預設 MySQL 連線埠 */
    private static final String DEFAULT_MYSQL_PORT = "3306";
    /** 預設 MySQL 使用者 */
    private static final String DEFAULT_MYSQL_USER = "root";
    /** 固定使用檔案型 H2，重建後資料會落在 ./data 下，方便本機開發檢查 */
    private static final String H2_URL = "jdbc:h2:file:./data/mogumogu;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=0";

    public static void main(String[] args) throws Exception {
        log.info("ResetDb 開始");
        Map<String, String> env = loadRuntimeEnv();
        String activeProfiles = env.getOrDefault("SPRING_PROFILES_ACTIVE", "");

        // 若啟用 mysql profile，優先走 MySQL 重建流程；否則預設走 H2
        if (containsMysqlProfile(activeProfiles)) {
            resetMysql(env);
            return;
        }

        resetH2(env);
    }

    /** 支援逗號分隔 profile，判斷是否包含 mysql */
    private static boolean containsMysqlProfile(String activeProfiles) {
        // 支援 "dev,mysql" 這類逗號分隔 profile 寫法
        for (String profile : activeProfiles.split(",")) {
            if ("mysql".equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 重建 MySQL 資料庫，並啟動 Spring Boot 進行 migration/seed */
    private static void resetMysql(Map<String, String> env) throws Exception {
        String dbName = env.getOrDefault("DB_NAME", DEFAULT_DB_NAME);
        String mysqlHost = env.getOrDefault("DB_HOST", DEFAULT_MYSQL_HOST);
        String mysqlPort = env.getOrDefault("DB_PORT", DEFAULT_MYSQL_PORT);
        String mysqlUser = env.getOrDefault("DB_USERNAME", DEFAULT_MYSQL_USER);
        String mysqlPassword = env.getOrDefault("DB_PASSWORD", "");
        // 先刪後建，確保 schema 能完全重置為乾淨狀態
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

        ProcessBuilder mysqlProcessBuilder = new ProcessBuilder(mysqlCommand);
        mysqlProcessBuilder.directory(new File("."));
        mysqlProcessBuilder.inheritIO();
        mysqlProcessBuilder.environment().putAll(env);
        if (!mysqlPassword.isEmpty()) {
            // 有密碼時透過環境變數傳遞，避免直接把密碼露在命令列參數中
            mysqlProcessBuilder.environment().put("MYSQL_PWD", mysqlPassword);
        }
        runOrThrow(mysqlProcessBuilder, mysqlCommand);

        // 啟動 Spring Boot 觸發 migration/seed，讓新資料庫立即可用
        ProcessBuilder springBootProcessBuilder = new ProcessBuilder("mvnw.cmd", "spring-boot:run",
                "-Dspring-boot.run.profiles=mysql")
                .directory(new File("."))
                .inheritIO();
        springBootProcessBuilder.environment().putAll(env);
        runOrThrow(
                springBootProcessBuilder,
                List.of("mvnw.cmd", "spring-boot:run", "-Dspring-boot.run.profiles=mysql"));
    }

    /** 清除舊 H2 檔案後，以固定 URL 啟動 Spring Boot 重建資料 */
    private static void resetH2(Map<String, String> env) throws Exception {
        // 清除既有 H2 檔案；找不到檔案時不視為錯誤，讓腳本可重複執行
        runOrThrow(
                new ProcessBuilder(
                        "powershell",
                        "-NoProfile",
                        "-Command",
                        "$ErrorActionPreference='SilentlyContinue'; Remove-Item -Path '.\\data\\mogumogu*' -Force; exit 0")
                        .directory(new File(".")).inheritIO(),
                List.of("powershell", "-NoProfile", "-Command", "Remove-Item .\\data\\mogumogu*"));

        // 以固定 URL 啟動，確保 H2 資料落在預期路徑而非暫存資料庫
        ProcessBuilder springBootProcessBuilder = new ProcessBuilder(
                "mvnw.cmd",
                "spring-boot:run",
                "-Dspring-boot.run.profiles=h2",
                "-Dspring-boot.run.arguments=--spring.datasource.url=" + H2_URL).directory(new File(".")).inheritIO();
        springBootProcessBuilder.environment().putAll(env);
        runOrThrow(
                springBootProcessBuilder,
                List.of("mvnw.cmd", "spring-boot:run", "-Dspring-boot.run.profiles=h2"));
    }

    /** 合併系統環境變數與 .env，讓直接執行 java 也能帶到專案設定 */
    private static Map<String, String> loadRuntimeEnv() throws Exception {
        Map<String, String> mergedEnv = new HashMap<>(System.getenv());
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return mergedEnv;
        }

        for (String rawLine : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int delimiterIndex = line.indexOf('=');
            if (delimiterIndex <= 0) {
                continue;
            }

            String key = line.substring(0, delimiterIndex).trim();
            String value = line.substring(delimiterIndex + 1).trim();
            if (!key.isEmpty()) {
                mergedEnv.put(key, value);
            }
        }
        return mergedEnv;
    }

    /** 執行外部命令，若失敗則拋出例外並附帶命令摘要 */
    private static void runOrThrow(ProcessBuilder processBuilder, List<String> commandPreview) throws Exception {
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // 保留可讀命令摘要，失敗時能快速定位是哪個步驟出錯
            throw new IllegalStateException("Command failed: " + String.join(" ", commandPreview));
        }
    }
}
