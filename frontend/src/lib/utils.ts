import type { ClassValue } from "clsx"
import { clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** public 資料夾內靜態資源（會依 Vite base 加上子路徑前綴，例如 GitHub Pages） */
export function publicAsset(path: string): string {
  const normalized = path.replace(/^\//, "")
  return `${import.meta.env.BASE_URL}${normalized}`
}

export const homeBgBackgroundStyle = {
  backgroundImage: `linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('${publicAsset("images/homeBg.jpg")}')`,
}
