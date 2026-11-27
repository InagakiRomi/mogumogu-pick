
package com.romi.mogumogu.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelToSqlConverter {
    public static void main(String[] args) throws Exception {
        ExcelToSql();
    }

    public static void ExcelToSql() throws IOException {
        /** Excel檔案放置路徑 */
        Path inputDir = Path.of("../excel-data");
        /** Sql輸出路徑 */
        Path outputDir = Path.of("src/main/resources/sql");

        // 檢查Excel檔案放置路徑是否存在
        if (!Files.isDirectory(inputDir)) {
            throw new NoSuchFileException("Excel檔案放置路徑不存在");
        }

        // 如果Sql輸出資料夾不存在就建立
        Files.createDirectories(outputDir);

        try (Stream<Path> files = Files.list(inputDir)) {
            // 調整檔案順序
            List<Path> sortedFiles = files
                    .sorted(Comparator.comparingInt(file -> {
                        String name = file.getFileName().toString();

                        return switch (name) {
                            case "group.xlsx" -> 0;
                            case "restaurant_category.xlsx" -> 1;
                            case "restaurant.xlsx" -> 2;
                            default -> 3;
                        };
                    }))
                    .toList();

            // 計算第幾個檔案
            int index = 1;

            for (Path file : sortedFiles) {
                String fileName = file.getFileName().toString();

                // 只處理Excel檔案
                if (!fileName.endsWith(".xlsx")) {
                    continue;
                }

                // Excel轉成Sql語言
                StringBuilder sql = readExcel(file);

                // 處理檔案名稱
                String fileNameAddNumber = index + "_" + fileName.replace(".xlsx", "");
                if (index <= 9) {
                    fileNameAddNumber = "0" + fileNameAddNumber;
                }
                Path sqlFile = outputDir.resolve(fileNameAddNumber + ".sql");

                // 建立SQL檔案
                Files.writeString(sqlFile, sql);

                // 增加處理檔案順位
                index++;
            }
        }
        System.out.println("已完成Excel轉Sql");
    }

    /** 讀取Excel內容並轉成Sql語言 */
    private static StringBuilder readExcel(Path file) throws IOException {
        StringBuilder sql = new StringBuilder();

        try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            String fileName = file.getFileName().toString().replace(".xlsx", "");

            // 建立Table文字
            sql.append("INSERT INTO ");
            if (fileName.equals("user") || fileName.equals("group")) {
                sql.append("`" + fileName + "`");
            } else {
                sql.append(fileName);
            }
            sql.append("(");

            // 讀取表單文字
            for (Row row : sheet) {
                // 跳過中文標題和資料型別還有空白列
                if (row.getRowNum() == 0 || row.getRowNum() == 2 || row.getPhysicalNumberOfCells() == 0) {
                    continue;
                }

                // 存取抓取欄位文字
                for (Cell cell : row) {
                    // 抓取欄位
                    String type = formatter.formatCellValue(sheet.getRow(2).getCell(cell.getColumnIndex()));

                    // 輸出文字
                    String value = formatter.formatCellValue(cell);

                    // 判斷是不是最後一欄
                    boolean isLast = cell.getColumnIndex() == row.getLastCellNum() - 1;

                    // 從處理資料開始只要資料型別是文字的就加上''
                    if (row.getRowNum() >= 3 && !value.equalsIgnoreCase("NULL")) {
                        switch (type) {
                            case "VARCHAR", "TIMESTAMP" ->
                                value = "'" + value + "'";
                            default -> {
                            }
                        }
                    }

                    sql.append(value);

                    if (!isLast) {
                        sql.append(",");
                    }
                }
                sql.append(")");

                if (row.getRowNum() == 1) {
                    sql.append(" VALUES")
                            .append("\n");
                } else {
                    sql.append(",")
                            .append("\n");
                }
                sql.append("(");
            }
            // 刪除最後多餘的逗號與換行
            sql.delete(sql.length() - 3, sql.length());

            sql.append(";");
        }
        return sql;
    }
}
