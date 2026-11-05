<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import StatusTag from '../components/StatusTag.vue'

const router = useRouter()
const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/submissions/mine')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="page-title">我的业务提交</h1>
    <p class="page-subtitle">查看待审核、已通过和已驳回的业务记录。被驳回后请根据审核意见重新填写。</p>
    <el-card>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="batchNo" label="批次号" min-width="160">
          <template #default="{ row }"><span class="mono">{{ row.batchNo }}</span></template>
        </el-table-column>
        <el-table-column prop="eventType" label="业务类型" width="140" />
        <el-table-column prop="stage" label="阶段" width="120" />
        <el-table-column prop="createdAt" label="提交时间" min-width="170" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }"><StatusTag :status="row.reviewStatus || row.chainStatus" /></template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="190" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/batches/${row.batchId}`)">查看批次</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="还没有提交记录" />
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin-bottom: 8px;
}
</style>
