# scripts 使用說明

這個資料夾的腳本負責三件事：

- 從 `excel-data/*.xlsx` 產生 SQL（MySQL/H2 各一份）
- 匯入 `sql/generated/*.sql` 到目前 profile 指定的資料庫
- 重建資料庫後直接啟動 Spring Boot

## 建議執行順序

### 1) Excel 轉 SQL

```powershell
mvn exec:java@excel-to-sql
```

- Excel 規格：第 2 列為欄名、第 3 列起為資料
- 來源資料夾：`excel-data`
- 輸出檔案：
  - `sql/generated/data-mysql.sql`
  - `sql/generated/data-h2.sql`

### 2) 重建 DB 並啟動後端

```powershell
mvn exec:java
```

- `ResetDb` 會依 `SPRING_PROFILES_ACTIVE` 判斷重建 MySQL 或 H2
- 完成後直接呼叫 `MogumoguApplication.main(...)` 啟動（行為接近 IDE 執行主程式）

### 3) 匯入 SQL

```powershell
mvn exec:java@import-generated-sql
```

- 匯入前會先清空資料表（保留 `flyway_schema_history`）
- 會依 `SPRING_PROFILES_ACTIVE` 選擇目標：
  - 包含 `mysql` -> 匯入 MySQL
  - 包含 `h2` -> 匯入 H2
  - 其他值 -> 直接報錯

## 常用環境變數

- `SPRING_PROFILES_ACTIVE`
- `DB_HOST`
- `PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

腳本讀值順序為：系統環境變數 -> 專案根目錄 `.env` -> 內建預設值。

## 常見問題

- `excel-data` 有檔案但仍報找不到可用 xlsx：
  - 先確認不是 Excel 暫存鎖檔（`~$*.xlsx`）
  - 關閉 Excel 後再執行
- `import-generated-sql` 失敗：
  - 先確認目前 `SPRING_PROFILES_ACTIVE` 是否為 `mysql` 或 `h2`
  - 再確認對應 SQL 檔存在於 `sql/generated`
