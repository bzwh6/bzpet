<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/user'
import { useUserStore } from '../store/user'
import { parseUserId } from '../utils/jwt'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const form = ref({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const token = await login(form.value)
    userStore.setLogin(token, parseUserId(token))
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/pets')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="box">
    <template #header>登录</template>
    <el-form label-width="70px" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
        <el-button @click="router.push('/register')">去注册</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.box {
  max-width: 420px;
  margin: 60px auto;
}
</style>
