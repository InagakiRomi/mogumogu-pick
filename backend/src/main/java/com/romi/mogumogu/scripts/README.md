# scripts 說明

| 指令 | 用途 |
|------|------|
| `mvn exec:java@excel-to-sql` | 讀 `excel-data/*.xlsx`，輸出 `sql/generated/data-mysql.sql` 與 `data-h2.sql` |
| `mvn exec:java` | 依 profile 重建資料庫後啟動 Spring Boot（`ResetDb` + 主程式） |
| `mvn exec:java@import-generated-sql` | 清空資料表（保留 `flyway_schema_history`）後匯入 `sql/generated/*.sql` |

**Excel 轉 SQL：** 第 2 列為欄名、第 3 列起為資料。

**匯入目標：** `SPRING_PROFILES_ACTIVE` 須含 `mysql` 或 `h2`，對應匯入 MySQL 或 H2；其他值會報錯。

**環境變數：** `SPRING_PROFILES_ACTIVE`、`DB_HOST`、`DB_PORT`（MySQL，預設 3306）、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。讀取順序：系統環境變數 → 專案根 `.env` → 內建預設。

**若出錯：** 先 `mvn compile` 再執行（或 `mvn compile exec:java@...`）。找不到 xlsx 時排除 `~$*.xlsx` 並關閉 Excel。匯入失敗時檢查 profile 與 `sql/generated` 是否有對應檔案。
