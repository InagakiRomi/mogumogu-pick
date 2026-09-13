<script setup lang="ts">
type PrimaryPanelWidth = 'narrow' | 'wide' | 'full'

const props = withDefaults(
  defineProps<{
    width?: PrimaryPanelWidth
  }>(),
  {
    width: 'narrow',
  },
)

const maxWidthClassMap: Record<PrimaryPanelWidth, string> = {
  narrow: 'max-w-150 max-lg:max-w-[90%] max-md:max-w-[calc(100%-24px)]',
  wide: 'max-w-4xl',
  full: '',
}
</script>

<template>
  <div
    :class="[
      'relative z-10 mx-auto mt-6 w-full rounded-[10px] border border-[rgba(226,164,136,0.52)] bg-linear-to-br from-[rgba(255,248,241,0.9)] to-[rgba(255,233,219,0.84)] px-7.5 pt-7.5 pb-8 shadow-[0_14px_32px_rgba(95,57,41,0.24),inset_0_1px_0_rgba(255,255,255,0.55)] backdrop-blur-sm max-lg:mt-5 max-lg:px-6 max-lg:pt-6 max-lg:pb-7 max-md:mt-4 max-md:rounded-lg max-md:px-4 max-md:pt-4 max-md:pb-6',
      maxWidthClassMap[props.width],
    ]"
  >
    <slot />
  </div>
</template>
