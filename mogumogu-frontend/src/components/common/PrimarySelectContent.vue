<script setup lang="ts">
import { SelectContent } from '@/components/ui/select'

defineOptions({
  inheritAttrs: false,
})
</script>

<template>
  <SelectContent v-bind="$attrs" :body-lock="false">
    <slot />
  </SelectContent>
</template>
