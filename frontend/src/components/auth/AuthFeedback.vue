<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{
  type: 'success' | 'error'
  message: string
}>()
</script>

<template>
  <p
    :class="
      cn(
        'mt-5 flex min-h-[50px] items-center justify-center rounded-xl p-3.5 text-center text-base font-bold tracking-wide opacity-100 transition-opacity duration-600 max-md:rounded-[10px] max-md:p-3',
        type === 'error'
          ? 'border border-[rgba(176,68,68,0.38)] bg-linear-to-br from-[rgba(255,240,240,0.93)] to-[rgba(255,224,224,0.9)] text-destructive shadow-[0_8px_18px_rgba(128,54,54,0.2),inset_0_1px_0_rgba(255,255,255,0.65)]'
          : 'border border-[rgba(76,140,74,0.38)] bg-linear-to-br from-[rgba(240,255,240,0.93)] to-[rgba(224,248,224,0.9)] text-[#3d7a3b] shadow-[0_8px_18px_rgba(54,100,54,0.15),inset_0_1px_0_rgba(255,255,255,0.65)]',
      )
    "
  >
    {{ message }}
  </p>
</template>
