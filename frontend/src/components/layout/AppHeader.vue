<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import client from '@/api/client'
import { authSession, clearAuthSession, hasGroup } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import { getRoleLabel } from '@/lib/userRole'
import NinePatchBox from '@/components/nine-patch/NinePatchBox.vue'
import WarmButton from '@/components/warm/WarmButton.vue'

const headerTiles = {
  tl: '/images/header-patch/tl.png',
  t: '/images/header-patch/t.png',
  tr: '/images/header-patch/tr.png',
  l: '/images/header-patch/l.png',
  c: '/images/header-patch/c.png',
  r: '/images/header-patch/r.png',
  bl: '/images/header-patch/bl.png',
  b: '/images/header-patch/b.png',
  br: '/images/header-patch/br.png',
} as const

/** header 內容區留白（px），與 tile-size 無關，可單獨調整 */
const headerContentInset = {
  top: 32,
  right: 40,
  bottom: 32,
  left: 40,
} as const

const router = useRouter()
const groupName = ref('')

const username = computed(() => authSession.value?.username ?? '使用者')
const roleLabel = computed(() => getRoleLabel(authSession.value?.role))

const navItems = [
  { label: '抽餐廳', name: 'random-restaurant' },
  { label: '餐廳一覽', name: 'list-restaurant' },
  { label: '歷史紀錄', name: 'restaurant-history' },
  { label: '分類管理', name: 'category-management' },
  { label: '成員管理', name: 'member-management' },
] as const

async function loadGroupName() {
  if (!hasGroup()) {
    groupName.value = ''
    return
  }

  const { data, error } = await client.GET('/groups/my')
  if (error) {
    groupName.value = ''
    return
  }

  groupName.value = data?.groupName?.trim() ?? ''
}

function handleLogout() {
  authToken.value = null
  clearAuthSession()
  router.push({ name: 'home' })
}

onMounted(() => {
  void loadGroupName()
})
</script>

<template>
  <NinePatchBox
    tag="header"
    :tiles="headerTiles"
    :tile-size="64"
    :content-inset="headerContentInset"
    root-class="shadow-[0_4px_14px_rgba(95,57,41,0.18)]"
    class="flex flex-wrap items-center justify-between gap-2 text-[#4a2c2a] md:flex-col md:items-center md:gap-3 lg:flex-row lg:items-center lg:justify-between"
  >
    <div class="flex w-full flex-col gap-0 md:w-full md:items-center lg:w-auto lg:items-start">
      <p
        class="text-center text-[1.25rem] font-bold tracking-wide text-[#4a2c2a] [text-shadow:0_1px_1px_rgba(255,255,255,0.4)] md:text-[1.8rem] lg:text-left"
      >
        Hello！{{ username }}
      </p>
      <p
        v-if="groupName"
        class="text-center text-[0.95rem] font-semibold md:text-[1.1rem] lg:text-left"
      >
        團隊：{{ groupName }}
      </p>
      <p class="text-center text-[0.95rem] font-semibold md:text-[1.1rem] lg:text-left">
        帳號權限：{{ roleLabel }}
      </p>
    </div>

    <nav class="w-full lg:w-auto">
      <ul
        class="flex flex-col gap-2 md:flex-row md:flex-wrap md:items-center md:justify-center md:gap-4 lg:justify-end"
      >
        <li v-for="item in navItems" :key="item.name" class="w-full md:w-auto">
          <RouterLink v-slot="{ isActive, href, navigate }" :to="{ name: item.name }" custom>
            <WarmButton
              as="a"
              :href="href"
              variant="nav"
              :active="isActive"
              class="w-full"
              @click="navigate"
            >
              {{ item.label }}
            </WarmButton>
          </RouterLink>
        </li>
        <li class="w-full md:w-auto">
          <WarmButton
            type="button"
            variant="compact"
            class="w-full md:w-auto"
            @click="handleLogout"
          >
            登出
          </WarmButton>
        </li>
      </ul>
    </nav>
  </NinePatchBox>
</template>
