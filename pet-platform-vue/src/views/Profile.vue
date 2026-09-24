<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo, updatePassword } from '../api/user'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const info = ref({ nickname: '', avatar: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '' })

async function load() {
  if (!userStore.userId) return
  const data = await getUserInfo(userStore.userId)
  info.value = { nickname: data.nickname || '', avatar: data.avatar || '' }
}

async function saveInfo() {
  await updateUserInfo(info.value)
  ElMessage.success('更新成功')
}

async function savePassword() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请输入旧密码和新密码')
    return
  }
  await updatePassword(pwdForm.value)
  ElMessage.success('密码更新成功')
  pwdForm.value = { oldPassword: '', newPassword: '' }
}

onMounted(load)
</script>

<template>
  <el-card style="max-width: 520px">
    <template #header>个人中心</template>
    <el-form label-width="80px" @submit.prevent>
      <el-form-item label="用户ID">
        <el-input :model-value="userStore.userId" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="info.nickname" />
      </el-form-item>
      <el-form-item label="头像URL">
        <el-input v-model="info.avatar" placeholder="图片地址" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveInfo">保存资料</el-button>
      </el-form-item>
      <el-divider>修改密码</el-divider>
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="warning" @click="savePassword">修改密码</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
