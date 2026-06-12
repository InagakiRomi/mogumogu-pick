<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import client from '@/api/client'
import { AUTH_FEEDBACK_MESSAGES, getApiErrorMessage } from '@/lib/apiErrorMessage'
import { setAuthSession, hasGroup } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import WarmButton from '@/components/warm/WarmButton.vue'
import WarmPanel from '@/components/warm/WarmPanel.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { useServerConnection } from '@/composables/useServerConnection'
import WarmSelectTrigger from '@/components/warm/WarmSelectTrigger.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

type AuthTab = 'login' | 'register'

const authLabelClass = 'font-bold text-muted-foreground'

const authFieldClass =
  'h-[42px] rounded-md border-border bg-muted/90 px-2.5 py-0 text-base text-popover-foreground shadow-[inset_0_1px_2px_rgba(121,73,52,0.12)] placeholder:text-[rgba(118,78,60,0.72)] focus-visible:border-[rgba(168,98,68,0.75)] focus-visible:bg-[rgba(255,239,227,0.95)] focus-visible:shadow-[0_0_0_3px_rgba(238,175,143,0.26),inset_0_1px_3px_rgba(121,73,52,0.14)]'

const authTabsTriggerClass =
  'h-auto min-h-0 cursor-pointer select-none px-3 py-2 text-sm font-semibold text-[rgba(118,78,60,0.58)] hover:text-[rgba(95,57,41,0.78)] data-active:rounded-md data-active:border data-active:border-[rgba(186,118,88,0.55)] data-active:bg-linear-to-br data-active:from-white data-active:to-[rgba(255,225,205,0.98)] data-active:text-[rgba(78,42,28,0.95)] data-active:shadow-[0_2px_10px_rgba(95,57,41,0.22)]'

const router = useRouter()
const activeTab = ref<AuthTab>('login')
const isLoading = ref(false)
const { showFeedback, clearFeedback } = useFeedbackDialog()
const { status: serverStatus, checkConnection } = useServerConnection()

const serverStatusMessage = computed(() => {
  switch (serverStatus.value) {
    case 'checking':
      return '正在連接伺服器...'
    case 'connected':
      return '伺服器連線成功'
    case 'disconnected':
    default:
      return '伺服器連線中斷，請稍後等待伺服器啟動並重新檢查'
  }
})

const serverStatusClass = computed(() => {
  switch (serverStatus.value) {
    case 'checking':
      return 'border-[rgba(186,134,88,0.45)] bg-[rgba(255,244,228,0.92)] text-[rgba(95,57,41,0.88)]'
    case 'connected':
      return 'border-[rgba(168,138,98,0.4)] bg-[rgba(255,247,238,0.92)] text-[rgba(92,68,48,0.9)]'
    case 'disconnected':
    default:
      return 'border-[rgba(186,88,88,0.45)] bg-[rgba(255,236,232,0.94)] text-[rgba(120,42,36,0.94)]'
  }
})

const loginForm = ref({
  email: '',
  password: '',
})

const registerForm = ref({
  username: '',
  email: '',
  password: '',
  role: '1',
})

async function handleLogin() {
  clearFeedback()
  isLoading.value = true

  const { data, error } = await client.POST('/auth/login', {
    body: {
      email: loginForm.value.email.trim(),
      password: loginForm.value.password,
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, AUTH_FEEDBACK_MESSAGES.login.fallback))
    isLoading.value = false
    return
  }

  const token = data?.token?.trim()
  if (!token) {
    showFeedback(AUTH_FEEDBACK_MESSAGES.login.missingToken)
    isLoading.value = false
    return
  }

  authToken.value = token
  setAuthSession(data)
  await router.push({ name: hasGroup() ? 'random-restaurant' : 'no-group' })
  isLoading.value = false
}

async function handleRegister() {
  clearFeedback()
  isLoading.value = true

  const { error } = await client.POST('/auth/register', {
    body: {
      username: registerForm.value.username.trim(),
      email: registerForm.value.email.trim(),
      password: registerForm.value.password,
      role: Number(registerForm.value.role),
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, AUTH_FEEDBACK_MESSAGES.register.fallback))
    isLoading.value = false
    return
  }

  showFeedback(AUTH_FEEDBACK_MESSAGES.register.success, 'success')
  activeTab.value = 'login'
  isLoading.value = false
}
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-cover bg-center bg-no-repeat px-4 pt-5 pb-10 max-md:bg-top max-md:pt-3"
  >
    <div class="mx-auto w-fit text-center max-md:w-[calc(100%-24px)]">
      <img
        class="mx-auto block h-[146px] w-[480px] object-contain max-lg:h-auto max-lg:w-[min(420px,88vw)] max-md:h-auto max-md:w-[min(340px,100%)]"
        src="/images/logo.png"
        alt="MoguMogu"
      />
    </div>

    <div
      class="relative z-10 mx-auto mt-4 flex w-full max-w-[600px] items-center justify-center gap-3 rounded-lg border px-4 py-2.5 text-center text-sm font-semibold shadow-[0_6px_16px_rgba(95,57,41,0.12)] backdrop-blur-sm max-lg:max-w-[90%] max-md:max-w-[calc(100%-24px)] max-md:text-xs"
      :class="serverStatusClass"
      role="status"
      aria-live="polite"
    >
      <span
        class="inline-block size-2.5 shrink-0 translate-y-0.5 rounded-full"
        :class="{
          'animate-pulse bg-[rgba(186,134,88,0.75)]': serverStatus === 'checking',
          'bg-[rgba(64,158,84,0.95)]': serverStatus === 'connected',
          'bg-[rgba(186,72,72,0.9)]': serverStatus === 'disconnected',
        }"
        aria-hidden="true"
      />
      <span>{{ serverStatusMessage }}</span>
      <button
        v-if="serverStatus === 'disconnected'"
        type="button"
        class="shrink-0 rounded-md border border-current/30 px-2 py-0.5 text-xs font-semibold transition-colors hover:bg-black/5"
        @click="checkConnection"
      >
        重新檢查
      </button>
    </div>

    <WarmPanel>
      <Tabs v-model="activeTab" class="w-full">
        <TabsList
          class="grid h-auto w-full grid-cols-2 gap-1 rounded-lg border border-[rgba(198,134,105,0.35)] bg-[rgba(255,245,236,0.75)] p-1 group-data-horizontal/tabs:h-auto"
        >
          <TabsTrigger value="login" :class="authTabsTriggerClass"> 會員登入 </TabsTrigger>
          <TabsTrigger value="register" :class="authTabsTriggerClass"> 建立帳號 </TabsTrigger>
        </TabsList>

        <TabsContent value="login" class="mt-5">
          <form class="space-y-4" @submit.prevent="handleLogin">
            <div class="space-y-2">
              <Label for="login-email" :class="authLabelClass"> 電子郵件 </Label>
              <Input
                id="login-email"
                v-model="loginForm.email"
                :class="authFieldClass"
                type="email"
                autocomplete="email"
                placeholder="you@test.com"
                required
              />
            </div>

            <div class="space-y-2">
              <Label for="login-password" :class="authLabelClass"> 密碼 </Label>
              <Input
                id="login-password"
                v-model="loginForm.password"
                :class="authFieldClass"
                type="password"
                autocomplete="current-password"
                placeholder="請輸入密碼"
                required
              />
            </div>

            <WarmButton type="submit" variant="block" :disabled="isLoading">
              {{ isLoading ? '登入中...' : '登入' }}
            </WarmButton>
          </form>
        </TabsContent>

        <TabsContent value="register" class="mt-5">
          <form class="space-y-4" @submit.prevent="handleRegister">
            <div class="space-y-2">
              <Label for="register-username" :class="authLabelClass"> 使用者名稱 </Label>
              <Input
                id="register-username"
                v-model="registerForm.username"
                :class="authFieldClass"
                type="text"
                autocomplete="username"
                placeholder="請輸入名稱"
                required
              />
            </div>

            <div class="space-y-2">
              <Label for="register-email" :class="authLabelClass"> 電子郵件 </Label>
              <Input
                id="register-email"
                v-model="registerForm.email"
                :class="authFieldClass"
                type="email"
                autocomplete="email"
                placeholder="you@test.com"
                required
              />
            </div>

            <div class="space-y-2">
              <Label for="register-password" :class="authLabelClass"> 密碼 </Label>
              <Input
                id="register-password"
                v-model="registerForm.password"
                :class="authFieldClass"
                type="password"
                autocomplete="new-password"
                placeholder="請輸入密碼"
                required
              />
            </div>

            <div class="space-y-2">
              <Label :class="authLabelClass"> 帳號類型 </Label>
              <Select v-model="registerForm.role">
                <WarmSelectTrigger>
                  <SelectValue placeholder="選擇帳號類型" />
                </WarmSelectTrigger>
                <SelectContent class="border-border bg-card text-popover-foreground">
                  <SelectItem value="1"> 一般使用者 </SelectItem>
                  <SelectItem value="0"> 群組管理員 </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <WarmButton type="submit" variant="block" :disabled="isLoading">
              {{ isLoading ? '註冊中...' : '註冊' }}
            </WarmButton>
          </form>
        </TabsContent>
      </Tabs>
    </WarmPanel>
  </main>
</template>
