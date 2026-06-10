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

## 環境與 API 位址

| 指令 | 設定檔 | 連線方式 |
|------|--------|----------|
| `npm run dev` | `.env.development` | `VITE_API_BASE_URL=/backend`，經 proxy 轉至本機 `8080` |
| `npm run build` | `.env.production` | 直連 `VITE_API_BASE_URL` 設定的正式伺服器 |

請在 `.env.production` 填入實際 API 網址。
