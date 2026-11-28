<script setup lang="ts">
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from '@/components/ui/navigation-menu'
import AppButton from '@/components/common/AppButton.vue'
import { Icon } from '@iconify/vue'
</script>

<template>
  <header class="app-header">
    <RouterLink to="/">
      <img src="@/assets/images/logo.png" alt="Mogumogu Pick" class="logo" />
    </RouterLink>

    <div class="header-actions">
      <NavigationMenu class="app-nav">
        <NavigationMenuList>
          <NavigationMenuItem>
            <NavigationMenuLink as-child :class="navigationMenuTriggerStyle()">
              <RouterLink to="/restaurants/random">抽選餐廳</RouterLink>
            </NavigationMenuLink>

            <NavigationMenuLink as-child :class="navigationMenuTriggerStyle()">
              <RouterLink to="/groups">群組管理</RouterLink>
            </NavigationMenuLink>
          </NavigationMenuItem>
        </NavigationMenuList>
      </NavigationMenu>

      <AppButton> <Icon icon="lucide:log-out" />登出 </AppButton>

      <AppButton as-child>
        <a
          href="https://github.com/InagakiRomi/mogumogu-pick"
          target="_blank"
          rel="noopener noreferrer"
        >
          <Icon icon="simple-icons:github" />GitHub
        </a>
      </AppButton>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  gap: 24px;
  padding: 0 24px;
  background-color: var(--surface);
}

.logo {
  width: 240px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

/* 導覽選單 */
.app-nav :deep(a) {
  color: var(--text-color);
}

/* 滑鼠移入 */
.app-nav :deep(a:hover) {
  color: #a66b35;
}
</style>
