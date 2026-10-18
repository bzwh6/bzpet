import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/pets' },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  // 首页是唯一允许游客访问的页面（数据由网关白名单放行的 /pets/list 提供）
  { path: '/pets', component: () => import('../views/PetList.vue') },
  { path: '/pets/:id', component: () => import('../views/PetDetail.vue'), meta: { auth: true } },
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

// 需要登录的页面做守卫：游客一律拦到登录页，并提示"请先登录"
router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
