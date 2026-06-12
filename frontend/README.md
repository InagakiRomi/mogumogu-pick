# frontend

Vue 3 + Vite 前端專案。

## 啟動

```sh
npm install
npm run dev
```

瀏覽器開啟 http://localhost:5173

開發時需先啟動後端（`localhost:8080`），API 會經 proxy 轉發。

## 備註

- 建置：`npm run build`
- 後端 API 有變更時：`npm run generate:api`（產出 `src/api/schema.d.ts`）
- 正式環境 API 網址請設定於 `.env.production`
