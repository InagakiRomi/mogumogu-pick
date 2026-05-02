# ResetDb.java

快速重置資料庫並啟動對應 profile 的 Spring Boot。

## 用法

在 `backend` 根目錄執行（同一指令，由環境變數決定 MySQL 或 H2）：

```powershell
java src/main/java/com/romi/mogumogu/scripts/ResetDb.java
```

## 會用到的環境變數

可放在 `.env`：

- `SPRING_PROFILES_ACTIVE`：若逗號分隔的 profile 中含 `mysql` 則重置 MySQL 並以 `mysql` 啟動；否則重置 H2 並以 `h2` 啟動（未設定時與 Spring 預設相同，視為 H2）。
- `DB_NAME`（僅 MySQL）
- `DB_USERNAME`、`DB_PASSWORD`（僅 MySQL）

## 注意

- 使用 MySQL 時需安裝並可執行 `mysql` 指令。
- 若重置 H2 失敗，先停止正在跑的後端程式再重試。
- 程式碼位置：`src/main/java/com/romi/mogumogu/scripts/ResetDb.java`。

---

# ExcelToSql.java

讀取 `excel-data` 底下的 `.xlsx`（每檔第一個 sheet，第 2 列欄名、第 3 列起資料），產出 `sql/generated/data-mysql.sql` 與 `data-h2.sql`。

## 用法

在 `backend` 根目錄執行：

```powershell
mvnw.cmd -DskipTests exec:java
```

## 注意

- Excel 路徑會找模組旁的 `excel-data`，或上一層 repo 的 `excel-data`；需有 `.xlsx` 才會成功。
- 依賴 POI，已在 `pom.xml`；`exec` 工作目錄為模組根，建議在 `backend` 下執行上述指令。
- 程式碼位置：`src/main/java/com/romi/mogumogu/scripts/ExcelToSql.java`。

---

# ImportGeneratedSql.java

先清空資料表，再分別匯入 `sql/generated/data-mysql.sql` 與 `sql/generated/data-h2.sql`。

## 用法

在 `backend` 根目錄執行：

```powershell
mvnw.cmd -DskipTests exec:java -Dexec.mainClass=com.romi.mogumogu.scripts.ImportGeneratedSql
```

## 行為說明

- MySQL、H2 兩個流程互相獨立執行。
- 若 MySQL 連線失敗，會先顯示警告，然後繼續執行 H2 匯入。
- 兩邊都跑完後，只要其中一個失敗就會拋錯（方便 CI/批次腳本判定失敗），但不會中斷另一邊流程。
- 清空資料時會略過 `flyway_schema_history`。

## 可用環境變數

- MySQL：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- H2 固定使用：`jdbc:h2:file:./data/mogumogu;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`（帳號 `sa`、空密碼）

## 注意

- `data-mysql.sql` 與 `data-h2.sql` 的資料表名稱需對應到各自資料庫現有 schema。
- 需在 `backend` 根目錄執行，腳本會直接讀 `sql/generated` 下的兩個檔案。
- 程式碼位置：`src/main/java/com/romi/mogumogu/scripts/ImportGeneratedSql.java`。
