<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './store/user'
import { logout } from './api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isLogin = computed(() => !!userStore.token)

async function handleLogout() {
  try {
    await logout(userStore.token)
  } catch (e) {
    // 忽略登出接口异常，本地清除即可
  }
  userStore.clear()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo" @click="router.push('/pets')">🐾 不知宠物</div>
      <el-menu mode="horizontal" :default-active="route.path" router :ellipsis="false" class="menu">
        <el-menu-item index="/pets">宠物列表</el-menu-item>
        <el-menu-item index="/publish">发布宠物</el-menu-item>
        <el-menu-item index="/my-applications">我的领养</el-menu-item>
        <el-menu-item index="/received-applications">收到的申请</el-menu-item>
        <el-menu-item index="/notifications">通知</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
      </el-menu>
      <div class="actions">
        <el-button v-if="isLogin" link type="primary" @click="handleLogout">退出登录</el-button>
        <template v-else>
          <el-button link type="primary" @click="router.push('/login')">登录</el-button>
          <el-button link @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.logo {
  font-weight: bold;
  font-size: 18px;
  cursor: pointer;
  white-space: nowrap;
}
.menu {
  flex: 1;
  border-bottom: none;
}
.actions {
  white-space: nowrap;
}
</style>
