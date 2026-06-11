<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type NinePatchTiles = {
  tl: string
  t: string
  tr: string
  l: string
  c: string
  r: string
  bl: string
  b: string
  br: string
}

type ContentInset = {
  top?: number
  right?: number
  bottom?: number
  left?: number
}

const props = withDefaults(
  defineProps<{
    tiles: NinePatchTiles
    tileSize?: number
    contentInset?: ContentInset
    tag?: string
    rootClass?: HTMLAttributes['class']
    class?: HTMLAttributes['class']
  }>(),
  {
    tileSize: 64,
    contentInset: () => ({}),
    tag: 'div',
  },
)

const gridStyle = computed(() => ({
  gridTemplateColumns: `${props.tileSize}px 1fr ${props.tileSize}px`,
  gridTemplateRows: `${props.tileSize}px 1fr ${props.tileSize}px`,
}))

const cornerStyle = computed(() => ({
  width: `${props.tileSize}px`,
  height: `${props.tileSize}px`,
}))

const tileBackgroundSize = computed(() => `${props.tileSize}px ${props.tileSize}px`)

const contentInsetStyle = computed(() => ({
  paddingTop: `${props.contentInset.top ?? 0}px`,
  paddingRight: `${props.contentInset.right ?? 0}px`,
  paddingBottom: `${props.contentInset.bottom ?? 0}px`,
  paddingLeft: `${props.contentInset.left ?? 0}px`,
}))
</script>

<template>
  <component :is="tag" :class="cn('grid', rootClass)" :style="gridStyle">
    <img
      :src="tiles.tl"
      alt=""
      class="pointer-events-none col-start-1 row-start-1 block max-w-none object-fill"
      :style="cornerStyle"
      draggable="false"
      aria-hidden="true"
    />
    <div
      class="pointer-events-none col-start-2 row-start-1 h-full w-full bg-repeat-x bg-top"
      :style="{ backgroundImage: `url(${tiles.t})`, backgroundSize: tileBackgroundSize }"
      aria-hidden="true"
    />
    <img
      :src="tiles.tr"
      alt=""
      class="pointer-events-none col-start-3 row-start-1 block max-w-none object-fill"
      :style="cornerStyle"
      draggable="false"
      aria-hidden="true"
    />

    <div
      class="pointer-events-none col-start-1 row-start-2 h-full w-full bg-repeat-y bg-left"
      :style="{ backgroundImage: `url(${tiles.l})`, backgroundSize: tileBackgroundSize }"
      aria-hidden="true"
    />
    <div
      class="pointer-events-none col-start-2 row-start-2 h-full w-full bg-repeat"
      :style="{ backgroundImage: `url(${tiles.c})`, backgroundSize: tileBackgroundSize }"
      aria-hidden="true"
    />
    <div
      class="pointer-events-none col-start-3 row-start-2 h-full w-full bg-repeat-y bg-right"
      :style="{ backgroundImage: `url(${tiles.r})`, backgroundSize: tileBackgroundSize }"
      aria-hidden="true"
    />

    <img
      :src="tiles.bl"
      alt=""
      class="pointer-events-none col-start-1 row-start-3 block max-w-none object-fill"
      :style="cornerStyle"
      draggable="false"
      aria-hidden="true"
    />
    <div
      class="pointer-events-none col-start-2 row-start-3 h-full w-full bg-repeat-x bg-bottom"
      :style="{ backgroundImage: `url(${tiles.b})`, backgroundSize: tileBackgroundSize }"
      aria-hidden="true"
    />
    <img
      :src="tiles.br"
      alt=""
      class="pointer-events-none col-start-3 row-start-3 block max-w-none object-fill"
      :style="cornerStyle"
      draggable="false"
      aria-hidden="true"
    />

    <div
      :class="
        cn(
          'relative z-10 col-start-1 col-span-3 row-start-1 row-span-3 min-w-0 self-stretch',
          props.class,
        )
      "
      :style="contentInsetStyle"
    >
      <slot />
    </div>
  </component>
</template>
