<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <Button class="app-button">
    <slot />
  </Button>
</template>

<style scoped>
.app-button {
  height: 40px;
  padding: 0 20px;
  border-radius: 9999px;
  background-color: #ad5164;
  color: #ffffff;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.app-button:hover {
  background-color: #934253;
}
</style>
