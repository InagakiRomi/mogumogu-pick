import { createRouter, createWebHistory } from 'vue-router'
import AuthHomeView from '@/views/AuthHomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: AuthHomeView,
    },
  ],
})

export default router
