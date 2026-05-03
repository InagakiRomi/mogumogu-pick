package com.romi.mogumogu.scripts;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Comparator;
import java.util.stream.Stream;

import com.romi.mogumogu.logging.JulLoggerFactory;

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
        Map<String, String> env = loadRuntimeEnv();

        if (containsMysqlProfile(env.getOrDefault("SPRING_PROFILES_ACTIVE", ""))) {
            resetMysql(env);
            return;
        }
        resetH2(env);
    }

    /** 支援逗號分隔 profile（例如 dev,mysql） */
    private static boolean containsMysqlProfile(String activeProfiles) {
        for (String profile : activeProfiles.split(",")) {
            if ("mysql".equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 重建 MySQL 資料庫後，以 mysql profile 啟動應用程式 */
    private static void resetMysql(Map<String, String> env) throws Exception {
        String dbName = env.getOrDefault("DB_NAME", DEFAULT_DB_NAME);
        String mysqlHost = env.getOrDefault("DB_HOST", DEFAULT_MYSQL_HOST).trim();
        String mysqlPort = env.getOrDefault("DB_PORT", DEFAULT_MYSQL_PORT).trim();
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

        ProcessBuilder mysqlProcessBuilder = baseProcess(mysqlCommand, env);
        if (!mysqlPassword.isEmpty()) {
            mysqlProcessBuilder.environment().put("MYSQL_PWD", mysqlPassword);
        }
        runOrThrow(mysqlProcessBuilder, mysqlCommand);

        runSpringBootWithProfile("mysql", null, env);
    }

    /** 清除 H2 檔案後，以 h2 profile 啟動應用程式 */
    private static void resetH2(Map<String, String> env) throws Exception {
        deleteH2DataFiles();
        runSpringBootWithProfile("h2", "--spring.datasource.url=" + H2_URL, env);
    }

    /** 合併系統環境變數與 .env，讓純 java 執行也能吃到專案設定 */
    private static Map<String, String> loadRuntimeEnv() throws Exception {
        Map<String, String> mergedEnv = new HashMap<>(System.getenv());
        ScriptRuntimeEnv.mergeDotEnvInto(mergedEnv);
        return mergedEnv;
    }

    /** 建立可繼承 I/O 且帶環境變數的 ProcessBuilder */
    private static ProcessBuilder baseProcess(List<String> command, Map<String, String> env) {
        ProcessBuilder processBuilder = new ProcessBuilder(command).directory(new File(".")).inheritIO();
        processBuilder.environment().putAll(env);
        return processBuilder;
    }

    /** 使用可執行 jar 啟動 Spring Boot，避免依賴 mvnw.cmd */
    private static void runSpringBootWithProfile(String profile, String additionalArgument, Map<String, String> env)
            throws Exception {
        Path jarPath = resolveSpringBootJar(Path.of("target"));
        List<String> javaCommand = new ArrayList<>();
        javaCommand.add("java");
        javaCommand.add("-jar");
        javaCommand.add(jarPath.toString());
        javaCommand.add("--spring.profiles.active=" + profile);
        if (additionalArgument != null && !additionalArgument.isBlank()) {
            javaCommand.add(additionalArgument);
        }
        runOrThrow(baseProcess(javaCommand, env), javaCommand);
    }

    /** 從 target 找可執行 jar，優先使用最新修改時間 */
    private static Path resolveSpringBootJar(Path targetDir) throws Exception {
        if (!Files.isDirectory(targetDir)) {
            throw new IllegalStateException("找不到 target 目錄，請先執行建置產生可執行 jar。");
        }

        try (Stream<Path> stream = Files.list(targetDir)) {
            Optional<Path> jarPath = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-plain.jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-sources.jar"))
                    .sorted(Comparator.comparingLong((Path jar) -> jar.toFile().lastModified()).reversed())
                    .findFirst();
            if (jarPath.isPresent()) {
                return jarPath.get();
            }
        }
        throw new IllegalStateException("找不到可執行 jar，請先建置專案。");
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

    /** 執行外部命令，非 0 直接拋錯附帶命令摘要 */
    private static void runOrThrow(ProcessBuilder processBuilder, List<String> commandPreview) throws Exception {
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", commandPreview));
        }
    }
}
