package com.romi.mogumogu.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelToSql {

    private static final Logger log = LoggerFactory.getLogger(ExcelToSql.class);

    /** Excel 來源目錄名稱 */
    private static final String EXCEL_DATA_DIR = "excel-data";
    /** SQL 產物輸出目錄 */
    private static final String OUTPUT_DIR = "sql/generated";
    /** MySQL 輸出檔名 */
    private static final String MYSQL_OUTPUT_FILE = "data-mysql.sql";
    /** H2 輸出檔名 */
    private static final String H2_OUTPUT_FILE = "data-h2.sql";

    /** 欄名列（POI 索引 1） */
    private static final int HEADER_ROW_INDEX = 1;
    /** 資料起始列（POI 索引 2 起） */
    private static final int FIRST_DATA_ROW_INDEX = 2;

    /** 日期時間格式化 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 多表 INSERT 輸出順序 */
    private static final List<String> TABLE_ORDER = List.of(
        "user_groups",
        "restaurants",
        "restaurant_history",
        "dishes",
        "users"
    );

    /** SQL 方言 */
    private enum SqlDialect {
        MYSQL,
        H2
    }

    /** 標題列：欄索引與欄名 */
    private record ColumnLayout(List<Integer> columnIndexes, List<String> columnNames) {}

    public static void main(String[] args) {
        try {
            // 模組根、excel-data、輸出目錄
            Path moduleRoot = resolveModuleRoot();
            Path excelDataDir = resolveExcelDataDir(moduleRoot);
            Path outputDir = moduleRoot.resolve(OUTPUT_DIR);

            // 各表 INSERT（MySQL / H2 各一份）
            Map<String, String> mysqlTableSql = loadTableSqlFromExcels(excelDataDir, SqlDialect.MYSQL);
            Map<String, String> h2TableSql = loadTableSqlFromExcels(excelDataDir, SqlDialect.H2);
            if (mysqlTableSql.isEmpty() || h2TableSql.isEmpty()) {
                throw new IllegalStateException(
                    "未產生任何 INSERT：請確認 .xlsx 第 2 列為欄名、第 3 列起有資料"
                );
            }
            // 排序後寫入檔案
            List<String> mysqlStatements = orderSqlStatements(mysqlTableSql);
            List<String> h2Statements = orderSqlStatements(h2TableSql);
            Path mysqlOutputPath = writeSqlFile(mysqlStatements, outputDir, MYSQL_OUTPUT_FILE);
            Path h2OutputPath = writeSqlFile(h2Statements, outputDir, H2_OUTPUT_FILE);
            log.info("已寫入 MySQL SQL： {}", mysqlOutputPath.toAbsolutePath());
            log.info("已寫入 H2 SQL： {}", h2OutputPath.toAbsolutePath());
        } catch (Exception ex) {
            log.error("失敗：{}", ex.getMessage(), ex);
        }
    }

    /** 解析 Maven 模組根目錄 */
    private static Path resolveModuleRoot() {
        Path start = Path.of("").toAbsolutePath().normalize();
        // 目前目錄即模組根
        if (hasModuleLayout(start)) {
            return start;
        }
        // 子目錄 backend
        Path backend = start.resolve("backend");
        if (hasModuleLayout(backend)) {
            return backend.normalize();
        }
        // 往上找模組根
        Path dir = start;
        for (int i = 0; i < 6 && dir != null; i++) {
            if (hasModuleLayout(dir)) {
                return dir;
            }
            dir = dir.getParent();
        }
        // 找不到則用目前目錄
        return start;
    }

    /** 是否為模組根 */
    private static boolean hasModuleLayout(Path dir) {
        return Files.isRegularFile(dir.resolve("pom.xml"))
            && Files.isDirectory(dir.resolve("src/main/resources"));
    }

    /** 解析 excel-data 目錄 */
    private static Path resolveExcelDataDir(Path moduleRoot) throws IOException {
        // 模組根與上一層的 excel-data
        List<Path> candidates = new ArrayList<>();
        candidates.add(moduleRoot.resolve(EXCEL_DATA_DIR));
        Path parent = moduleRoot.getParent();
        if (parent != null) {
            candidates.add(parent.resolve(EXCEL_DATA_DIR));
        }

        Path existingButEmpty = null;
        for (Path candidate : candidates) {
            if (!Files.isDirectory(candidate)) {
                continue;
            }
            List<Path> xlsxFiles = listXlsxFiles(candidate);
            // 第一個含 .xlsx 的目錄
            if (!xlsxFiles.isEmpty()) {
                return candidate;
            }
            existingButEmpty = candidate;
        }

        // 目錄在但無檔案 / 完全找不到
        if (existingButEmpty != null) {
            throw new IllegalStateException("資料夾內沒有任何 .xlsx：" + existingButEmpty.toAbsolutePath());
        }
        throw new IllegalStateException(
            "找不到含 .xlsx 的 excel-data。已嘗試："
                + candidates.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(", "))
        );
    }

    /** 掃描 xlsx，產生各表 INSERT */
    private static Map<String, String> loadTableSqlFromExcels(Path excelDataDir, SqlDialect dialect)
        throws IOException {
        // 目錄與 xlsx 列表
        if (!Files.isDirectory(excelDataDir)) {
            throw new IllegalStateException("找不到資料夾：" + excelDataDir.toAbsolutePath());
        }
        List<Path> excelFiles = listXlsxFiles(excelDataDir);
        if (excelFiles.isEmpty()) {
            throw new IllegalStateException("沒有 .xlsx：" + excelDataDir.toAbsolutePath());
        }
        // 有 ~$ 鎖定檔則中止
        requireExcelWorkbooksClosed(excelDataDir, excelFiles);

        // 檔名＝表名，逐檔產 INSERT
        Map<String, String> tableSql = new HashMap<>();
        for (Path excelPath : excelFiles) {
            String tableName = stripExtension(excelPath.getFileName().toString());
            String insertSql = excelToInsertSql(excelPath, tableName, dialect);
            if (!insertSql.isBlank()) {
                tableSql.put(tableName, insertSql);
            }
        }
        return tableSql;
    }

    /** 列出 .xlsx（排除 ~$），檔名排序 */
    private static List<Path> listXlsxFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            // .xlsx、排除 ~$，檔名排序
            return stream
                .filter(path -> {
                    String n = path.getFileName().toString();
                    return n.toLowerCase().endsWith(".xlsx") && !n.startsWith("~$");
                })
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                .collect(Collectors.toList());
        }
    }

    /** 檢查 ~$ 鎖定檔 */
    private static void requireExcelWorkbooksClosed(Path excelDataDir, List<Path> excelFiles) {
        // 同資料夾 ~$ 檔＝可能仍開著
        List<String> openLike = new ArrayList<>();
        for (Path excelPath : excelFiles) {
            String fileName = excelPath.getFileName().toString();
            Path lockSibling = excelDataDir.resolve("~$" + fileName);
            if (Files.isRegularFile(lockSibling)) {
                openLike.add(fileName);
            }
        }
        if (openLike.isEmpty()) {
            return;
        }
        // 列出疑似未關閉的檔名
        String detail = openLike.stream().map(n -> "  - " + n).collect(Collectors.joining("\n"));
        throw new IllegalStateException(
            "下列 .xlsx 可能仍於 Excel 中開啟（同資料夾存在對應的 ~$ 鎖定檔），已中止產生 SQL。"
                + "請先儲存並完全關閉活頁簿；若已關閉，請刪除「"
                + excelDataDir.toAbsolutePath()
                + "」內以 ~$ 開頭的暫存檔後再執行。\n"
                + detail
        );
    }

    /** 單一 xlsx → INSERT（第一張工作表） */
    private static String excelToInsertSql(Path excelPath, String tableName, SqlDialect dialect)
        throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                return "";
            }
            // 第一張工作表＋公式求值
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return sheetToInsertSql(sheet, tableName, formulaEvaluator, dialect);
        }
    }

    /** 工作表 → 單條 INSERT */
    private static String sheetToInsertSql(
        Sheet sheet,
        String tableName,
        FormulaEvaluator formulaEvaluator,
        SqlDialect dialect
    ) {
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            throw new IllegalStateException("第 2 列不存在（表：" + tableName + "）");
        }

        // 欄名與資料列
        ColumnLayout layout = parseColumnLayout(headerRow, tableName);
        List<String> valueTuples = collectValueTuples(
            sheet,
            layout,
            formulaEvaluator,
            dialect
        );

        if (valueTuples.isEmpty()) {
            return "";
        }

        // 單條 INSERT
        return "INSERT INTO " + tableName + "\n(" + String.join(", ", layout.columnNames()) + ") VALUES\n"
            + String.join(",\n", valueTuples)
            + ";";
    }

    /** 解析欄名列 */
    private static ColumnLayout parseColumnLayout(Row headerRow, String tableName) {
        List<Integer> columnIndexes = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        // 橫向掃標題列，略過空儲存格
        short lastCellNum = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell == null) {
                continue;
            }
            String columnName = cell.toString().trim();
            if (!columnName.isEmpty()) {
                columnIndexes.add(cellIndex);
                columns.add(columnName);
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalStateException("沒有欄名（表：" + tableName + "）");
        }
        return new ColumnLayout(columnIndexes, columns);
    }

    /** 組出 VALUES 括號字串（略過空白列） */
    private static List<String> collectValueTuples(
        Sheet sheet,
        ColumnLayout layout,
        FormulaEvaluator formulaEvaluator,
        SqlDialect dialect
    ) {
        List<Integer> columnIndexes = layout.columnIndexes();
        List<String> valuesList = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();
        // 自資料列起，一列一組括號
        for (int rowIndex = FIRST_DATA_ROW_INDEX; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isEmptyRow(row, columnIndexes)) {
                continue;
            }

            List<String> values = new ArrayList<>();
            for (Integer cellIndex : columnIndexes) {
                Cell cell = row == null ? null : row.getCell(cellIndex);
                values.add(escapeSql(cell, formulaEvaluator, dialect));
            }
            valuesList.add("(" + String.join(", ", values) + ")");
        }
        return valuesList;
    }

    /** 是否空白列（依指定欄） */
    private static boolean isEmptyRow(Row row, List<Integer> columnIndexes) {
        if (row == null) {
            return true;
        }
        for (Integer cellIndex : columnIndexes) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 儲存格 → SQL 片段 */
    private static String escapeSql(Cell cell, FormulaEvaluator formulaEvaluator, SqlDialect dialect) {
        if (cell == null) {
            return "NULL";
        }

        CellType cellType = cell.getCellType();
        // 公式先求值
        if (cellType == CellType.FORMULA) {
            CellValue evaluated = formulaEvaluator.evaluate(cell);
            if (evaluated == null) {
                return "NULL";
            }
            return escapeEvaluatedCell(cell, evaluated, dialect);
        }

        // 非公式：依儲存格型別
        return switch (cellType) {
            case BLANK -> "NULL";
            case STRING -> quoteString(cell.getStringCellValue());
            case BOOLEAN -> formatBoolean(cell.getBooleanCellValue(), dialect);
            case NUMERIC -> formatNumericOrDate(cell.getNumericCellValue(), DateUtil.isCellDateFormatted(cell));
            case ERROR, _NONE -> "NULL";
            default -> quoteString(cell.toString());
        };
    }

    /** CellValue → SQL 片段 */
    private static String escapeEvaluatedCell(
        Cell originalCell,
        CellValue evaluated,
        SqlDialect dialect
    ) {
        // 求值後型別對應 SQL
        return switch (evaluated.getCellType()) {
            case STRING -> quoteString(evaluated.getStringValue());
            case BOOLEAN -> formatBoolean(evaluated.getBooleanValue(), dialect);
            case NUMERIC -> formatNumericOrDate(
                evaluated.getNumberValue(),
                DateUtil.isCellDateFormatted(originalCell)
            );
            case BLANK, ERROR, _NONE -> "NULL";
            default -> quoteString(evaluated.formatAsString());
        };
    }

    /** 數值或日期 SQL 字面 */
    private static String formatNumericOrDate(double numericValue, boolean isDateCell) {
        // 日期儲存格→字串；否則數字字面
        if (isDateCell) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                DateUtil.getJavaDate(numericValue).toInstant(),
                ZoneId.systemDefault()
            );
            return quoteString(localDateTime.format(DATE_TIME_FORMATTER));
        }

        BigDecimal decimal = BigDecimal.valueOf(numericValue).stripTrailingZeros();
        return decimal.scale() <= 0 ? decimal.toBigInteger().toString() : decimal.toPlainString();
    }

    /** 布林 SQL 字面 */
    private static String formatBoolean(boolean value, SqlDialect dialect) {
        return switch (dialect) {
            case MYSQL -> value ? "1" : "0";
            case H2 -> value ? "TRUE" : "FALSE";
        };
    }

    /** 依 TABLE_ORDER 與表名字母排序 */
    private static List<String> orderSqlStatements(Map<String, String> tableSql) {
        List<String> orderedStatements = new ArrayList<>();
        Map<String, String> remaining = new HashMap<>(tableSql);

        // 固定順序內的表
        for (String tableName : TABLE_ORDER) {
            if (remaining.containsKey(tableName)) {
                orderedStatements.add(remaining.remove(tableName));
            }
        }

        // 其餘依表名排序
        remaining.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .forEach(orderedStatements::add);

        return orderedStatements;
    }

    /** 寫入 UTF-8 SQL 檔 */
    private static Path writeSqlFile(List<String> statements, Path outputDir, String outputFile) throws IOException {
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve(outputFile);

        // 多段 INSERT 之間空一行
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.join("\n\n", statements));
            writer.write("\n");
        }

        return outputPath;
    }

    /** 去掉副檔名 */
    private static String stripExtension(String fileName) {
        // 最後一個副檔名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    /** SQL 字串常值跳脫 */
    private static String quoteString(String value) {
        if (value == null) {
            return "NULL";
        }
        // 單引號加倍
        return "'" + value.replace("'", "''") + "'";
    }
}
