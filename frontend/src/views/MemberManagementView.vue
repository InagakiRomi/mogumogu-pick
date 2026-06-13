<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { components } from '@/api/schema'
import client from '@/api/client'
import ConfirmAlertDialog from '@/components/feedback/ConfirmAlertDialog.vue'
import FormAlertDialog from '@/components/feedback/FormAlertDialog.vue'
import FormSelectField from '@/components/form/FormSelectField.vue'
import ListPagePanel from '@/components/form/ListPagePanel.vue'
import ListSection from '@/components/form/ListSection.vue'
import ListTable, {
  ListTableActions,
  ListTableCell,
  ListTableHead,
  ListTableRow,
} from '@/components/form/ListTable.vue'
import WarmButton from '@/components/warm/WarmButton.vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'
import { authSession, logoutAuth } from '@/lib/authSession'
import { getRoleLabel, isGroupAdmin } from '@/lib/userRole'

const FORM_LABEL_CLASS = 'font-bold text-muted-foreground'
const FORM_INPUT_CLASS =
  'h-10 px-2.5 text-sm rounded-md border border-border bg-muted/90 text-popover-foreground'
const FORM_TOOLBAR_CLASS = 'flex flex-wrap items-end gap-3'
const DEFAULT_SORT_OPTIONS = [
  { label: '小到大', value: 'ASC' as const },
  { label: '大到小', value: 'DESC' as const },
]

type GroupProfile = components['schemas']['GroupProfileResponse']
type GroupMember = components['schemas']['GroupMemberResponse']
type MemberOrderBy = 'USER_ID' | 'DISPLAY_ORDER_ID'
type SortOrder = 'ASC' | 'DESC'

const memberListForm = {
  defaultOrderBy: 'DISPLAY_ORDER_ID' as MemberOrderBy,
  defaultSort: 'ASC' as SortOrder,
  orderByOptions: [
    { label: 'ID', value: 'USER_ID' as const },
    { label: '排序 ID', value: 'DISPLAY_ORDER_ID' as const },
  ],
  sortOptions: DEFAULT_SORT_OPTIONS,
  loadingText: '載入資料中...',
  emptyText: '目前沒有成員資料',
}

const router = useRouter()
const { showFeedback, clearFeedback } = useFeedbackDialog()

const isLoading = ref(false)
const isSavingGroupName = ref(false)
const isGroupNameDialogOpen = ref(false)
const isRemoveMemberDialogOpen = ref(false)
const isTransferAdminDialogOpen = ref(false)
const isAddingMember = ref(false)
const isRemovingMember = ref(false)
const isTransferringAdmin = ref(false)
const removingMember = ref<GroupMember | null>(null)
const transferTargetMember = ref<GroupMember | null>(null)
const groupProfile = ref<GroupProfile | null>(null)
const members = ref<GroupMember[]>([])
const groupNameInput = ref('')
const targetEmailInput = ref('')
const orderBy = ref<MemberOrderBy>(memberListForm.defaultOrderBy)
const sort = ref<SortOrder>(memberListForm.defaultSort)

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

  try {
    const { error } = await client.PATCH('/groups/my', {
      body: { groupName },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '載入成員資料失敗'))
      return
    }

    if (groupProfile.value) {
      groupProfile.value.groupName = groupName
    }
    showFeedback('團隊名稱已更新', 'success')
  } finally {
    closeGroupNameDialog()
    isSavingGroupName.value = false
  }
}

async function addMemberByEmail() {
  const email = targetEmailInput.value.trim()
  if (!email) {
    showFeedback('請輸入電子郵件')
    return
  }

  clearFeedback()
  isAddingMember.value = true

  try {
    const { error } = await client.POST('/groups/my/members', {
      body: { email },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '新增成員失敗'))
      return
    }

    targetEmailInput.value = ''
    showFeedback('成員已加入', 'success')
    await fetchMembers()
  } finally {
    isAddingMember.value = false
  }
}

function openRemoveMemberDialog(member: GroupMember) {
  if (member.userId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  removingMember.value = member
  isRemoveMemberDialogOpen.value = true
}

async function handleRemoveMember() {
  const memberUserId = removingMember.value?.userId
  if (memberUserId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  clearFeedback()
  isRemovingMember.value = true

  try {
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
  } finally {
    isRemoveMemberDialogOpen.value = false
    removingMember.value = null
    isRemovingMember.value = false
  }
}

function openTransferAdminDialog(member: GroupMember) {
  if (member.userId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  transferTargetMember.value = member
  isTransferAdminDialogOpen.value = true
}

async function handleTransferAdmin() {
  const memberUserId = transferTargetMember.value?.userId
  if (memberUserId == null) {
    showFeedback('找不到目標使用者 ID')
    return
  }

  clearFeedback()
  isTransferringAdmin.value = true

  try {
    const { error } = await client.POST('/groups/my/transfer-admin', {
      body: {
        targetUserId: memberUserId,
      },
    })

    if (error) {
      showFeedback(getApiErrorMessage(error, '轉移管理權失敗'))
      return
    }

    logoutAuth()
    showFeedback('管理權已轉移成功，將自動登出', 'success', () => {
      void router.push({ name: 'home' })
    })
  } finally {
    isTransferAdminDialogOpen.value = false
    transferTargetMember.value = null
    isTransferringAdmin.value = false
  }
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
  <ListPagePanel>
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

        <ListSection title="成員列表" :summary="`共 ${members.length} 位成員`">
          <div :class="FORM_TOOLBAR_CLASS">
            <FormSelectField
              v-model="orderBy"
              label="排序欄位"
              :options="memberListForm.orderByOptions"
              placeholder="選擇排序欄位"
            />
            <FormSelectField
              v-model="sort"
              label="排序方向"
              :options="memberListForm.sortOptions"
              placeholder="選擇排序方向"
            />
          </div>

          <div v-if="canManageMembers" :class="FORM_TOOLBAR_CLASS">
            <div class="min-w-[240px] grow space-y-2">
              <Label for="target-email" :class="FORM_LABEL_CLASS">新增成員（電子郵件）</Label>
              <Input
                id="target-email"
                v-model="targetEmailInput"
                type="email"
                autocomplete="email"
                :class="FORM_INPUT_CLASS"
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

          <ListTable
            :is-loading="isLoading"
            :is-empty="sortedMembers.length === 0"
            :column-count="5"
            :loading-text="memberListForm.loadingText"
            :empty-text="memberListForm.emptyText"
          >
            <template #header>
              <ListTableHead class="w-[84px]">ID</ListTableHead>
              <ListTableHead class="w-[80px]">排序 ID</ListTableHead>
              <ListTableHead>名稱</ListTableHead>
              <ListTableHead class="w-[120px]">角色</ListTableHead>
              <ListTableHead class="w-[300px]">操作</ListTableHead>
            </template>

            <ListTableRow
              v-for="member in sortedMembers"
              :key="member.userId ?? member.username"
            >
              <ListTableCell>{{ member.userId ?? '-' }}</ListTableCell>
              <ListTableCell>{{ member.displayOrderId ?? '-' }}</ListTableCell>
              <ListTableCell truncate :title="member.username ?? undefined">
                {{ member.username ?? '-' }}
              </ListTableCell>
              <ListTableCell>{{ getRoleLabel(member.role) }}</ListTableCell>
              <ListTableCell>
                <ListTableActions>
                  <template v-if="canManageMembers">
                    <template v-if="isCurrentUser(member)">
                      <span class="inline-flex h-9 items-center text-sm text-muted-foreground">-</span>
                    </template>
                    <template v-else>
                      <WarmButton
                        variant="outline-standard"
                        class="h-9 px-3 text-sm"
                        :disabled="member.role === 0"
                        @click="openTransferAdminDialog(member)"
                      >
                        轉移管理員
                      </WarmButton>
                      <WarmButton
                        variant="outline-standard"
                        class="h-9 px-3 text-sm"
                        @click="openRemoveMemberDialog(member)"
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
                </ListTableActions>
              </ListTableCell>
            </ListTableRow>
          </ListTable>
        </ListSection>
    </div>

    <template #overlay>
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
        <Label for="group-name-dialog" :class="FORM_LABEL_CLASS">團隊名稱</Label>
        <Input
          id="group-name-dialog"
          v-model="groupNameInput"
          maxlength="64"
          :class="FORM_INPUT_CLASS"
          placeholder="輸入團隊名稱"
        />
      </div>
      </FormAlertDialog>

      <ConfirmAlertDialog
        v-model:open="isRemoveMemberDialogOpen"
        title="確認移出成員？"
        confirm-label="確認移出"
        loading-label="移出中..."
        :loading="isRemovingMember"
        @confirm="handleRemoveMember"
      >
        確定要將成員「{{
          removingMember?.username?.trim() || `ID ${removingMember?.userId ?? ''}`
        }}」移出群組嗎？
      </ConfirmAlertDialog>

      <ConfirmAlertDialog
        v-model:open="isTransferAdminDialogOpen"
        title="確認轉移管理權？"
        confirm-label="確認轉移"
        loading-label="轉移中..."
        :loading="isTransferringAdmin"
        @confirm="handleTransferAdmin"
      >
        確定要將管理權轉移給「{{
          transferTargetMember?.username?.trim() || `ID ${transferTargetMember?.userId ?? ''}`
        }}」嗎？
      </ConfirmAlertDialog>
    </template>
  </ListPagePanel>
</template>
