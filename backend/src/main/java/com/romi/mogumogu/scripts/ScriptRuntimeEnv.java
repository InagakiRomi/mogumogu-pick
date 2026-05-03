package com.romi.mogumogu.scripts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 讀取專案根目錄 .env，供 Maven exec:java 等腳本與 ProcessBuilder 補齊環境變數 */
public final class ScriptRuntimeEnv {

    private static volatile Map<String, String> dotEnvCache;
    private static volatile boolean dotEnvLoaded;

    /** 工具類，禁止實例化 */
    private ScriptRuntimeEnv() {
    }

    /** 優先非空白系統環境變數，其次 .env 同鍵，最後預設值 */
    public static String lookup(String key, String defaultValue) {
        String fromOs = System.getenv(key);
        if (fromOs != null && !fromOs.isBlank()) {
            return fromOs;
        }
        String fromFile = dotEnvMap().get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }
        return defaultValue;
    }

    /** 將根目錄 .env 鍵值覆寫合入 env（無檔案則略過） */
    public static void mergeDotEnvInto(Map<String, String> env) throws IOException {
        if (!Files.isRegularFile(Path.of(".env"))) {
            return;
        }
        for (var entry : readDotEnvFile().entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
    }

    /** 惰性載入並快取 .env 內容 */
    private static Map<String, String> dotEnvMap() {
        if (!dotEnvLoaded) {
            synchronized (ScriptRuntimeEnv.class) {
                if (!dotEnvLoaded) {
                    try {
                        dotEnvCache = readDotEnvFile();
                    } catch (IOException ex) {
                        dotEnvCache = Collections.emptyMap();
                    }
                    dotEnvLoaded = true;
                }
            }
        }
        return dotEnvCache;
    }

    /** 解析根目錄 .env 成鍵值對（略過註解與空行） */
    private static Map<String, String> readDotEnvFile() throws IOException {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
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
            String value = normalizeEnvValue(line.substring(delimiterIndex + 1).trim());
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    /** 去除成對雙引號或單引號包住的值 */
    private static String normalizeEnvValue(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
