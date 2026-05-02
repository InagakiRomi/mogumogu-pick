package com.romi.mogumogu.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 啟動後把資料庫種類、JDBC URL、active profiles 打到 log */
@Component
public class DatabaseStartupLogger implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatabaseStartupLogger.class);

	private final DataSource dataSource;
	private final Environment environment;

	/** Spring 注入 DataSource 與 Environment */
	public DatabaseStartupLogger(DataSource dataSource, Environment environment) {
		this.dataSource = dataSource;
		this.environment = environment;
	}

	/** 讀 JDBC URL、判斷種類並寫 log */
	@Override
	public void run(ApplicationArguments args) {
		String profiles = String.join(", ", environment.getActiveProfiles());
		if (profiles.isEmpty()) {
			profiles = "(default)";
		}
		String jdbcUrl;
		try (Connection connection = dataSource.getConnection()) {
			jdbcUrl = connection.getMetaData().getURL();
		} catch (SQLException ex) {
			jdbcUrl = "（無法取得：" + ex.getMessage() + "）";
			log.warn("[資料來源] 讀取 JDBC URL 失敗", ex);
		}
		String kind = resolveDatabaseKind(jdbcUrl);
		log.info("[資料來源] 目前連線：{}｜作用中 profiles：{}", kind, profiles);
		log.info("[資料來源] JDBC URL：{}", jdbcUrl);
	}

	/** 從 JDBC URL 辨識 MySQL 或 H2 */
	private static String resolveDatabaseKind(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.startsWith("（")) {
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
