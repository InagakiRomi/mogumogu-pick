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
import java.util.logging.Level;
import java.util.logging.Logger;
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

import com.romi.mogumogu.constant.DateTimePatternConstants;
import com.romi.mogumogu.logging.JulLoggerFactory;

public class ExcelToSql {

    private static final Logger log = new JulLoggerFactory().printToolLog();
    /** Excel 來源目錄名稱 */
    private static final String EXCEL_DATA_DIR = "excel-data";
    /** SQL 輸出目錄 */
    private static final String OUTPUT_DIR = "sql/generated";
    /** MySQL SQL 輸出檔 */
    private static final String MYSQL_OUTPUT_FILE = "data-mysql.sql";
    /** H2 SQL 輸出檔 */
    private static final String H2_OUTPUT_FILE = "data-h2.sql";

    /** 欄名所在列（POI index） */
    private static final int HEADER_ROW_INDEX = 1;
    /** 資料起始列（POI index） */
    private static final int FIRST_DATA_ROW_INDEX = 2;

    /** 與 Flyway 既有資料表一致；外鍵被參考者在前 */
    private static final List<String> TABLE_ORDER = List.of(
            "restaurant_category",
            "restaurant");

    /** SQL 方言 */
    private enum SqlDialect {
        MYSQL,
        H2
    }

    private record ColumnLayout(List<Integer> columnIndexes, List<String> columnNames) {
    }

    /** 讀取 Excel 來源目錄內的 .xlsx，輸出 MySQL/H2 的 INSERT SQL 檔 */
    public static void main(String[] args) {
        try {
            Path moduleRoot = resolveModuleRoot();
            Path excelDataDir = resolveExcelDataDir(moduleRoot);
            Path outputDir = moduleRoot.resolve(OUTPUT_DIR);

            Map<String, String> mysqlTableSql = loadTableSqlFromExcels(excelDataDir, SqlDialect.MYSQL);
            Map<String, String> h2TableSql = loadTableSqlFromExcels(excelDataDir, SqlDialect.H2);
            if (mysqlTableSql.isEmpty() || h2TableSql.isEmpty()) {
                throw new IllegalStateException(
                        "未產生任何 INSERT：請確認 .xlsx 第 2 列為欄名、第 3 列起有資料");
            }

            List<String> mysqlStatements = orderSqlStatements(mysqlTableSql);
            List<String> h2Statements = orderSqlStatements(h2TableSql);
            Path mysqlOutputPath = writeSqlFile(mysqlStatements, outputDir, MYSQL_OUTPUT_FILE);
            Path h2OutputPath = writeSqlFile(h2Statements, outputDir, H2_OUTPUT_FILE);
            log.info("已寫入 MySQL SQL： " + mysqlOutputPath.toAbsolutePath());
            log.info("已寫入 H2 SQL： " + h2OutputPath.toAbsolutePath());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "失敗：" + ex.getMessage(), ex);
        }
    }

    /** 解析模組根目錄，支援從子目錄執行 */
    private static Path resolveModuleRoot() {
        Path start = Path.of("").toAbsolutePath().normalize();
        if (hasModuleLayout(start)) {
            return start;
        }
        Path backend = start.resolve("backend");
        if (hasModuleLayout(backend)) {
            return backend.normalize();
        }
        Path dir = start;
        for (int i = 0; i < 6 && dir != null; i++) {
            if (hasModuleLayout(dir)) {
                return dir;
            }
            dir = dir.getParent();
        }
        return start;
    }

    /** 檢查是否具備 Maven 模組必要結構 */
    private static boolean hasModuleLayout(Path dir) {
        return Files.isRegularFile(dir.resolve("pom.xml"))
                && Files.isDirectory(dir.resolve("src/main/resources"));
    }

    /** 尋找內含 .xlsx 的來源目錄（模組內或上一層，目錄名見 {@link #EXCEL_DATA_DIR}） */
    private static Path resolveExcelDataDir(Path moduleRoot) throws IOException {
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
            if (!xlsxFiles.isEmpty()) {
                return candidate;
            }
            existingButEmpty = candidate;
        }

        if (existingButEmpty != null) {
            throw new IllegalStateException("資料夾內沒有任何 .xlsx：" + existingButEmpty.toAbsolutePath());
        }
        throw new IllegalStateException(
                "找不到含 .xlsx 的 Excel 來源目錄。已嘗試："
                        + candidates.stream().map(path -> path.toAbsolutePath().toString())
                                .collect(Collectors.joining(", ")));
    }

    /** 掃描所有 xlsx 並轉成各資料表 INSERT */
    private static Map<String, String> loadTableSqlFromExcels(Path excelDataDir, SqlDialect dialect)
            throws IOException {
        if (!Files.isDirectory(excelDataDir)) {
            throw new IllegalStateException("找不到資料夾：" + excelDataDir.toAbsolutePath());
        }
        List<Path> excelFiles = listXlsxFiles(excelDataDir);
        if (excelFiles.isEmpty()) {
            throw new IllegalStateException("沒有 .xlsx：" + excelDataDir.toAbsolutePath());
        }
        requireExcelWorkbooksClosed(excelDataDir, excelFiles);

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

    /** 列出 .xlsx 並排除 Excel 暫存鎖定檔 */
    private static List<Path> listXlsxFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> {
                        String n = path.getFileName().toString();
                        return n.toLowerCase().endsWith(".xlsx") && !n.startsWith("~$");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    /** 若偵測到 ~$ 鎖定檔，提示先關閉 Excel */
    private static void requireExcelWorkbooksClosed(Path excelDataDir, List<Path> excelFiles) {
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
        String detail = openLike.stream().map(n -> "  - " + n).collect(Collectors.joining("\n"));
        throw new IllegalStateException(
                "下列 .xlsx 可能仍於 Excel 中開啟（同資料夾存在對應的 ~$ 鎖定檔），已中止產生 SQL。"
                        + "請先儲存並完全關閉活頁簿；若已關閉，請刪除「"
                        + excelDataDir.toAbsolutePath()
                        + "」內以 ~$ 開頭的暫存檔後再執行。\n"
                        + detail);
    }

    /** 單一 Excel 檔轉成單條 INSERT */
    private static String excelToInsertSql(Path excelPath, String tableName, SqlDialect dialect)
            throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelPath);
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                return "";
            }
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return sheetToInsertSql(sheet, tableName, formulaEvaluator, dialect);
        }
    }

    /** 將工作表內容組成 INSERT INTO ... VALUES ... */
    private static String sheetToInsertSql(
            Sheet sheet,
            String tableName,
            FormulaEvaluator formulaEvaluator,
            SqlDialect dialect) {
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            throw new IllegalStateException("第 2 列不存在（表：" + tableName + "）");
        }

        ColumnLayout layout = parseColumnLayout(headerRow, tableName);
        List<String> valueTuples = collectValueTuples(
                sheet,
                tableName,
                layout,
                formulaEvaluator,
                dialect);

        if (valueTuples.isEmpty()) {
            return "";
        }

        return "INSERT INTO " + tableName + "\n(" + String.join(", ", layout.columnNames()) + ") VALUES\n"
                + String.join(",\n", valueTuples)
                + ";";
    }

    /** 解析欄位名稱列，略過空欄 */
    private static ColumnLayout parseColumnLayout(Row headerRow, String tableName) {
        List<Integer> columnIndexes = new ArrayList<>();
        List<String> columns = new ArrayList<>();
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

    /** 收集每列資料成 SQL tuple，空白列會跳過 */
    private static List<String> collectValueTuples(
            Sheet sheet,
            String tableName,
            ColumnLayout layout,
            FormulaEvaluator formulaEvaluator,
            SqlDialect dialect) {
        List<Integer> columnIndexes = layout.columnIndexes();
        List<String> columnNames = layout.columnNames();
        List<String> valuesList = new ArrayList<>();
        int categoryIdColumnIndex = -1;
        if ("restaurant_category".equalsIgnoreCase(tableName)) {
            for (int i = 0; i < columnNames.size(); i++) {
                if ("category_id".equalsIgnoreCase(columnNames.get(i))) {
                    categoryIdColumnIndex = i;
                    break;
                }
            }
        }
        int lastRowNum = sheet.getLastRowNum();
        for (int rowIndex = FIRST_DATA_ROW_INDEX; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isEmptyRow(row, columnIndexes)) {
                continue;
            }

            List<String> values = new ArrayList<>();
            int colPos = 0;
            for (Integer cellIndex : columnIndexes) {
                if (categoryIdColumnIndex == colPos) {
                    values.add(String.valueOf(valuesList.size() + 1));
                } else {
                    Cell cell = row == null ? null : row.getCell(cellIndex);
                    values.add(escapeSql(cell, formulaEvaluator, dialect));
                }
                colPos++;
            }
            valuesList.add("(" + String.join(", ", values) + ")");
        }
        return valuesList;
    }

    /** 判斷一列是否全空 */
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

    /** 將儲存格值轉成 SQL 字面值 */
    private static String escapeSql(Cell cell, FormulaEvaluator formulaEvaluator, SqlDialect dialect) {
        if (cell == null) {
            return "NULL";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            CellValue evaluated = formulaEvaluator.evaluate(cell);
            if (evaluated == null) {
                return "NULL";
            }
            return escapeEvaluatedCell(cell, evaluated, dialect);
        }

        return switch (cellType) {
            case BLANK -> "NULL";
            case STRING -> quoteString(cell.getStringCellValue());
            case BOOLEAN -> formatBoolean(cell.getBooleanCellValue(), dialect);
            case NUMERIC -> formatNumericOrDate(cell.getNumericCellValue(), DateUtil.isCellDateFormatted(cell));
            case ERROR, _NONE -> "NULL";
            default -> quoteString(cell.toString());
        };
    }

    /** 處理公式儲存格求值後的 SQL 格式 */
    private static String escapeEvaluatedCell(
            Cell originalCell,
            CellValue evaluated,
            SqlDialect dialect) {
        return switch (evaluated.getCellType()) {
            case STRING -> quoteString(evaluated.getStringValue());
            case BOOLEAN -> formatBoolean(evaluated.getBooleanValue(), dialect);
            case NUMERIC -> formatNumericOrDate(
                    evaluated.getNumberValue(),
                    DateUtil.isCellDateFormatted(originalCell));
            case BLANK, ERROR, _NONE -> "NULL";
            default -> quoteString(evaluated.formatAsString());
        };
    }

    /** 日期轉字串，其餘數值保持數字格式 */
    private static String formatNumericOrDate(double numericValue, boolean isDateCell) {
        if (isDateCell) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                    DateUtil.getJavaDate(numericValue).toInstant(),
                    ZoneId.systemDefault());
            return quoteString(localDateTime.format(DateTimeFormatter.ofPattern(DateTimePatternConstants.STANDARD_DATE_TIME)));
        }

        BigDecimal decimal = BigDecimal.valueOf(numericValue).stripTrailingZeros();
        return decimal.scale() <= 0 ? decimal.toBigInteger().toString() : decimal.toPlainString();
    }

    /** 依方言輸出布林（MySQL=1/0，H2=TRUE/FALSE） */
    private static String formatBoolean(boolean value, SqlDialect dialect) {
        return switch (dialect) {
            case MYSQL -> value ? "1" : "0";
            case H2 -> value ? "TRUE" : "FALSE";
        };
    }

    /** 先依 TABLE_ORDER，再以表名字母排序 */
    private static List<String> orderSqlStatements(Map<String, String> tableSql) {
        List<String> orderedStatements = new ArrayList<>();
        Map<String, String> remaining = new HashMap<>(tableSql);

        for (String tableName : TABLE_ORDER) {
            if (remaining.containsKey(tableName)) {
                orderedStatements.add(remaining.remove(tableName));
            }
        }

        remaining.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(orderedStatements::add);

        return orderedStatements;
    }

    /** 以 UTF-8 寫檔，語句間以空行分隔 */
    private static Path writeSqlFile(List<String> statements, Path outputDir, String outputFile) throws IOException {
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.join("\n\n", statements));
            writer.write("\n");
        }

        return outputPath;
    }

    /** 去除檔名副檔名 */
    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    /** SQL 字串常值跳脫（單引號加倍） */
    private static String quoteString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }
}
