<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { computed } from 'vue'
import { Button, buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

type CoffeeButtonVariant = 'auth' | 'compact' | 'standard' | 'nav' | 'outline-standard'

const props = withDefaults(
  defineProps<{
    variant?: CoffeeButtonVariant
    active?: boolean
    class?: HTMLAttributes['class']
  }>(),
  {
    variant: 'standard',
    active: false,
  },
)

const baseClass =
  'rounded-lg bg-linear-to-br from-[#d78867] to-[#c96d57] text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.3)] transition-all duration-300 hover:-translate-y-0.5 hover:from-[#de8f6c] hover:to-[#d3735d] hover:shadow-[0_10px_22px_rgba(138,73,52,0.36)] active:scale-[0.98] active:from-[#cb7b5c] active:to-[#bf644f] active:shadow-[0_3px_8px_rgba(138,73,52,0.15)] disabled:translate-y-0 disabled:scale-100'

const navActiveClass =
  'border-transparent bg-linear-to-br from-[#de8f6c] to-[#c96d57] text-primary-foreground shadow-[0_4px_12px_rgba(138,73,52,0.25)] hover:border-transparent hover:from-[#e59674] hover:to-[#d3735d] hover:text-primary-foreground hover:shadow-[0_6px_16px_rgba(138,73,52,0.3)]'

const outlineBaseClass =
  'rounded-lg border-[rgba(146,80,58,0.32)] bg-linear-to-br from-[rgba(255,252,248,0.98)] to-[rgba(255,236,220,0.95)] text-[#5c4033] shadow-[0_2px_8px_rgba(138,73,52,0.1)] hover:border-[rgba(146,80,58,0.45)] hover:from-[rgba(255,245,235,1)] hover:to-[rgba(255,228,210,0.98)] hover:text-[#4a2c2a] hover:shadow-[0_4px_12px_rgba(138,73,52,0.15)]'

const variantClass = computed(() => {
  switch (props.variant) {
    case 'auth':
      return cn(
        baseClass,
        'mt-7.5 block h-[54px] w-full border border-[rgba(146,80,58,0.28)] px-6 py-0 text-[1.1rem] font-semibold',
      )
    case 'compact':
      return cn(buttonVariants({ size: 'lg' }), baseClass, 'h-11 min-w-[120px] px-8')
    case 'nav':
      return cn(
        buttonVariants({ variant: 'outline', size: 'lg' }),
        outlineBaseClass,
        'h-auto min-h-[40px] px-4 py-2 text-base font-semibold',
        props.active && navActiveClass,
      )
    case 'outline-standard':
      return cn(buttonVariants({ variant: 'outline' }), outlineBaseClass, 'h-11')
    default:
      return cn(baseClass, 'h-11')
  }
})
</script>

<template>
  <Button v-bind="$attrs" :class="cn(variantClass, props.class)">
    <slot />
  </Button>
</template>
