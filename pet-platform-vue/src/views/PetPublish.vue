<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addPet } from '../api/pet'

const router = useRouter()
const form = ref({ petName: '', breed: '', age: null, gender: 0, description: '', imageUrl: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.petName) {
    ElMessage.warning('请填写宠物昵称')
    return
  }
  loading.value = true
  try {
    await addPet(form.value)
    ElMessage.success('发布成功')
    router.push('/pets')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card style="max-width: 600px">
    <template #header>发布宠物</template>
    <el-form label-width="80px" @submit.prevent>
      <el-form-item label="昵称">
        <el-input v-model="form.petName" />
      </el-form-item>
      <el-form-item label="品种">
        <el-input v-model="form.breed" />
      </el-form-item>
      <el-form-item label="年龄">
        <el-input-number v-model="form.age" :min="0" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="0">公</el-radio>
          <el-radio :value="1">母</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="图片URL">
        <el-input v-model="form.imageUrl" placeholder="图片地址" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">发布</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
