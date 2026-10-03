<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo, updatePassword, uploadAvatar } from '../api/user'
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

// 上传头像到 OSS，拿到 URL 后回填（再点保存资料入库）
async function onUpload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const url = await uploadAvatar(fd)
  info.value.avatar = url
  ElMessage.success('头像上传成功，请点击“保存资料”')
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
      <el-form-item label="头像">
        <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
          <el-avatar :size="72" :src="info.avatar">头像</el-avatar>
          <el-button size="small" style="margin-left: 12px">更换头像</el-button>
        </el-upload>
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
