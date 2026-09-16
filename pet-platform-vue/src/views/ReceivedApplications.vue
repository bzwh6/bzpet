<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReceivedAdoptions, processApplication } from '../api/adoption'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNo: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await getReceivedAdoptions(query.value)
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

async function process(row, accept) {
  let rejectReason = ''
  if (!accept) {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
      inputPlaceholder: '拒绝原因'
    })
    rejectReason = value
  }
  await processApplication({
    applicationId: row.id,
    status: accept ? 1 : 2,
    rejectReason
  })
  ElMessage.success('处理成功')
  load()
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header>我收到的领养申请</template>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="id" label="申请ID" width="90" />
      <el-table-column prop="petId" label="宠物ID" width="90" />
      <el-table-column label="申请人" width="150">
        <template #default="{ row }">
          {{ row.applicantNickname || row.applicantId }}
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="领养理由" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" size="small" type="success" @click="process(row, true)">
            同意
          </el-button>
          <el-button v-if="row.status === 0" size="small" type="danger" @click="process(row, false)">
            拒绝
          </el-button>
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
