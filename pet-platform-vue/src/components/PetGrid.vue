<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePet } from '../api/pet'
import { useBreeds, breedName } from '../utils/breed'

// 通用宠物卡片列表：我发布的 / 我点赞的 共用
const props = defineProps({
  // 分页查询函数，返回 { list, total, pages }
  fetchApi: { type: Function, required: true },
  // 是否显示状态/品种筛选（点赞列表不支持筛选）
  showFilter: { type: Boolean, default: false },
  // 是否显示删除（只有自己发布的才可删）
  showDelete: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无宠物' },
  pageSize: { type: Number, default: 8 }
})

const router = useRouter()
const { options: breedOptions } = useBreeds()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNo: 1, pageSize: props.pageSize, adoptionStatus: null, breed: null })

async function load() {
  loading.value = true
  try {
    const data = await props.fetchApi(query.value)
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
  query.value = { pageNo: 1, pageSize: props.pageSize, adoptionStatus: null, breed: null }
  load()
}

async function handleDelete(pet) {
  try {
    await ElMessageBox.confirm(`确定删除「${pet.petName}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deletePet(pet.id)
  ElMessage.success('删除成功')
  // 删掉当前页最后一条时回退一页，避免停在空页
  if (list.value.length === 1 && query.value.pageNo > 1) {
    query.value.pageNo -= 1
  }
  load()
}

function genderText(g) {
  return g === 0 ? '公' : g === 1 ? '母' : '-'
}

function statusText(s) {
  return s === 0 ? '待领养' : s === 1 ? '已领养' : s === 2 ? '下架' : '-'
}

function statusType(s) {
  return s === 0 ? 'success' : s === 1 ? 'info' : 'warning'
}

defineExpose({ reload: load })
onMounted(load)
</script>

<template>
  <div>
    <div v-if="showFilter || $slots.action" class="bar">
      <el-form v-if="showFilter" :inline="true" class="filters">
        <el-form-item label="状态">
          <el-select v-model="query.adoptionStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="待领养" :value="0" />
            <el-option label="已领养" :value="1" />
            <el-option label="下架" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种">
          <el-select v-model="query.breed" placeholder="全部" clearable filterable style="width: 150px">
            <el-option v-for="b in breedOptions" :key="b.code" :label="b.name" :value="b.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <div v-if="$slots.action" class="action">
        <slot name="action" />
      </div>
    </div>

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
        <div class="meta">
          {{ breedName(pet.breed) || '未知品种' }} · {{ pet.age ?? '-' }}岁 · {{ genderText(pet.gender) }}
        </div>
        <div class="meta">
          <el-tag size="small" :type="statusType(pet.status)">{{ statusText(pet.status) }}</el-tag>
          <span class="likes">❤️ {{ pet.likesCount ?? 0 }}</span>
        </div>
        <div class="ops">
          <el-button link type="primary" @click.stop="router.push(`/pets/${pet.id}`)">查看</el-button>
          <!-- 后端只允许删除待领养的宠物 -->
          <el-button
            v-if="showDelete && pet.status === 0"
            link
            type="danger"
            @click.stop="handleDelete(pet)"
          >
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && list.length === 0" :description="emptyText" />

    <el-pagination
      v-if="total > 0"
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
.bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.filters {
  flex: 1;
}
.action {
  margin-left: auto;
  white-space: nowrap;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 4px;
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
.likes {
  margin-left: 8px;
}
.ops {
  margin-top: 8px;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
