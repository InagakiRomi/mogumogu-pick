import { createRouter, createWebHistory } from 'vue-router'
import { authToken } from '@/lib/authToken'
import AuthHomeView from '@/views/AuthHomeView.vue'
import RandomRestaurantView from '@/views/RandomRestaurantView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: AuthHomeView,
    },
    {
      path: '/restaurants/random',
      name: 'random-restaurant',
      component: RandomRestaurantView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authToken.value) {
    return { name: 'home' }
  }
})

export default router
