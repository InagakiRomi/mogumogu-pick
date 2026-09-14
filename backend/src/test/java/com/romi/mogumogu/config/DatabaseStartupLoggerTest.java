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
    @DisplayName("run() and Environment lookup flow")
    class RunAndEnvironmentFlowTests {

        @Test
        @DisplayName("Prefers spring.datasource.url when present and completes")
        void run_shouldPreferSpringDatasourceUrl_whenUrlIsPresent() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("jdbc:mysql://localhost:3306/mogu");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyOnlyDatasourceUrlQueried(environment);
        }

        @Test
        @DisplayName("Falls back to spring.datasource.jdbc-url when url is blank")
        void run_shouldFallbackToJdbcUrl_whenUrlIsBlank() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("   ");
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn("jdbc:h2:mem:testdb");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("Still completes when both datasource properties are missing (unknown database)")
        void run_shouldStillComplete_whenBothDatasourcePropertiesMissing() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn(null);
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn(null);

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("Still completes when url is empty and jdbc-url is blank (unknown database)")
        void run_shouldStillComplete_whenUrlEmptyAndJdbcUrlBlank() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("");
            when(environment.getProperty(DATASOURCE_JDBC_URL_KEY)).thenReturn("   ");

            assertRunDoesNotThrow(environment, EMPTY_ARGS);
            verifyUrlAndJdbcUrlQueried(environment);
        }

        @Test
        @DisplayName("Still completes when ApplicationArguments is null (the argument is unused)")
        void run_shouldNotThrow_whenApplicationArgumentsIsNull() {
            Environment environment = mock(Environment.class);
            when(environment.getProperty(DATASOURCE_URL_KEY)).thenReturn("jdbc:mysql://localhost:3306/mogu");

            assertRunDoesNotThrow(environment, null);
            verifyOnlyDatasourceUrlQueried(environment);
        }

        @Test
        @DisplayName("run throws NullPointerException when Environment is null")
        void run_shouldThrowNullPointerException_whenEnvironmentIsNull() {
            assertThrows(NullPointerException.class, () -> new DatabaseStartupLogger(null).run(EMPTY_ARGS));
        }

        @Test
        @DisplayName("run propagates exceptions from reading spring.datasource.url")
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
        @DisplayName("run propagates exceptions from reading spring.datasource.jdbc-url")
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
    @DisplayName("resolveDatabaseKind(jdbcUrl) database kind detection")
    class ResolveDatabaseKindTests {

        @Test
        @DisplayName("jdbc:mysql prefix is detected as MySQL")
        void resolveDatabaseKind_shouldReturnMySql_whenStartsWithJdbcMysql() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("jdbc:mysql://localhost:3306/app"));
        }

        @Test
        @DisplayName("A URL containing :mysql: is detected as MySQL")
        void resolveDatabaseKind_shouldReturnMySql_whenContainsMysqlSegment() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("prefix:mysql:segment"));
        }

        @Test
        @DisplayName("jdbc:h2 prefix is detected as H2")
        void resolveDatabaseKind_shouldReturnH2_whenStartsWithJdbcH2() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("jdbc:h2:file:./data/mogu"));
        }

        @Test
        @DisplayName("A URL containing :h2: is detected as H2")
        void resolveDatabaseKind_shouldReturnH2_whenContainsH2Segment() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("abc:h2:def"));
        }

        @Test
        @DisplayName("Mixed-case URLs still detect MySQL")
        void resolveDatabaseKind_shouldBeCaseInsensitive_forMysql() throws Exception {
            assertEquals("MySQL", invokeResolveDatabaseKind("JDBC:MySQL://localhost:3306/app"));
        }

        @Test
        @DisplayName("Mixed-case URLs still detect H2")
        void resolveDatabaseKind_shouldBeCaseInsensitive_forH2() throws Exception {
            assertEquals("H2", invokeResolveDatabaseKind("JDBC:H2:MEM:testdb"));
        }

        @Test
        @DisplayName("Unsupported JDBC URLs return the unknown label")
        void resolveDatabaseKind_shouldReturnUnknown_forUnsupportedJdbcUrl() throws Exception {
            assertEquals("未知", invokeResolveDatabaseKind("jdbc:postgresql://localhost:5432/app"));
        }

        @Test
        @DisplayName("A blank string returns the unknown label")
        void resolveDatabaseKind_shouldReturnUnknown_forBlankInput() throws Exception {
            assertEquals("未知", invokeResolveDatabaseKind("   "));
        }

        @Test
        @DisplayName("null returns the unknown label")
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
