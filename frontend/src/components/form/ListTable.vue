<script lang="ts">
import { defineComponent, h, type HTMLAttributes, type PropType } from 'vue'
import {
  TableCell as UiTableCell,
  TableHead,
  TableRow as UiTableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'

export const ListTableHead = defineComponent({
  name: 'ListTableHead',
  props: {
    class: {
      type: [String, Object, Array] as PropType<HTMLAttributes['class']>,
      default: undefined,
    },
  },
  setup(props, { slots }) {
    return () =>
      h(TableHead, { class: cn('text-center', props.class) }, slots.default?.())
  },
})

export const ListTableRow = defineComponent({
  name: 'ListTableRow',
  props: {
    class: {
      type: [String, Object, Array] as PropType<HTMLAttributes['class']>,
      default: undefined,
    },
  },
  setup(props, { slots }) {
    return () =>
      h(
        UiTableRow,
        {
          class: cn(
            'h-[52px] hover:bg-[rgba(255,185,135,0.72)]',
            props.class,
          ),
        },
        slots.default?.(),
      )
  },
})

export const ListTableCell = defineComponent({
  name: 'ListTableCell',
  props: {
    class: {
      type: [String, Object, Array] as PropType<HTMLAttributes['class']>,
      default: undefined,
    },
    title: { type: String, default: undefined },
    truncate: { type: Boolean, default: false },
  },
  setup(props, { slots }) {
    return () =>
      h(
        UiTableCell,
        {
          class: cn('h-[52px] py-2 text-center', props.truncate && 'truncate', props.class),
          title: props.title,
        },
        slots.default?.(),
      )
  },
})

export const ListTableActions = defineComponent({
  name: 'ListTableActions',
  setup(_, { slots }) {
    return () => h('div', { class: 'flex h-9 items-center justify-center gap-2' }, slots.default?.())
  },
})
</script>

<script setup lang="ts">
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

const LIST_TABLE_EMPTY_CELL_CLASS = 'py-8 text-center text-muted-foreground'

const props = withDefaults(
  defineProps<{
    isLoading?: boolean
    isEmpty?: boolean
    columnCount: number
    loadingText?: string
    emptyText?: string
  }>(),
  {
    isLoading: false,
    isEmpty: false,
    loadingText: '載入資料中...',
    emptyText: '目前沒有資料',
  },
)
</script>

<template>
  <Table class="table-fixed rounded-md border border-border bg-card">
    <TableHeader>
      <TableRow class="hover:bg-transparent">
        <slot name="header" />
      </TableRow>
    </TableHeader>
    <TableBody>
      <TableRow v-if="props.isLoading">
        <TableCell :colspan="props.columnCount" :class="LIST_TABLE_EMPTY_CELL_CLASS">
          {{ props.loadingText }}
        </TableCell>
      </TableRow>
      <TableRow v-else-if="props.isEmpty">
        <TableCell :colspan="props.columnCount" :class="LIST_TABLE_EMPTY_CELL_CLASS">
          {{ props.emptyText }}
        </TableCell>
      </TableRow>
      <slot v-else />
    </TableBody>
  </Table>
</template>
