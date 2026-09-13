<script setup lang="ts">
import { Button } from '@/components/ui/button'

type PrimaryButtonVariant = 'standard' | 'outline'

const props = withDefaults(
  defineProps<{
    variant?: PrimaryButtonVariant
  }>(),
  {
    variant: 'standard',
  },
)

const variantConfig = {
  standard: {
    buttonVariant: 'default',
    class:
      'from-[#d78867] to-[#c96d57] text-primary-foreground ' +
      'shadow-[0_8px_18px_rgba(138,73,52,0.3)] ' +
      'hover:from-[#de8f6c] hover:to-[#d3735d] ' +
      'hover:shadow-[0_10px_22px_rgba(138,73,52,0.36)] ' +
      'active:from-[#cb7b5c] active:to-[#bf644f] ' +
      'active:shadow-[0_3px_8px_rgba(138,73,52,0.15)]',
  },
  outline: {
    buttonVariant: 'outline',
    class:
      'border-[rgba(146,80,58,0.32)] ' +
      'from-[rgba(255,252,248,0.98)] to-[rgba(255,236,220,0.95)] ' +
      'text-[#5c4033] shadow-[0_2px_8px_rgba(138,73,52,0.1)] ' +
      'hover:border-[rgba(146,80,58,0.45)] ' +
      'hover:from-[rgba(255,245,235,1)] hover:to-[rgba(255,228,210,0.98)] ' +
      'hover:text-[#4a2c2a] ' +
      'hover:shadow-[0_4px_12px_rgba(138,73,52,0.15)]',
  },
} as const
</script>

<template>
  <Button
    :variant="variantConfig[props.variant].buttonVariant"
    :class="[
      'h-11 rounded-lg bg-linear-to-br transition-all duration-300',
      'hover:-translate-y-0.5',
      'active:scale-[0.98]',
      'disabled:translate-y-0 disabled:scale-100',
      variantConfig[props.variant].class,
    ]"
  >
    <slot />
  </Button>
</template>
