# frontend

Vue 3 + Vite 前端專案。

## 開發

```sh
npm install
npm run dev
```

## OpenAPI

後端啟動後（port `8080`），執行以下指令產生型別：

```sh
npm run generate:api
```

產出：`src/api/schema.d.ts`（自動產生，勿手動修改）

呼叫 API 請使用 `src/api/client.ts`：

```typescript
import client from '@/api/client'

const { data, error } = await client.GET('/restaurants')
```

API 位址可於 `.env` 設定 `VITE_API_BASE_URL`（預設 `http://localhost:8080`）。
