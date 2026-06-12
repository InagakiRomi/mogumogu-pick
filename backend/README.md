# backend

Spring Boot 後端 API。

## 啟動

```sh
# 首次：複製 .env.default 為 .env
mvn exec:java
```

API 於 http://localhost:8080 執行。

## 備註

- 需 Java 17、Maven
- 預設使用 H2 本機資料庫（`.env` 的 `SPRING_PROFILES_ACTIVE=h2`）
- 改用 MySQL：設 `SPRING_PROFILES_ACTIVE=mysql` 並填入 DB 帳密
- 不重建資料庫、僅啟動：`mvn spring-boot:run`
