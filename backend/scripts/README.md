# reset-db.ts

快速重置資料庫並啟動對應 profile 的 Spring Boot。

## 用法

在 `backend` 根目錄執行（同一指令，由環境變數決定 MySQL 或 H2）：

```powershell
npx tsx scripts/reset-db.ts
```

## 會用到的環境變數

可放在 `.env`：

- `SPRING_PROFILES_ACTIVE`：若逗號分隔的 profile 中含 `mysql` 則重置 MySQL 並以 `mysql` 啟動；否則重置 H2 並以 `h2` 啟動（未設定時與 Spring 預設相同，視為 H2）。
- `DB_NAME`（僅 MySQL）
- `DB_USERNAME`、`DB_PASSWORD`（僅 MySQL）

## 注意

- 使用 MySQL 時需安裝並可執行 `mysql` 指令。
- 若重置 H2 失敗，先停止正在跑的後端程式再重試。

