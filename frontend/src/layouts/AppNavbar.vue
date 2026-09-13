<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import client from '@/api/client'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { authSession, hasGroup, logoutAuth } from '@/lib/authSession'
import { getRoleLabel } from '@/lib/userRole'

const router = useRouter()

/** 目前所屬群組名稱 */
const groupName = ref('')

/** 主要功能頁捷徑 */
const navItems = [
  { label: '抽餐廳', name: 'random-restaurant' },
  { label: '餐廳一覽', name: 'list-restaurant' },
  { label: '歷史紀錄', name: 'restaurant-history' },
  { label: '分類管理', name: 'category-management' },
  { label: '成員管理', name: 'member-management' },
]

/** 群組名稱與角色 */
const userInfo = computed(() =>
  [groupName.value, getRoleLabel(authSession.value?.role)].filter(Boolean).join(' · '),
)

/** 查詢群組名稱 */
async function loadGroupName() {
  if (!hasGroup()) return

  const { data } = await client.GET('/groups/my')
  groupName.value = data?.groupName?.trim() ?? ''
}

/** 登出並回到登入頁 */
function handleLogout() {
  logoutAuth()
  router.push({ name: 'home' })
}

onMounted(loadGroupName)
</script>

<template>
  <header class="sticky top-0 z-50 border-b bg-background/95">
    <div class="flex w-full items-center justify-between px-4 py-2">
      <div class="flex items-center gap-7">
        <!-- 回抽餐廳頁 -->
        <RouterLink :to="{ name: 'random-restaurant' }">
          <img src="/images/logo.png" alt="Mogumogu Pick" class="h-16" />
        </RouterLink>

        <nav class="flex gap-2">
          <PrimaryButton
            v-for="item in navItems"
            :key="item.name"
            variant="standard"
            @click="router.push({ name: item.name })"
          >
            {{ item.label }}
          </PrimaryButton>
        </nav>
      </div>

      <div class="flex items-center gap-5">
        <!-- 使用者與群組資訊 -->
        <div class="hidden text-right text-base font-bold text-[#5c4033] sm:block">
          <div>Hello！{{ authSession?.username ?? '使用者' }}</div>
          <div>{{ userInfo }}</div>
        </div>

        <PrimaryButton class="min-w-20 px-6" variant="outline" @click="handleLogout">
          登出
        </PrimaryButton>
      </div>
    </div>
  </header>
</template>
