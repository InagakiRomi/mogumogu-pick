<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { onClickOutside, useMediaQuery } from '@vueuse/core'
import { MenuIcon, XIcon } from '@lucide/vue'
import { useRoute, useRouter } from 'vue-router'

import client from '@/api/client'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { authSession, hasGroup, logoutAuth } from '@/lib/authSession'
import { getRoleLabel } from '@/lib/userRole'
import { publicAsset } from '@/lib/utils'

const router = useRouter()
const route = useRoute()

const groupName = ref('')
const isMenuOpen = ref(false)
const headerRef = ref<HTMLElement | null>(null)

const isCompactLayout = useMediaQuery('(max-width: 1023px)')

const navItems = [
  { label: '抽餐廳', name: 'random-restaurant' },
  { label: '我的餐廳', name: 'list-restaurant' },
  { label: '美食地圖', name: 'map' },
  { label: '歷史紀錄', name: 'restaurant-history' },
  { label: '分類管理', name: 'category-management' },
  { label: '成員管理', name: 'member-management' },
] as const

const username = computed(() => authSession.value?.username ?? '使用者')

const roleLabel = computed(() => getRoleLabel(authSession.value?.role))

const greetingText = computed(() => `Hello！${username.value}`)

const userInfo = computed(() => [groupName.value, roleLabel.value].filter(Boolean).join(' · '))

/** 取得目前群組名稱 */
async function loadGroupName() {
  if (!hasGroup()) {
    groupName.value = ''
    return
  }

  try {
    const { data } = await client.GET('/groups/my')
    groupName.value = data?.groupName?.trim() ?? ''
  } catch {
    groupName.value = ''
  }
}

/** 關閉手機／平板選單 */
function closeMenu() {
  isMenuOpen.value = false
}

/** 切換手機／平板選單 */
function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}

/** 前往指定頁面 */
function goTo(name: string) {
  closeMenu()
  void router.push({ name })
}

/** 登出 */
function handleLogout() {
  closeMenu()
  logoutAuth()
  void router.push({ name: 'home' })
}

onClickOutside(headerRef, closeMenu)

watch(() => route.fullPath, closeMenu)

watch(isCompactLayout, (isCompact) => {
  if (!isCompact) {
    closeMenu()
  }
})

onMounted(loadGroupName)
</script>

<template>
  <header ref="headerRef" class="sticky top-0 z-50 border-b bg-background/95">
    <div class="flex w-full items-center justify-between gap-3 px-3 py-2 sm:px-4">
      <!-- 左側 -->
      <div class="flex min-w-0 flex-1 items-center gap-3 overflow-hidden sm:gap-5 xl:gap-7">
        <RouterLink :to="{ name: 'random-restaurant' }" class="shrink-0">
          <img
            :src="publicAsset('images/logo.png')"
            alt="Mogumogu Pick"
            class="h-12 w-auto max-w-40 object-contain object-left sm:h-14 sm:max-w-48 md:h-16 xl:max-w-none"
          />
        </RouterLink>

        <!-- Desktop Navigation -->
        <nav class="hidden min-w-0 items-center gap-1.5 lg:flex xl:gap-2">
          <PrimaryButton
            v-for="item in navItems"
            :key="item.name"
            class="px-2 xl:px-2.5"
            variant="standard"
            @click="goTo(item.name)"
          >
            {{ item.label }}
          </PrimaryButton>
        </nav>
      </div>

      <!-- 右側 -->
      <div class="flex shrink-0 items-center gap-2 sm:gap-4 xl:gap-5">
        <!-- Desktop User Info -->
        <div
          class="hidden min-w-0 max-w-36 text-right font-bold lg:block xl:max-w-52"
        >
          <div class="truncate" :title="greetingText">
            {{ greetingText }}
          </div>

          <div class="flex min-w-0 items-center justify-end" :title="userInfo">
            <span v-if="groupName" class="min-w-0 truncate">
              {{ groupName }}
            </span>

            <span v-if="groupName" class="shrink-0"> &nbsp;·&nbsp; </span>

            <span class="shrink-0">
              {{ roleLabel }}
            </span>
          </div>
        </div>

        <PrimaryButton
          class="min-w-16 px-3 sm:min-w-20 sm:px-6"
          variant="outline"
          @click="handleLogout"
        >
          登出
        </PrimaryButton>

        <PrimaryButton
          class="w-11 px-0 lg:hidden"
          variant="outline"
          :aria-expanded="isMenuOpen"
          aria-controls="app-navbar-menu"
          :aria-label="isMenuOpen ? '關閉選單' : '開啟選單'"
          @click="toggleMenu"
        >
          <XIcon v-if="isMenuOpen" class="size-6" />

          <MenuIcon v-else class="size-6" />
        </PrimaryButton>
      </div>
    </div>

    <!-- Mobile / Tablet Menu -->
    <div
      v-show="isMenuOpen"
      id="app-navbar-menu"
      class="border-t bg-background/95 px-4 py-3 lg:hidden"
    >
      <div class="mb-3 font-bold">
        <div>{{ greetingText }}</div>
        <div>{{ userInfo }}</div>
      </div>

      <nav class="flex flex-col gap-2">
        <PrimaryButton
          v-for="item in navItems"
          :key="`mobile-${item.name}`"
          class="w-full"
          variant="standard"
          @click="goTo(item.name)"
        >
          {{ item.label }}
        </PrimaryButton>
      </nav>
    </div>
  </header>
</template>
