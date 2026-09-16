<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const router = useRouter()
const form = ref({ username: '', password: '', nickname: '', phone: '' })
const loading = ref(false)

async function handleRegister() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="box">
    <template #header>注册</template>
    <el-form label-width="70px" @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
        <el-button @click="router.push('/login')">返回登录</el-button>
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
