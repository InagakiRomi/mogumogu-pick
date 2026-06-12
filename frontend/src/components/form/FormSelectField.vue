<script setup lang="ts" generic="T extends string">
import type { HTMLAttributes } from 'vue'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'

export type SelectOption<T extends string = string> = {
  label: string
  value: T
}

const model = defineModel<T>({ required: true })

const props = withDefaults(
  defineProps<{
    label: string
    options: SelectOption<T>[]
    placeholder?: string
    id?: string
    fieldClass?: HTMLAttributes['class']
    contentClass?: HTMLAttributes['class']
    contentPosition?: 'item-aligned' | 'popper'
  }>(),
  {
    placeholder: '請選擇',
    fieldClass: 'w-[180px]',
    contentClass: 'border-border bg-card text-popover-foreground',
    contentPosition: undefined,
  },
)
</script>

<template>
  <div :class="cn('space-y-2', props.fieldClass)">
    <Label :for="id" class="font-bold text-muted-foreground">{{ label }}</Label>
    <Select v-model="model">
      <SelectTrigger
        :id="id"
        class="h-10 w-full px-3 text-left text-sm rounded-md border border-border bg-muted/90 text-popover-foreground"
      >
        <SelectValue :placeholder="placeholder" />
      </SelectTrigger>
      <SelectContent :position="contentPosition" :class="contentClass">
        <SelectItem v-for="option in options" :key="option.value" :value="option.value">
          {{ option.label }}
        </SelectItem>
      </SelectContent>
    </Select>
  </div>
</template>
