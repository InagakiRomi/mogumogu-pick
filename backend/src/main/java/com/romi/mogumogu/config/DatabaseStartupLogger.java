package com.romi.mogumogu.config;

import java.util.Locale;
import java.util.logging.Logger;

import com.romi.mogumogu.logging.JulLoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 啟動後將資料庫種類寫入 log */
@Component
public class DatabaseStartupLogger implements ApplicationRunner {

	private static final Logger log = new JulLoggerFactory().printMainLog();

	private final Environment environment;

	/** 注入 Environment 以讀取 spring.datasource 相關設定 */
	public DatabaseStartupLogger(Environment environment) {
		this.environment = environment;
	}

	/** 啟動完成時輸出目前連線種類 */
	@Override
	public void run(ApplicationArguments args) {
		String jdbcUrl = resolveJdbcUrlFromEnvironment();
		String kind = resolveDatabaseKind(jdbcUrl);
		log.info(String.format("目前連線：%s", kind));
	}

	/** 讀取 spring.datasource.url 或 spring.datasource.jdbc-url */
	private String resolveJdbcUrlFromEnvironment() {
		String url = environment.getProperty("spring.datasource.url");
		if (url != null && !url.isBlank()) {
			return url;
		}
		String jdbcUrl = environment.getProperty("spring.datasource.jdbc-url");
		return jdbcUrl != null && !jdbcUrl.isBlank() ? jdbcUrl : "";
	}

	/** 依 JDBC URL 回傳 MySQL、H2 或未知 */
	private static String resolveDatabaseKind(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			return "未知";
		}
		String lower = jdbcUrl.toLowerCase(Locale.ROOT);
		if (lower.startsWith("jdbc:mysql:") || lower.contains(":mysql:")) {
			return "MySQL";
		}
		if (lower.startsWith("jdbc:h2:") || lower.contains(":h2:")) {
			return "H2";
		}
		return "未知";
	}
}
