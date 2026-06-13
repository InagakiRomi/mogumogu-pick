# mogumogu-pick

**mogumogu-pick** 用於協助團隊決定要吃什麼。使用者可在群組內維護餐廳與餐點資料，透過隨機抽籤選出餐廳，並查詢過往選取紀錄。

### 專案連結

前端 Demo：
https://inagakiromi.github.io/mogumogu-pick/

API 文件：
https://mogumogu-pick.onrender.com/swagger-ui/index.html#/

### 測試帳號與密碼

| 類別 | 帳號 | 密碼 |
| --- | --- | --- |
| 群組管理員 | groupadmin@test.com | 123 |
| 一般帳號 | user@test.com | 123 |

---

## 後端技術棧

| 類別 | 技術 |
| --- | --- |
| 語言 / 框架 | Java 17、Spring Boot 3.5 |
| 安全 | Spring Security、JWT |
| 資料存取 | Spring Data JPA |
| 資料庫 | H2、MySQL；Flyway 管理 schema |
| API 文件 | springdoc-openapi |
| 測試 | JUnit 5、MockMvc |
| 部署 | Docker |
| 工具 | Excel 轉 SQL、DB 重置與匯入腳本 |

## 後端目錄結構

```
backend/src/main/java/com/romi/mogumogu/
├── controller/     # REST API 端點
├── service/        # 業務邏輯
├── repository/     # 資料存取層
├── entity/         # JPA 實體
├── dto/            # 請求參數
├── Response/       # 回應格式
├── security/       # JWT 與安全設定
├── config/         # CORS、OpenAPI 等設定
├── exception/      # 全域例外處理
└── scripts/        # 資料匯入與維護工具
```

---

## 功能說明

### 認證與授權

- `POST /auth/register`：使用者註冊
- `POST /auth/login`：使用者登入，成功後回傳 JWT Access Token
- 後續請求需於 Header 帶入 `Authorization: Bearer <token>`
- 角色分為 `GROUP_ADMIN`（群組管理員）與 `USER`（一般成員）

### 群組

- 餐廳、分類、選取紀錄以群組（`group_id`）隔離
- 以 `GROUP_ADMIN` 身分註冊時，系統會自動建立群組並加入預設餐廳分類
- `/groups/my/*` 提供成員管理、群組名稱修改、管理權移轉與退群等功能

### 餐廳管理

| 端點 | 功能 |
| --- | --- |
| `GET /restaurants` | 分頁列表，支援分類篩選、名稱搜尋與排序 |
| `GET /restaurants/{id}` | 取得單筆餐廳資料 |
| `POST /restaurants` | 新增餐廳 |
| `PATCH /restaurants/{id}` | 更新餐廳資料 |
| `DELETE /restaurants/{id}` | 刪除餐廳（連帶刪除關聯餐點與選取紀錄） |

### 隨機抽籤

| 端點 | 功能 |
| --- | --- |
| `GET /restaurants/random?categoryId=` | 從群組餐廳池中隨機抽取一間 |
| `POST /restaurants/random/clear` | 重置抽籤池 |
| `PATCH /restaurants/{id}/choose` | 確認選擇，更新統計並寫入歷史紀錄 |

抽籤流程：

- 每位使用者擁有獨立的記憶體抽籤池
- 首次抽籤或切換分類時，從資料庫載入符合條件的餐廳 ID
- 採不放回方式抽籤：已抽出的餐廳自池中移除，本輪不再重複
- 池中餐廳抽完後自動重置，亦可透過 API 手動清除
- 確認選擇時更新 `selectedCount`、`lastSelectedAt`，並寫入 `restaurant_selection_history`

### 餐廳分類與餐點

- `/restaurant-categories`：餐廳分類 CRUD，群組內名稱不可重複
- `/dishes`：餐點 CRUD，隸屬於指定餐廳

### 選取歷史

- `GET /restaurants/selection-history`：分頁查詢群組選取紀錄

### 錯誤處理

- 以 `@RestControllerAdvice` 統一處理例外
- 回應格式：`{ status, message, path, timestamp }`

---

## 資料模型

```
Group ──< User
  │
  ├──< RestaurantCategory ──< Restaurant ──< Dish
  │
  └──< RestaurantSelectionHistory >── Restaurant
```

| 資料表 | 說明 |
| --- | --- |
| `user` | 使用者與角色 |
| `group` | 群組基本資訊 |
| `restaurant_category` | 餐廳分類 |
| `restaurant` | 餐廳主檔，含選取次數與最後選取時間 |
| `dish` | 餐點 |
| `restaurant_selection_history` | 選取紀錄 |

資料庫 schema 由 Flyway 管理，H2 與 MySQL 各有一套 migration 檔案。

---

## API 端點總覽

| 模組 | 路徑前綴 | 說明 |
| --- | --- | --- |
| Health | `/health` | 健康檢查（公開） |
| Auth | `/auth` | 註冊、登入（公開） |
| Restaurants | `/restaurants` | 餐廳 CRUD、抽籤、選取、歷史 |
| Categories | `/restaurant-categories` | 餐廳分類 CRUD |
| Dishes | `/dishes` | 餐點 CRUD |
| Groups | `/groups/my` | 群組成員與設定 |

完整參數與回應格式請參考 [Swagger UI](https://mogumogu-pick.onrender.com/swagger-ui/index.html#/)。

---

## 前端

前端為 Vue 3 SPA，作為後端 API 的操作介面。

| 項目 | 技術 |
| --- | --- |
| 框架 | Vue 3、Vue Router、TypeScript |
| 建置 | Vite |
| UI | Tailwind CSS、shadcn-vue |
| API | openapi-fetch（型別由後端 OpenAPI 規格產生） |
| 部署 | GitHub Pages（build 輸出至 `/docs`） |

主要頁面：登入 / 註冊、餐廳列表、隨機抽籤、選取歷史、分類管理、成員管理。

---

## 專案結構

```
mogumogu-pick/
├── backend/          # Spring Boot API
├── frontend/         # Vue 3 SPA
├── docs/             # GitHub Pages 靜態站
├── excel-data/       # 種子資料 Excel 來源
└── README.md
```

---

## 專案畫面
<img width="800" align="top" src="https://github.com/user-attachments/assets/2cbeb69b-0313-47f4-a6e1-72a5cf921354" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/68eef0b7-29bf-432f-a5c0-38ad79974fee" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/a77c1999-41b4-4182-83f6-b83830763794" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/7ab9fc62-ee21-4013-804c-42d4bd7f6574" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/3a8f4d9c-08c5-4490-add4-a28c6a28eb51" />





