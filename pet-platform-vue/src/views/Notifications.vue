<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getNotificationList,
  getNotificationDetail,
  markAllRead,
  clearRead,
  deleteNotification
} from '../api/notification'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNo: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await getNotificationList(query.value)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function open(row) {
  const detail = await getNotificationDetail(row.id)
  ElMessage.info(detail.content || detail.title || '通知')
  load()
}

async function readAll() {
  await markAllRead()
  ElMessage.success('已全部标记已读')
  load()
}

async function clear() {
  await clearRead()
  ElMessage.success('已清空已读通知')
  load()
}

async function remove(row) {
  await deleteNotification(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>我的通知</span>
        <div>
          <el-button size="small" @click="readAll">全部已读</el-button>
          <el-button size="small" @click="clear">清空已读</el-button>
        </div>
      </div>
    </template>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 1 ? 'info' : 'danger'">
            {{ row.isRead === 1 ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button size="small" @click="open(row)">查看</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
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
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
