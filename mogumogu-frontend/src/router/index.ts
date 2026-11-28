import { createRouter, createWebHistory } from 'vue-router'
import NoGroupView from '@/views/NoGroupView.vue'
import MemberManagementView from '@/views/MemberManagementView.vue'
import CategoryManagementView from '@/views/CategoryManagementView.vue'
import RandomRestaurantView from '@/views/RandomRestaurantView.vue'
import RestaurantHistoryView from '@/views/RestaurantHistoryView.vue'
import RestaurantDetailView from '@/views/RestaurantDetailView.vue'
import RestaurantListView from '@/views/RestaurantListView.vue'
import RestaurantMapView from '@/views/RestaurantMapView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'auth-home',
      component: () => import('@/views/AuthHomeView.vue'),
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
      path: '/map',
      name: 'map',
      component: RestaurantMapView,
      meta: { requiresAuth: true, requiresGroup: true },
    },
    {
      path: '/groups',
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

export default router
