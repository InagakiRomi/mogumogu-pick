<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { components } from '@/api/schema'
import client from '@/api/client'
import FormAlertDialog from '@/components/feedback/FormAlertDialog.vue'
import WarmButton from '@/components/warm/WarmButton.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'
import { authSession } from '@/lib/authSession'
import { getRoleLabel, isGroupAdmin } from '@/lib/userRole'

type GroupProfile = components['schemas']['GroupProfileResponse']
type GroupMember = components['schemas']['GroupMemberResponse']
type MemberOrderBy = 'USER_ID' | 'DISPLAY_ORDER_ID'
type SortOrder = 'ASC' | 'DESC'

const orderByOptions: Array<{ label: string; value: MemberOrderBy }> = [
  { label: 'ID', value: 'USER_ID' },
  { label: '排序 ID', value: 'DISPLAY_ORDER_ID' },
]

const sortOptions: Array<{ label: string; value: SortOrder }> = [
  { label: '小到大', value: 'ASC' },
  { label: '大到小', value: 'DESC' },
]

const router = useRouter()
const { showFeedback, clearFeedback } = useFeedbackDialog()

const isLoading = ref(false)
const isSavingGroupName = ref(false)
const isGroupNameDialogOpen = ref(false)
const isAddingMember = ref(false)
const groupProfile = ref<GroupProfile | null>(null)
const members = ref<GroupMember[]>([])
const groupNameInput = ref('')
const targetEmailInput = ref('')
const orderBy = ref<MemberOrderBy>('DISPLAY_ORDER_ID')
const sort = ref<SortOrder>('ASC')

const currentUsername = computed(() => authSession.value?.username ?? '')
const currentUserId = computed(() => authSession.value?.userId ?? null)
const canManageMembers = computed(() => isGroupAdmin(authSession.value?.role))

const sortedMembers = computed(() => {
  const direction = sort.value === 'ASC' ? 1 : -1

  return [...members.value].sort((a, b) => {
    const primaryDiff =
      orderBy.value === 'USER_ID'
        ? (a.userId ?? 0) - (b.userId ?? 0)
        : (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)

    if (primaryDiff !== 0) {
      return primaryDiff * direction
    }

    const secondaryDiff =
      orderBy.value === 'USER_ID'
        ? (a.displayOrderId ?? 0) - (b.displayOrderId ?? 0)
        : (a.userId ?? 0) - (b.userId ?? 0)

    return secondaryDiff * direction
  })
})

function isCurrentUser(member: GroupMember) {
  if (member.userId != null && currentUserId.value != null) {
    return member.userId === currentUserId.value
  }
  return member.username === currentUsername.value
}

async function fetchGroupProfile() {
  const { data, error } = await client.GET('/groups/my')
  if (error) {
    throw error
  }

  groupProfile.value = data ?? null
}

async function fetchMembers() {
  const { data, error } = await client.GET('/groups/my/members')
  if (error) {
    throw error
  }

  members.value = Array.isArray(data) ? data : []
}

async function fetchPageData() {
  isLoading.value = true
  const [groupProfileResult, membersResult] = await Promise.allSettled([
    fetchGroupProfile(),
    fetchMembers(),
  ])
  if (groupProfileResult.status === 'rejected') {
    showFeedback(getApiErrorMessage(groupProfileResult.reason, '載入成員資料失敗'))
    isLoading.value = false
    return
  }
  if (membersResult.status === 'rejected') {
    showFeedback(getApiErrorMessage(membersResult.reason, '載入成員資料失敗'))
    isLoading.value = false
    return
  }
  isLoading.value = false
}

function openGroupNameDialog() {
  groupNameInput.value = groupProfile.value?.groupName?.trim() ?? ''
  isGroupNameDialogOpen.value = true
}

function closeGroupNameDialog() {
  isGroupNameDialogOpen.value = false
}

function handleGroupNameDialogOpenChange(open: boolean) {
  isGroupNameDialogOpen.value = open
}

async function updateGroupName() {
  const groupName = groupNameInput.value.trim()
  if (!groupName) {
    showFeedback('請輸入團隊名稱')
    return
  }

  clearFeedback()
  isSavingGroupName.value = true

  const { error } = await client.PATCH('/groups/my', {
    body: { groupName },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, '載入成員資料失敗'))
    isSavingGroupName.value = false
    return
  }

  if (groupProfile.value) {
    groupProfile.value.groupName = groupName
  }
  closeGroupNameDialog()
  showFeedback('團隊名稱已更新', 'success')
  isSavingGroupName.value = false
}

async function addMemberByEmail() {
  const email = targetEmailInput.value.trim()
  if (!email) {
    showFeedback('請輸入電子郵件')
    return
  }

  clearFeedback()
  isAddingMember.value = true

  const { error } = await client.POST('/groups/my/members', {
    body: { email },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, '新增成員失敗'))
    isAddingMember.value = false
    return
  }

  targetEmailInput.value = ''
  showFeedback('成員已加入', 'success')
  await fetchMembers()
  isAddingMember.value = false
}

async function removeMember(member: GroupMember) {
  const memberUserId = member.userId
  if (memberUserId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  const memberName = member.username?.trim() || `ID ${memberUserId}`
  const confirmed = window.confirm(`確定要將成員 ${memberName} 移出群組嗎？`)
  if (!confirmed) {
    return
  }

  const { error } = await client.DELETE('/groups/my/members/{userId}', {
    params: {
      path: {
        userId: memberUserId,
      },
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, '移除成員失敗'))
    return
  }

  showFeedback('成員已移出群組', 'success')
  await fetchMembers()
}

async function transferAdmin(member: GroupMember) {
  const memberUserId = member.userId
  if (memberUserId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  const memberName = member.username?.trim() || `ID ${memberUserId}`
  const confirmed = window.confirm(`確定要將管理權轉移給 ${memberName} 嗎？`)
  if (!confirmed) {
    return
  }

  const { error } = await client.POST('/groups/my/transfer-admin', {
    body: {
      targetUserId: memberUserId,
    },
  })

  if (error) {
    showFeedback(getApiErrorMessage(error, '轉移管理權失敗'))
    return
  }

  if (authSession.value) {
    authSession.value = {
      ...authSession.value,
      role: 1,
    }
  }

  showFeedback('管理權已轉移', 'success')
  await fetchMembers()
}

async function leaveGroup() {
  const confirmed = window.confirm('確定要退出群組嗎？')
  if (!confirmed) {
    return
  }

  const { error } = await client.POST('/groups/my/leave', {})
  if (error) {
    showFeedback(getApiErrorMessage(error, '退出群組失敗'))
    return
  }

  if (authSession.value) {
    authSession.value = {
      ...authSession.value,
      groupId: null,
    }
  }

  showFeedback('你已退出群組', 'success')
  await router.push({ name: 'no-group' })
}

onMounted(() => {
  void fetchPageData()
})
</script>

<template>
  <main
    class="min-h-screen bg-[linear-gradient(rgba(255,255,255,0.24),rgba(255,255,255,0.24)),url('/images/homeBg.jpg')] bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
  >
    <div
      class="relative z-10 mx-auto mt-6 w-full rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-[30px] pt-[30px] pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6"
    >
      <div class="space-y-6">
        <section class="space-y-3 rounded-lg border border-border bg-card/70 p-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <p class="text-lg font-bold text-card-foreground">
              團隊名稱：{{ groupProfile?.groupName || '-' }}
            </p>
            <WarmButton
              v-if="canManageMembers"
              :disabled="isLoading"
              @click="openGroupNameDialog"
            >
              更新團隊名稱
            </WarmButton>
          </div>
        </section>

        <section class="space-y-4 rounded-lg border border-border bg-card/70 p-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-lg font-bold text-card-foreground">成員列表</h2>
            <p class="text-sm text-muted-foreground">共 {{ members.length }} 位成員</p>
          </div>

          <div class="flex flex-wrap items-end gap-3">
            <div class="w-[180px] space-y-2">
              <Label class="font-bold text-muted-foreground">排序欄位</Label>
              <Select v-model="orderBy">
                <SelectTrigger
                  class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
                >
                  <SelectValue placeholder="選擇排序欄位" />
                </SelectTrigger>
                <SelectContent class="border-border bg-card text-popover-foreground">
                  <SelectItem v-for="option in orderByOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div class="w-[180px] space-y-2">
              <Label class="font-bold text-muted-foreground">排序方向</Label>
              <Select v-model="sort">
                <SelectTrigger
                  class="h-10 w-full rounded-md border border-border bg-muted/90 px-3 text-left text-sm text-popover-foreground"
                >
                  <SelectValue placeholder="選擇排序方向" />
                </SelectTrigger>
                <SelectContent class="border-border bg-card text-popover-foreground">
                  <SelectItem v-for="option in sortOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div v-if="canManageMembers" class="flex flex-wrap items-end gap-3">
            <div class="min-w-[240px] grow space-y-2">
              <Label for="target-email" class="font-bold text-muted-foreground">新增成員（電子郵件）</Label>
              <Input
                id="target-email"
                v-model="targetEmailInput"
                type="email"
                autocomplete="email"
                class="h-10"
                placeholder="輸入要加入的電子郵件"
                @keyup.enter="addMemberByEmail"
              />
            </div>
            <WarmButton
              :disabled="isLoading || isAddingMember"
              @click="addMemberByEmail"
            >
              {{ isAddingMember ? '加入中...' : '加入成員' }}
            </WarmButton>
          </div>

          <Table class="table-fixed rounded-md border border-border bg-card">
            <TableHeader>
              <TableRow>
                <TableHead class="w-[84px] text-center">ID</TableHead>
                <TableHead class="w-[80px] text-center">排序 ID</TableHead>
                <TableHead class="text-center">名稱</TableHead>
                <TableHead class="w-[120px] text-center">角色</TableHead>
                <TableHead class="w-[300px] text-center">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="isLoading">
                <TableCell colspan="5" class="py-8 text-center text-muted-foreground">
                  載入資料中...
                </TableCell>
              </TableRow>
              <TableRow v-else-if="sortedMembers.length === 0">
                <TableCell colspan="5" class="py-8 text-center text-muted-foreground">
                  目前沒有成員資料
                </TableCell>
              </TableRow>
              <TableRow
                v-for="member in sortedMembers"
                :key="member.userId ?? member.username"
                class="h-[52px]"
              >
                <TableCell class="h-[52px] py-2 text-center">{{ member.userId ?? '-' }}</TableCell>
                <TableCell class="h-[52px] py-2 text-center">{{ member.displayOrderId ?? '-' }}</TableCell>
                <TableCell
                  class="h-[52px] truncate py-2 text-center"
                  :title="member.username ?? undefined"
                >
                  {{ member.username ?? '-' }}
                </TableCell>
                <TableCell class="h-[52px] py-2 text-center">
                  {{ getRoleLabel(member.role) }}
                </TableCell>
                <TableCell class="h-[52px] py-2 text-center">
                  <div class="flex h-9 items-center justify-center gap-2">
                    <template v-if="canManageMembers">
                      <template v-if="isCurrentUser(member)">
                        <span class="inline-flex h-9 items-center text-sm text-muted-foreground">-</span>
                      </template>
                      <template v-else>
                        <WarmButton
                          variant="outline-standard"
                          class="h-9 px-3 text-sm"
                          :disabled="member.role === 0"
                          @click="transferAdmin(member)"
                        >
                          轉移管理員
                        </WarmButton>
                        <WarmButton
                          variant="outline-standard"
                          class="h-9 px-3 text-sm"
                          @click="removeMember(member)"
                        >
                          刪除成員
                        </WarmButton>
                      </template>
                    </template>
                    <template v-else>
                      <WarmButton
                        v-if="isCurrentUser(member)"
                        variant="outline-standard"
                        class="h-9 px-3 text-sm"
                        @click="leaveGroup"
                      >
                        退出群組
                      </WarmButton>
                      <span v-else class="inline-flex h-9 items-center text-sm text-muted-foreground">-</span>
                    </template>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </section>
      </div>
    </div>

    <FormAlertDialog
      :open="isGroupNameDialogOpen"
      title="更新團隊名稱"
      submit-label="更新"
      :loading="isSavingGroupName"
      loading-label="更新中..."
      :can-submit="groupNameInput.trim().length > 0"
      @update:open="handleGroupNameDialogOpenChange"
      @submit="updateGroupName"
      @cancel="closeGroupNameDialog"
    >
      <div class="space-y-2">
        <Label for="group-name-dialog" class="font-bold text-muted-foreground">團隊名稱</Label>
        <Input
          id="group-name-dialog"
          v-model="groupNameInput"
          maxlength="64"
          class="h-10"
          placeholder="輸入團隊名稱"
        />
      </div>
    </FormAlertDialog>
  </main>
</template>
