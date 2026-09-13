<script setup lang="ts" generic="T extends string">
import { Label } from '@/components/ui/label'
import PrimarySelectTrigger from '@/components/common/PrimarySelectTrigger.vue'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select'

type SelectOption<T extends string = string> = {
  label: string
  value: T
}

const model = defineModel<T>({ required: true })

withDefaults(
  defineProps<{
    label: string
    options: SelectOption<T>[]
    placeholder?: string
    id?: string
  }>(),
  {
    placeholder: '請選擇',
  },
)
</script>

<template>
  <div class="w-45 space-y-2">
    <Label :for="id" class="font-bold text-muted-foreground">{{ label }}</Label>
    <Select v-model="model">
      <PrimarySelectTrigger :id="id">
        <SelectValue :placeholder="placeholder" />
      </PrimarySelectTrigger>
      <SelectContent class="border-border bg-card text-popover-foreground">
        <SelectItem v-for="option in options" :key="option.value" :value="option.value">
          {{ option.label }}
        </SelectItem>
      </SelectContent>
    </Select>
  </div>
</template>
