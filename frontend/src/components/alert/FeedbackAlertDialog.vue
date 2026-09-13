<script setup lang="ts">
import { computed } from 'vue'
import { AlertDialog, AlertDialogDescription, AlertDialogTitle } from '@/components/ui/alert-dialog'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import WarmAlertDialogShell from '@/components/alert/WarmAlertDialogShell.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'

const { open, message, type, onOpenChange } = useFeedbackDialog()

const title = computed(() => (type.value === 'error' ? '提示' : '完成'))
</script>

<template>
  <AlertDialog :open="open" @update:open="onOpenChange">
    <WarmAlertDialogShell>
      <template #title>
        <AlertDialogTitle class="w-full text-center text-2xl font-bold text-popover-foreground">
          {{ title }}
        </AlertDialogTitle>
      </template>
      <AlertDialogDescription
        class="w-full text-pretty wrap-break-word text-center text-xl font-semibold tracking-wide text-popover-foreground/90"
      >
        {{ message }}
      </AlertDialogDescription>
      <template #actions>
        <PrimaryButton class="min-w-24" @click="onOpenChange(false)"> 確定 </PrimaryButton>
      </template>
    </WarmAlertDialogShell>
  </AlertDialog>
</template>
