<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuthSession } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import { homeBgBackgroundStyle } from '@/lib/utils'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import PrimaryPanel from '@/components/common/PrimaryPanel.vue'

const router = useRouter()

function handleLogout() {
  authToken.value = null
  clearAuthSession()
  router.push({ name: 'home' })
}
</script>

<template>
  <main
    class="flex min-h-screen items-center justify-center bg-cover bg-center bg-no-repeat px-4 py-6"
    :style="homeBgBackgroundStyle"
  >
    <PrimaryPanel class="mt-0 -translate-y-10">
      <div class="flex flex-col items-center gap-5 text-center">
        <p class="text-[1.2rem] font-semibold">
          還沒有加入任何群組哦，請尋找管理員加入群組
        </p>
        <PrimaryButton
          type="button"
          class="min-w-30 px-8 font-semibold"
          @click="handleLogout"
        >
          登出
        </PrimaryButton>
      </div>
    </PrimaryPanel>
  </main>
</template>
