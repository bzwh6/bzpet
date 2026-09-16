<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPetList } from '../api/pet'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNo: 1, pageSize: 8, adoptionStatus: null, gender: null })

async function load() {
  loading.value = true
  try {
    const data = await getPetList(query.value)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.value.pageNo = 1
  load()
}

function reset() {
  query.value = { pageNo: 1, pageSize: 8, adoptionStatus: null, gender: null }
  load()
}

function genderText(g) {
  return g === 0 ? '公' : g === 1 ? '母' : '-'
}

onMounted(load)
</script>

<template>
  <div>
    <el-card>
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="query.adoptionStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="待领养" :value="0" />
            <el-option label="已领养" :value="1" />
            <el-option label="下架" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="query.gender" placeholder="全部" clearable style="width: 130px">
            <el-option label="公" :value="0" />
            <el-option label="母" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="grid">
      <el-card
        v-for="pet in list"
        :key="pet.id"
        shadow="hover"
        class="pet-card"
        @click="router.push(`/pets/${pet.id}`)"
      >
        <el-image :src="pet.imageUrl" fit="cover" class="cover">
          <template #error>
            <div class="cover placeholder">暂无图片</div>
          </template>
        </el-image>
        <div class="name">{{ pet.petName }}</div>
        <div class="meta">{{ pet.breed || '未知品种' }} · {{ pet.age ?? '-' }}岁 · {{ genderText(pet.gender) }}</div>
        <div class="meta">❤️ {{ pet.likesCount ?? 0 }}</div>
      </el-card>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="暂无宠物" />

    <el-pagination
      class="pager"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @current-change="(p) => { query.pageNo = p; load() }"
    />
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.pet-card {
  cursor: pointer;
}
.cover {
  width: 100%;
  height: 150px;
}
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  height: 150px;
}
.name {
  font-weight: bold;
  margin-top: 8px;
}
.meta {
  color: #888;
  font-size: 13px;
  margin-top: 4px;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
