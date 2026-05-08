package com.romi.mogumogu.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DatabaseStartupLoggerTest {
    private static final DefaultApplicationArguments EMPTY_ARGS = new DefaultApplicationArguments(new String[]{});

    private static final String DATASOURCE_URL_KEY = "spring.datasource.url";
    private static final String DATASOURCE_JDBC_URL_KEY = "spring.datasource.jdbc-url";

    @Nested
    @DisplayName("run() 與 Environment 讀取流程")
    class RunAndEnvironmentFlowTests {

        @Test
        @DisplayName("有 spring.datasource.url 時，優先使用 url 並完成執行")
        void run_shouldPreferSpringDatasourceUrl_whenUrlIsPresent() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("jdbc:mysql://localhost:3306/mogu");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyOnlyDatasourceUrlQueried(environment);
        }

        @Test
        @DisplayName("url 為空白時，應 fallback 使用 spring.datasource.jdbc-url")
        void run_shouldFallbackToJdbcUrl_whenUrlIsBlank() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("   ");
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn("jdbc:h2:mem:testdb");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("url 與 jdbc-url 都缺失時，仍可正常執行（未知資料庫）")
        void run_shouldStillComplete_whenBothDatasourcePropertiesMissing() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn(null);
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn(null);

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("url 為空字串且 jdbc-url 為空白時，仍可正常執行（未知資料庫）")
        void run_shouldStillComplete_whenUrlEmptyAndJdbcUrlBlank() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("");
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn("   ");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("ApplicationArguments 傳入 null 時也可正常執行（參數未被使用）")
        void run_shouldNotThrow_whenApplicationArgumentsIsNull() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("jdbc:mysql://localhost:3306/mogu");

            assertRunDoesNotThrow(environment, null);
            verifyOnlyDatasourceUrlQueried(environment);
        }

        @Test
        @DisplayName("Environment 為 null 時，執行 run 應拋出 NullPointerException")
        void run_shouldThrowNullPointerException_whenEnvironmentIsNull() {
            assertThrows(NullPointerException.class, () -> new DatabaseStartupLogger(null).run(EMPTY_ARGS));
        }

        @Test
        @DisplayName("讀取 spring.datasource.url 發生例外時，run 應將例外往外拋")
        void run_shouldPropagateException_whenReadingDatasourceUrlFails() {
            Environment environment = mock(Environment.class);
            RuntimeException expected = new RuntimeException("read url failed");
            doThrow(expected).when(environment).getProperty(DATASOURCE_URL_KEY);

            RuntimeException actual = assertThrows(RuntimeException.class,
                    () -> new DatabaseStartupLogger(environment).run(EMPTY_ARGS));
            assertEquals("read url failed", actual.getMessage());

            verifyOnlyDatasourceUrlQueried(environment);
        }

        @Test
        @DisplayName("fallback 讀取 spring.datasource.jdbc-url 發生例外時，run 應將例外往外拋")
        void run_shouldPropagateException_whenReadingDatasourceJdbcUrlFails() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn(" ");
            RuntimeException expected = new RuntimeException("read jdbc-url failed");
            doThrow(expected).when(environment).getProperty(DATASOURCE_JDBC_URL_KEY);

            RuntimeException actual = assertThrows(RuntimeException.class,
                    () -> new DatabaseStartupLogger(environment).run(EMPTY_ARGS));
            assertEquals("read jdbc-url failed", actual.getMessage());

            verifyUrlAndJdbcUrlQueried(environment);
        }
    }

    @Nested
    @DisplayName("resolveDatabaseKind(jdbcUrl) 資料庫種類判斷")
    class ResolveDatabaseKindTests {

        @Test
        @DisplayName("jdbc:mysql 開頭應判定為 MySQL")
        void resolveDatabaseKind_shouldReturnMySql_whenStartsWithJdbcMysql() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("jdbc:mysql://localhost:3306/app"));
        }

        @Test
        @DisplayName("URL 含 :mysql: 片段應判定為 MySQL")
        void resolveDatabaseKind_shouldReturnMySql_whenContainsMysqlSegment() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("prefix:mysql:segment"));
        }

        @Test
        @DisplayName("jdbc:h2 開頭應判定為 H2")
        void resolveDatabaseKind_shouldReturnH2_whenStartsWithJdbcH2() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("jdbc:h2:file:./data/mogu"));
        }

        @Test
        @DisplayName("URL 含 :h2: 片段應判定為 H2")
        void resolveDatabaseKind_shouldReturnH2_whenContainsH2Segment() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("abc:h2:def"));
        }

        @Test
        @DisplayName("大小寫混用也應正確辨識 MySQL")
        void resolveDatabaseKind_shouldBeCaseInsensitive_forMysql() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("JDBC:MySQL://localhost:3306/app"));
        }

        @Test
        @DisplayName("大小寫混用也應正確辨識 H2")
        void resolveDatabaseKind_shouldBeCaseInsensitive_forH2() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("JDBC:H2:MEM:testdb"));
        }

        @Test
        @DisplayName("非 MySQL/H2 URL 應回傳 未知")
        void resolveDatabaseKind_shouldReturnUnknown_forUnsupportedJdbcUrl() throws Exception {
            assertEquals("未知", invokeResolveDatabaseKind("jdbc:postgresql://localhost:5432/app"));
        }

        @Test
        @DisplayName("空白字串應回傳 未知")
        void resolveDatabaseKind_shouldReturnUnknown_forBlankInput() throws Exception {
            assertEquals("未知", invokeResolveDatabaseKind("   "));
        }

        @Test
        @DisplayName("null 應回傳 未知")
        void resolveDatabaseKind_shouldReturnUnknown_forNullInput() throws Exception {
            assertEquals("未知", invokeResolveDatabaseKind(null));
        }
    }

    private String invokeResolveDatabaseKind(String jdbcUrl) throws Exception {
        Method method = DatabaseStartupLogger.class.getDeclaredMethod("resolveDatabaseKind", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, jdbcUrl);
    }

    private void assertRunDoesNotThrow(Environment environment, DefaultApplicationArguments args) {
        DatabaseStartupLogger logger = new DatabaseStartupLogger(environment);
        assertDoesNotThrow(() -> logger.run(args));
    }

    private void verifyOnlyDatasourceUrlQueried(Environment environment) {
        verify(environment).getProperty(DATASOURCE_URL_KEY);
        verifyNoMoreInteractions(environment);
    }

    private void verifyUrlAndJdbcUrlQueried(Environment environment) {
        verify(environment).getProperty(DATASOURCE_URL_KEY);
        verify(environment).getProperty(DATASOURCE_JDBC_URL_KEY);
        verifyNoMoreInteractions(environment);
    }
}
