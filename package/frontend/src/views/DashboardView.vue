<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Box, CircleCheck, Connection, Warning } from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import StatusTag from '../components/StatusTag.vue'
import { stageLabel } from '../utils/labels'
import { primaryJobRole } from '../utils/roles'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const data = ref({})
const guide = computed(() => ({
  SUPPLIER: ['创建原料鸭批次', '登记检疫结果', '选择加工企业并发货'],
  PROCESSOR: ['确认原料收货', '建立加工子批次', '完成屠宰、酱卤、杀菌和包装'],
  LOGISTICS: ['接收运输任务', '登记冷链温度', '送达后等待销售商确认'],
  RETAILER: ['核对成品批次并收货', '维护销售记录', '执行召回处置'],
  REGULATOR: ['查看辖区批次', '登记检验结论', '对风险批次冻结或召回'],
  ADMIN: ['维护企业和账号', '检查链码连接', '查看审计与系统状态'],
}[auth.user?.orgType] || []))
const jobLabel = computed(() => primaryJobRole(auth.roles))

async function load() {
  loading.value = true
  try {
    data.value = await http.get('/dashboard')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <section class="welcome">
      <div>
        <div class="eyebrow">CHANGDE DUCK TRACE</div>
        <h1>{{ auth.user?.orgName }}</h1>
        <p>当前身份：{{ jobLabel }} · 数据范围仅限所属企业内部。</p>
      </div>
      <div class="welcome-mark">沅</div>
    </section>

    <section class="metrics">
      <article>
        <el-icon><Box /></el-icon>
        <div><span>可见批次</span><strong>{{ data.batchTotal || 0 }}</strong></div>
      </article>
      <article>
        <el-icon><Connection /></el-icon>
        <div><span>待处理上链</span><strong>{{ data.pendingChain || 0 }}</strong></div>
      </article>
      <article>
        <el-icon><CircleCheck /></el-icon>
        <div><span>冷链在途</span><strong>{{ data.inTransit || 0 }}</strong></div>
      </article>
      <article>
        <el-icon><Warning /></el-icon>
        <div><span>风险批次</span><strong>{{ data.riskBatches || 0 }}</strong></div>
      </article>
    </section>

    <section class="dashboard-grid">
      <el-card class="recent-card">
        <template #header>
          <div class="card-head">
            <div><strong>最近批次</strong><span>最近进入业务链路的溯源批次</span></div>
            <el-button link @click="router.push('/batches')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="data.recentBatches || []" style="width: 100%">
          <el-table-column prop="batchNo" label="批次号" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.batchNo }}</span></template>
          </el-table-column>
          <el-table-column prop="productName" label="产品" min-width="120" />
          <el-table-column prop="currentStage" label="阶段" width="120">
            <template #default="{ row }">{{ stageLabel(row.currentStage) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="" width="74">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/batches/${row.id}`)">进入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <div class="side-column">
        <el-card>
          <template #header><strong>当前岗位操作顺序</strong></template>
          <ol class="guide-list">
            <li v-for="item in guide" :key="item">{{ item }}</li>
          </ol>
        </el-card>
        <el-card>
          <template #header><strong>阶段分布</strong></template>
          <div v-if="(data.stageCounts || []).length" class="stage-bars">
            <div v-for="item in data.stageCounts.slice(0, 7)" :key="item.stage" class="stage-row">
              <span>{{ stageLabel(item.stage) }}</span>
              <div><i :style="{ width: `${Math.max(8, Math.min(100, Number(item.total) * 18))}%` }" /></div>
              <b>{{ item.total }}</b>
            </div>
          </div>
          <el-empty v-else description="暂无阶段数据" :image-size="72" />
        </el-card>
      </div>
    </section>
  </div>
</template>

<style scoped>
.welcome {
  position: relative;
  overflow: hidden;
  display: flex;
  min-height: 180px;
  align-items: center;
  padding: 32px 36px;
  border-radius: 20px;
  color: #fff8ed;
  background:
    radial-gradient(circle at 86% 24%, rgba(218, 161, 78, 0.34), transparent 17rem),
    linear-gradient(120deg, #3c2119, #6a2e24);
}
.eyebrow {
  color: #d9ab63;
  font-size: 10px;
  letter-spacing: 0.26em;
}
.welcome h1 {
  margin: 13px 0 10px;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 29px;
}
.welcome p {
  margin: 0;
  color: rgba(255, 248, 237, 0.66);
  font-size: 13px;
}
.welcome-mark {
  position: absolute;
  right: 38px;
  color: rgba(255, 241, 218, 0.11);
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 130px;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin: 20px 0;
}
.metrics article {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 20px;
  border: 1px solid #e8daca;
  border-radius: 15px;
  background: rgba(255, 250, 242, 0.9);
}
.metrics .el-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  color: #6a2e24;
  background: #f2e4d2;
  font-size: 18px;
}
.metrics span,
.metrics strong {
  display: block;
}
.metrics span {
  color: #8b7767;
  font-size: 11px;
}
.metrics strong {
  margin-top: 6px;
  font-family: Georgia, serif;
  font-size: 26px;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(300px, 0.75fr);
  gap: 18px;
}
.side-column {
  display: grid;
  gap: 18px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-head strong,
.card-head span {
  display: block;
}
.card-head span {
  margin-top: 4px;
  color: #9a8777;
  font-size: 11px;
}
.guide-list {
  margin: 0;
  padding-left: 20px;
  color: #6b5546;
  line-height: 2.15;
  font-size: 13px;
}
.stage-bars {
  display: grid;
  gap: 11px;
}
.stage-row {
  display: grid;
  grid-template-columns: 78px 1fr 24px;
  gap: 9px;
  align-items: center;
  color: #755e4f;
  font-size: 11px;
}
.stage-row div {
  height: 7px;
  border-radius: 999px;
  background: #efe3d4;
}
.stage-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #a9583e, #d79b51);
}
.stage-row b {
  text-align: right;
  font-family: Georgia, serif;
}
@media (max-width: 1050px) {
  .metrics { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .welcome { padding: 25px; }
  .welcome-mark { display: none; }
  .metrics { grid-template-columns: 1fr 1fr; gap: 10px; }
  .metrics article { padding: 15px; }
}
</style>
