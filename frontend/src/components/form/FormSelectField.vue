<script setup lang="ts" generic="T extends string">
import { Label } from '@/components/ui/label'
import PrimarySelectTrigger from '@/components/common/PrimarySelectTrigger.vue'
import PrimarySelectContent from '@/components/common/PrimarySelectContent.vue'
import { Select, SelectItem, SelectValue } from '@/components/ui/select'

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
    placeholder: '',
  },
)
</script>

<template>
  <div class="w-full min-w-0 space-y-2 sm:w-45">
    <Label :for="id" class="font-bold text-muted-foreground">{{ label }}</Label>
    <Select v-model="model">
      <PrimarySelectTrigger :id="id">
        <SelectValue :placeholder="placeholder" />
      </PrimarySelectTrigger>
      <PrimarySelectContent
        position="popper"
        align="start"
        class="form-select-content w-(--reka-select-trigger-width) max-w-(--reka-select-trigger-width) border-border bg-card text-popover-foreground"
      >
        <SelectItem v-for="option in options" :key="option.value" :value="option.value">
          {{ option.label }}
        </SelectItem>
      </PrimarySelectContent>
    </Select>
  </div>
</template>

<style scoped>
:global([data-slot='select-content'].form-select-content [data-position='popper']) {
  height: auto;
  max-height: min(16rem, var(--reka-select-content-available-height, 16rem));
}
</style>
