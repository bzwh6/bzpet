<script setup>
import { ref, onMounted } from 'vue'
import { getMyAdoptions } from '../api/adoption'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNo: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await getMyAdoptions(query.value)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function statusText(s) {
  return s === 0 ? '待审核' : s === 1 ? '已通过' : s === 2 ? '已拒绝' : '-'
}

function statusType(s) {
  return s === 1 ? 'success' : s === 2 ? 'danger' : 'info'
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header>我的领养申请</template>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="id" label="申请ID" width="100" />
      <el-table-column prop="petId" label="宠物ID" width="100" />
      <el-table-column prop="reason" label="申请理由" />
      <el-table-column label="发布人" width="140">
        <template #default="{ row }">
          {{ row.publisherNickname || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @current-change="(p) => { query.pageNo = p; load() }"
    />
  </el-card>
</template>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
