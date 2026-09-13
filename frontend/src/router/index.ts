import { createRouter, createWebHistory } from 'vue-router'
import { authSession, hasGroup } from '@/lib/authSession'
import { authToken } from '@/lib/authToken'
import AuthHomeView from '@/views/AuthHomeView.vue'
import NoGroupView from '@/views/NoGroupView.vue'
import MemberManagementView from '@/views/MemberManagementView.vue'
import CategoryManagementView from '@/views/CategoryManagementView.vue'
import RandomRestaurantView from '@/views/RandomRestaurantView.vue'
import RestaurantHistoryView from '@/views/RestaurantHistoryView.vue'
import RestaurantDetailView from '@/views/RestaurantDetailView.vue'
import RestaurantListView from '@/views/RestaurantListView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: AuthHomeView,
      meta: { hideNavbar: true },
    },
    {
      path: '/no-group',
      name: 'no-group',
      component: NoGroupView,
      meta: { requiresAuth: true, hideNavbar: true },
    },
    {
      path: '/restaurants/random',
      name: 'random-restaurant',
      component: RandomRestaurantView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/restaurants/list',
      name: 'list-restaurant',
      component: RestaurantListView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/restaurants/list/:id',
      name: 'restaurant-detail',
      component: RestaurantDetailView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/restaurants/history',
      name: 'restaurant-history',
      component: RestaurantHistoryView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/members',
      name: 'member-management',
      component: MemberManagementView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/categories',
      name: 'category-management',
      component: CategoryManagementView,
      meta: { requiresAuth: true, requiresGroup: true },
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
