<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import StatusTag from '../components/StatusTag.vue'
import { stageLabel } from '../utils/labels'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/batches', { params: query })
    rows.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <div>
        <h1 class="page-title">溯源批次</h1>
        <p class="page-subtitle">只显示当前企业参与或监管部门授权范围内的批次。</p>
      </div>
      <el-button v-if="auth.canCreateBatch" type="primary" :icon="Plus" @click="router.push('/batches/new')">
        创建批次
      </el-button>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="批次号、追溯码或产品名称"
          @keyup.enter="query.page = 1; load()"
        />
        <el-select v-model="query.status" clearable placeholder="全部状态" @change="query.page = 1; load()">
          <el-option value="ACTIVE" label="正常" />
          <el-option value="DISPATCHED" label="已发货" />
          <el-option value="IN_TRANSIT" label="运输中" />
          <el-option value="ON_SALE" label="销售中" />
          <el-option value="SOLD" label="已售出" />
          <el-option value="FROZEN" label="已冻结" />
          <el-option value="RECALLED" label="已召回" />
        </el-select>
        <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" class="batch-table" @row-click="(row) => router.push(`/batches/${row.id}`)">
        <el-table-column prop="batchNo" label="批次号" min-width="165">
          <template #default="{ row }"><span class="mono">{{ row.batchNo }}</span></template>
        </el-table-column>
        <el-table-column prop="productName" label="产品" min-width="135">
          <template #default="{ row }">{{ row.productName || '原料鸭' }}</template>
        </el-table-column>
        <el-table-column prop="ownerOrgName" label="所属企业" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="100">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="currentStage" label="当前阶段" width="120">
          <template #default="{ row }">{{ stageLabel(row.currentStage) }}</template>
        </el-table-column>
        <el-table-column label="业务状态" width="115">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="链上状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.chainStatus" /></template>
        </el-table-column>
        <el-table-column prop="traceCode" label="追溯码" min-width="180">
          <template #default="{ row }"><span class="mono">{{ row.traceCode || '包装后生成' }}</span></template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.filter-bar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 150px auto auto;
  gap: 10px;
  margin-bottom: 20px;
}
.batch-table :deep(.el-table__row) {
  cursor: pointer;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
@media (max-width: 760px) {
  .filter-bar { grid-template-columns: 1fr 1fr; }
  .header-row { flex-direction: column; }
}
</style>
