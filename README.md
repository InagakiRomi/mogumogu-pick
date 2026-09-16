# mogumogu-pick

**mogumogu-pick** 協助團隊決定要吃什麼。使用者以群組為單位維護餐廳與餐點，透過隨機抽籤選出餐廳、查看過往選取紀錄，並可用地圖搜尋附近店家後加入群組清單。

### 專案連結

- 前端 Demo：https://inagakiromi.github.io/mogumogu-pick/
- API 文件：https://mogumogu-pick.onrender.com/swagger-ui/index.html#/

### 測試帳號

| 角色 | 帳號 | 密碼 |
| --- | --- | --- |
| 群組管理員 | groupadmin@test.com | 123 |
| 一般成員 | user@test.com | 123 |

---

## 技術棧

### 前端

| 類別 | 技術 |
| --- | --- |
| 框架 | Vue 3、TypeScript、Vite |
| 路由 / 狀態 | Vue Router、Pinia |
| UI | Tailwind CSS、shadcn-vue、Reka UI、Lucide |
| 地圖 | Leaflet、OpenStreetMap |
| API | openapi-fetch（型別由後端 OpenAPI 產生） |
| 部署 | GitHub Pages |

### 後端

| 類別 | 技術 |
| --- | --- |
| 語言 / 框架 | Java 17、Spring Boot 3.5 |
| 安全 | Spring Security、JWT |
| 資料 | Spring Data JPA、H2 / MySQL、Flyway |
| 外部資料 | Overpass API（OpenStreetMap 附近餐廳） |
| API 文件 | springdoc-openapi |
| 部署 | Docker、Render |

角色分為 `GROUP_ADMIN`（群組管理員）與 `USER`（一般成員）。餐廳、分類、餐點與選取紀錄皆以群組隔離。

---

## 功能

### 認證

- 註冊、登入；登入後以 JWT 存取功能頁
- 註冊可選擇管理員或一般成員；管理員註冊時自動建立群組與預設分類
- 尚未加入群組的成員會進入提示頁，需由管理員加入後才能使用功能
- 登入頁會檢查後端連線狀態（部署環境伺服器可能需等待喚醒）

### 隨機抽籤

- 從群組餐廳池抽出一間，可依分類篩選
- 不放回抽籤：本輪已抽出的餐廳不會重複，抽完後自動重置，也可手動重置
- 確認選擇後更新選取次數、最後選取時間，並寫入歷史紀錄

### 我的餐廳

- 分頁列表：分類篩選、名稱搜尋、依 ID／建立時間／選取次數／最後選擇時間排序
- 新增餐廳（名稱、分類、地址、備註、圖片等）
- 餐廳詳情：檢視與編輯餐廳資料、刪除餐廳
- 餐點管理：為指定餐廳新增、修改、刪除餐點

### 美食地圖

- 以目前位置（失敗時使用預設座標）顯示台灣範圍地圖
- 搜尋地圖中心附近的店家（資料來自 OpenStreetMap / Overpass，搜尋範圍限台灣）
- 在地圖上檢視店家資訊，並可帶入資料新增到群組餐廳清單

### 歷史紀錄

- 分頁查詢群組選取紀錄，可依時間排序
- 可從紀錄進入對應餐廳詳情
- 群組管理員可清除全部歷史紀錄

### 分類管理

- 群組內餐廳分類的新增、編輯、刪除（名稱不可重複）
- 可依排序 ID、使用次數排序

### 成員管理

- 查看群組成員與角色
- 管理員：修改群組名稱、以 Email 新增成員、移出成員、移轉管理權
- 成員可自行退出群組

---

## 專案畫面

<img width="800" align="top" src="https://github.com/user-attachments/assets/98f79141-44fa-4a1d-aa93-ac98cb35724b" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/b7c27469-da06-4f01-900a-ba1a80e5c5c1" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/14fb2616-d5e8-4f77-a561-eec31608bec5" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/8572d2ba-0774-4ef7-9653-74ab8ffec048" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/48290ee2-f0c0-4324-ae53-efb3dbd063df" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/23f8947f-e730-4f6d-a069-a2a680794d8e" /><br><br>
<img width="800" align="top" src="https://github.com/user-attachments/assets/50fa2e7e-44ff-4027-aff4-e3d072dab6bd" />

