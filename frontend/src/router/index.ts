import { createRouter, createWebHistory } from 'vue-router'
import { authSession, hasGroup } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import AppLayout from '@/layouts/AppLayout.vue'
import AuthHomeView from '@/views/AuthHomeView.vue'
import NoGroupView from '@/views/NoGroupView.vue'
import PlaceholderView from '@/views/PlaceholderView.vue'
import RandomRestaurantView from '@/views/RandomRestaurantView.vue'
import RestaurantListView from '@/views/RestaurantListView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: AuthHomeView,
        },
        {
          path: 'no-group',
          name: 'no-group',
          component: NoGroupView,
          meta: { requiresAuth: true },
        },
        {
          path: 'restaurants/random',
          name: 'random-restaurant',
          component: RandomRestaurantView,
          meta: { requiresAuth: true, requiresGroup: true },
        },
        {
          path: 'restaurants/list',
          name: 'list-restaurant',
          component: RestaurantListView,
          meta: { requiresAuth: true, requiresGroup: true },
        },
        {
          path: 'restaurants/history',
          name: 'restaurant-history',
          component: PlaceholderView,
          meta: { requiresAuth: true, requiresGroup: true, pageTitle: '歷史紀錄' },
        },
        {
          path: 'members',
          name: 'member-management',
          component: PlaceholderView,
          meta: { requiresAuth: true, requiresGroup: true, pageTitle: '成員管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authToken.value) {
    return { name: 'home' }
  }

  if (!authToken.value) {
    return true
  }

  if (!authSession.value) {
    if (to.name === 'home') {
      return true
    }

    authToken.value = null
    return { name: 'home' }
  }

  const userHasGroup = hasGroup()

  if (to.name === 'home') {
    return { name: userHasGroup ? 'random-restaurant' : 'no-group' }
  }

  if (userHasGroup && to.name === 'no-group') {
    return { name: 'random-restaurant' }
  }

  if (!userHasGroup && to.meta.requiresGroup) {
    return { name: 'no-group' }
  }
})

export default router
