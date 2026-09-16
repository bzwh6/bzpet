import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/pets' },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/pets', component: () => import('../views/PetList.vue') },
  { path: '/pets/:id', component: () => import('../views/PetDetail.vue') },
  { path: '/publish', component: () => import('../views/PetPublish.vue'), meta: { auth: true } },
  { path: '/my-applications', component: () => import('../views/MyApplications.vue'), meta: { auth: true } },
  { path: '/received-applications', component: () => import('../views/ReceivedApplications.vue'), meta: { auth: true } },
  { path: '/notifications', component: () => import('../views/Notifications.vue'), meta: { auth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 需要登录的页面做守卫
router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
