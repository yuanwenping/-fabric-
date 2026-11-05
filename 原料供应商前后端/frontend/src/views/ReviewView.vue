<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CloseBold, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { roleLabels } from '../utils/roles'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const detailVisible = ref(false)
const selected = ref(null)
const reviewComment = ref('')

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/reviews/pending')
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  selected.value = row
  reviewComment.value = ''
  detailVisible.value = true
}

async function approve() {
  await ElMessageBox.confirm(
    '审核通过后，该业务事件将正式写入 Fabric，并推进批次状态。确认通过吗？',
    '确认审核',
    { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' },
  )
  await http.post(`/reviews/${selected.value.eventId}/approve`, {
    comment: reviewComment.value,
  })
  ElMessage.success('审核通过并完成上链')
  detailVisible.value = false
  await load()
}

async function reject() {
  if (!reviewComment.value.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  await http.post(`/reviews/${selected.value.eventId}/reject`, {
    comment: reviewComment.value,
  })
  ElMessage.success('已驳回，等待员工修改后重新提交')
  detailVisible.value = false
  await load()
}

function formattedPayload(payload) {
  try {
    return JSON.stringify(JSON.parse(payload || '{}'), null, 2)
  } catch {
    return payload
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <div>
        <h1 class="page-title">业务审核中心</h1>
        <p class="page-subtitle">员工提交的数据需由本企业管理员审核，通过后才会写入区块链并流转到下一环节。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="batchNo" label="批次号" min-width="155">
          <template #default="{ row }"><span class="mono">{{ row.batchNo }}</span></template>
        </el-table-column>
        <el-table-column prop="productName" label="产品" min-width="130" />
        <el-table-column prop="eventType" label="业务类型" width="130" />
        <el-table-column prop="submittedByName" label="提交员工" width="120" />
        <el-table-column prop="submittedByPhone" label="员工手机号" width="140" />
        <el-table-column label="岗位" width="145">
          <template #default="{ row }">{{ roleLabels[row.submittedRole] || row.submittedRole }}</template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="170" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="showDetail(row)">审核</el-button>
            <el-button link @click="router.push(`/batches/${row.batchId}`)">批次</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="当前没有待审核业务" />
    </el-card>

    <el-dialog v-model="detailVisible" title="审核员工提交" width="760px">
      <template v-if="selected">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="批次号">{{ selected.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ selected.eventType }}</el-descriptions-item>
          <el-descriptions-item label="提交员工">
            {{ selected.submittedByName }}（{{ selected.submittedByPhone }}）
          </el-descriptions-item>
          <el-descriptions-item label="岗位">
            {{ roleLabels[selected.submittedRole] || selected.submittedRole }}
          </el-descriptions-item>
          <el-descriptions-item label="业务时间">{{ selected.businessTime }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ selected.locationCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内容哈希" :span="2">
            <span class="mono">{{ selected.contentHash }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4>员工填报内容</h4>
        <pre class="payload">{{ formattedPayload(selected.payloadJson) }}</pre>

        <el-form-item label="审核意见">
          <el-input
            v-model="reviewComment"
            type="textarea"
            :rows="3"
            placeholder="通过时可选，驳回时必须填写原因"
          />
        </el-form-item>
      </template>
      <template #footer>
        <el-button :icon="CloseBold" @click="reject">驳回并退回修改</el-button>
        <el-button type="primary" :icon="CircleCheck" @click="approve">审核通过并上链</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
h4 {
  margin: 22px 0 10px;
}
.payload {
  max-height: 280px;
  overflow: auto;
  padding: 16px;
  border-radius: 10px;
  color: #5d473a;
  background: #f7eee2;
  font-size: 11px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
