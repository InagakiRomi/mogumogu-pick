<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <Button
    v-bind="$attrs"
    class="mt-7.5 block h-[54px] w-full rounded-lg border border-[rgba(146,80,58,0.28)] bg-linear-to-br from-[#d78867] to-[#c96d57] px-6 py-0 text-[1.1rem] font-semibold text-primary-foreground shadow-[0_8px_18px_rgba(138,73,52,0.3)] transition-all duration-300 hover:-translate-y-0.5 hover:from-[#de8f6c] hover:to-[#d3735d] hover:shadow-[0_10px_22px_rgba(138,73,52,0.36)] active:scale-[0.98] active:from-[#cb7b5c] active:to-[#bf644f] active:shadow-[0_3px_8px_rgba(138,73,52,0.15)] disabled:translate-y-0 disabled:scale-100"
  >
    <slot />
  </Button>
</template>
