# scripts 重點用法

## 1) 重建資料庫並啟動後端（預設）

```powershell
mvn exec:java
```

## 2) 產生 SQL（Excel -> MySQL/H2）

```powershell
mvn exec:java@excel-to-sql
```

- 來源：`excel-data/*.xlsx`（第 2 列欄名、第 3 列開始資料）
- 輸出：`sql/generated/data-mysql.sql`、`sql/generated/data-h2.sql`

## 3) 匯入產生好的 SQL

```powershell
mvn exec:java@import-generated-sql
```

- 會先清空資料表（保留 `flyway_schema_history`）再匯入
- MySQL、H2 分開執行；單邊失敗不會中斷另一邊

- `SPRING_PROFILES_ACTIVE` 包含 `mysql` 時走 MySQL；否則走 H2
- 需要先有可執行 jar（`ResetDb` 會使用 `java -jar target/*.jar` 啟動）

## 常用環境變數

- `SPRING_PROFILES_ACTIVE`
- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
