<script setup lang="ts">
import PrimaryPanel from '@/components/common/PrimaryPanel.vue'
import { homeBgBackgroundStyle } from '@/lib/utils'
</script>

<template>
  <main
    class="min-h-screen bg-fixed bg-cover bg-center bg-no-repeat px-4 py-6 md:px-6"
    :style="homeBgBackgroundStyle"
  >
    <PrimaryPanel width="full">
      <slot />
    </PrimaryPanel>
    <slot name="overlay" />
  </main>
</template>
