<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuthSession } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import CoffeeButton from '@/components/coffee/CoffeeButton.vue'

const router = useRouter()

function handleLogout() {
  authToken.value = null
  clearAuthSession()
  router.push({ name: 'home' })
}
</script>

<template>
  <main class="flex min-h-screen flex-col items-center justify-center gap-5 px-4 text-center">
    <p class="text-[1.2rem] font-semibold text-[#5c4033]">
      還沒有加入任何群組哦，請尋找管理員加入群組
    </p>
    <CoffeeButton type="button" variant="compact" @click="handleLogout">登出</CoffeeButton>
  </main>
</template>
