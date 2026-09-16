<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPetDetail, likePet, deletePet, isLiked } from '../api/pet'
import { applyAdoption } from '../api/adoption'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const pet = ref(null)
const liked = ref(false)
const loading = ref(false)
const reason = ref('')

async function load() {
  if (!route.params.id || route.params.id === 'undefined') {
    ElMessage.error('宠物ID无效')
    router.push('/pets')
    return
  }
  loading.value = true
  try {
    pet.value = await getPetDetail(route.params.id)
    liked.value = await isLiked(route.params.id)
  } finally {
    loading.value = false
  }
}

async function handleLike() {
  await likePet(route.params.id)
  ElMessage.success(liked.value ? '已取消点赞' : '点赞成功')
  load()
}

async function handleApply() {
  if (!reason.value) {
    ElMessage.warning('请填写领养理由')
    return
  }
  await applyAdoption({ petId: Number(route.params.id), reason: reason.value })
  ElMessage.success('申请已提交')
  reason.value = ''
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除这只宠物吗？', '提示', { type: 'warning' })
  await deletePet(route.params.id)
  ElMessage.success('删除成功')
  router.push('/pets')
}

function genderText(g) {
  return g === 0 ? '公' : g === 1 ? '母' : '-'
}

function statusText(s) {
  return s === 0 ? '待领养' : s === 1 ? '已领养' : s === 2 ? '下架' : '-'
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <el-page-header content="宠物详情" @back="router.back()" />
    <div v-if="pet" class="detail">
      <el-image :src="pet.imageUrl" fit="cover" class="cover">
        <template #error>
          <div class="cover placeholder">暂无图片</div>
        </template>
      </el-image>
      <div class="info">
        <h2>{{ pet.petName }}</h2>
        <p>品种：{{ pet.breed || '-' }}</p>
        <p>年龄：{{ pet.age ?? '-' }} 岁</p>
        <p>性别：{{ genderText(pet.gender) }}</p>
        <p>状态：{{ statusText(pet.status) }}</p>
        <p>点赞数：❤️ {{ pet.likesCount ?? 0 }}</p>
        <p>描述：{{ pet.description || '-' }}</p>
        <div class="ops">
          <el-button type="primary" @click="handleLike">
            {{ liked ? '取消点赞' : '点赞' }}
          </el-button>
          <el-button
            v-if="pet.publisherId === Number(userStore.userId)"
            type="danger"
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <el-divider>申请领养</el-divider>
    <el-input v-model="reason" type="textarea" placeholder="填写领养理由" :rows="3" style="max-width: 600px" />
    <div style="margin-top: 12px">
      <el-button type="success" @click="handleApply">提交领养申请</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.detail {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}
.cover {
  width: 300px;
  height: 300px;
  flex-shrink: 0;
}
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
}
.info {
  flex: 1;
}
.ops {
  margin-top: 16px;
}
</style>
