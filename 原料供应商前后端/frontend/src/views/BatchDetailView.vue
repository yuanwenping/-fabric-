<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Download, Plus, RefreshRight, Tickets } from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import StatusTag from '../components/StatusTag.vue'
import EventDialog from '../components/EventDialog.vue'
import { eventOptionsFor, eventSchemas } from '../components/eventSchemas'
import { fieldLabel, stageLabel } from '../utils/labels'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const detail = ref(null)
const eventVisible = ref(false)
const verifying = ref(false)
const verification = ref(null)

const batch = computed(() => detail.value?.batch)
const hasEventRole = computed(() => {
  return auth.orgType === 'SUPPLIER'
    && auth.roles.some((role) =>
      ['SUPPLIER_OPERATOR', 'SUPPLIER_BREEDER', 'SUPPLIER_QA', 'SUPPLIER_SHIPPER'].includes(role))
})
const canReview = computed(() =>
  auth.roles.includes('ORG_MANAGER') && auth.orgType === 'SUPPLIER')
const canOperate = computed(() =>
  batch.value
  && hasEventRole.value
  && batch.value.custodianOrgId === auth.user?.orgId,
)
const availableActions = computed(() => eventOptionsFor(auth.orgType, auth.roles))
async function load() {
  loading.value = true
  try {
    detail.value = await http.get(`/batches/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function verify() {
  verifying.value = true
  try {
    verification.value = await http.get(`/batches/${route.params.id}/verify`)
    ElMessage.success(verification.value.allVerified ? '全部事件与链上哈希一致' : '核验完成，请查看异常项')
  } finally {
    verifying.value = false
  }
}

async function retry(event) {
  await http.post(`/events/${event.id}/retry`)
  ElMessage.success('事件已重新提交并完成上链')
  await load()
}

async function download(evidence) {
  const response = await http.get(`/evidence/${evidence.id}`, { responseType: 'blob' })
  const url = URL.createObjectURL(response)
  const link = document.createElement('a')
  link.href = url
  link.download = evidence.originalName
  link.click()
  URL.revokeObjectURL(url)
}

function eventTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function summaryEntries(processData) {
  if (!processData || typeof processData !== 'object') return []
  return Object.entries(processData)
    .filter(([, value]) => value !== null && value !== '' && typeof value !== 'object')
    .slice(0, 8)
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <template v-if="batch">
      <div class="detail-head">
        <div>
          <div class="back" @click="router.push('/batches')">← 返回批次列表</div>
          <h1 class="page-title">{{ batch.productName || '原料鸭批次' }}</h1>
          <div class="batch-meta">
            <span class="mono">{{ batch.batchNo }}</span>
            <StatusTag :status="batch.status" />
            <StatusTag :status="batch.chainStatus" />
          </div>
        </div>
        <div class="head-actions">
          <el-button :icon="CircleCheck" :loading="verifying" @click="verify">链上核验</el-button>
          <el-button
            v-if="canOperate"
            type="primary"
            :icon="Plus"
            @click="eventVisible = true"
          >
            登记业务事件
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="verification"
        :type="verification.allVerified ? 'success' : 'warning'"
        :closable="false"
        show-icon
        :title="`核验结果：${verification.verifiedCount}/${verification.eventCount} 个事件与链上记录一致`"
        class="verify-alert"
      />

      <section class="overview-grid">
        <el-card>
          <template #header><strong>批次基础信息</strong></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="当前阶段">{{ stageLabel(batch.currentStage) }}</el-descriptions-item>
            <el-descriptions-item label="所属企业">{{ batch.ownerOrgName }}</el-descriptions-item>
            <el-descriptions-item label="当前保管方">{{ batch.custodianOrgName }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ batch.quantity }} {{ batch.unit }}</el-descriptions-item>
            <el-descriptions-item label="生产日期">{{ batch.productionDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="保质期至">{{ batch.expiryDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="追溯码" :span="2">
              <span>原料批次不生成终端追溯码</span>
            </el-descriptions-item>
            <el-descriptions-item label="最新交易" :span="2">
              <span class="mono">{{ batch.txId || '尚未产生交易' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card>
          <template #header><strong>链路关系</strong></template>
          <div v-if="detail.relations.length" class="relation-list">
            <div v-for="relation in detail.relations" :key="`${relation.parentBatchId}-${relation.childBatchId}`">
              <span class="mono">{{ relation.parentBatchNo }}</span>
              <b>{{ relation.relationType }}</b>
              <span class="mono">{{ relation.childBatchNo }}</span>
            </div>
          </div>
          <el-empty v-else description="暂未关联上下游批次" :image-size="70" />
        </el-card>
      </section>

      <section class="content-grid">
        <el-card class="timeline-card">
          <template #header>
            <div class="card-title">
              <strong>全链路事件</strong>
              <span>{{ detail.events.length }} 个已登记节点</span>
            </div>
          </template>
          <el-timeline v-if="detail.events.length">
            <el-timeline-item
              v-for="event in detail.events"
              :key="event.id"
              :timestamp="eventTime(event.businessTime)"
              placement="top"
              :color="event.chainStatus === 'COMMITTED' ? '#456756' : '#b54235'"
            >
              <article class="event-card">
                <div class="event-head">
                  <div>
                    <h3>{{ eventSchemas[event.eventType]?.label || event.eventType }}</h3>
                    <p>{{ event.issuerOrgName }} · {{ event.operatorName }}</p>
                  </div>
                  <StatusTag :status="event.chainStatus" />
                </div>
                <div class="event-facts">
                  <span>阶段：{{ stageLabel(event.stage) }}</span>
                  <span>地点：{{ event.locationCode || '-' }}</span>
                  <span>结果：{{ event.resultStatus }}</span>
                </div>
                <div class="process-grid">
                  <div v-for="[key, value] in summaryEntries(event.processData)" :key="key">
                    <span>{{ fieldLabel(key) }}</span>
                    <strong>{{ value }}</strong>
                  </div>
                </div>
                <div class="hash-row">
                  <span>内容哈希</span>
                  <code>{{ event.contentHash }}</code>
                </div>
                <div v-if="event.chainStatus === 'PENDING_REVIEW'" class="review-note">
                  <span>该记录正在等待企业管理员审核，尚未写入区块链。</span>
                  <el-button v-if="canReview" link type="primary" @click="router.push('/reviews')">
                    前往审核
                  </el-button>
                </div>
                <div v-if="event.reviewComment" class="review-note rejected">
                  <span>审核意见：{{ event.reviewComment }}</span>
                </div>
                <div v-if="event.evidence?.length" class="evidence-row">
                  <el-button
                    v-for="file in event.evidence"
                    :key="file.id"
                    size="small"
                    link
                    :icon="Download"
                    @click="download(file)"
                  >
                    {{ file.originalName }}
                  </el-button>
                </div>
                <div v-if="event.chainStatus === 'CHAIN_FAILED' && canReview" class="failed-row">
                  <span>{{ event.chainError }}</span>
                  <el-button size="small" :icon="RefreshRight" @click="retry(event)">重试上链</el-button>
                </div>
              </article>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="尚未登记业务事件" />
        </el-card>

        <div class="side-stack">
          <el-card>
            <template #header><strong>交接记录</strong></template>
            <div v-if="detail.transfers.length" class="transfer-list">
              <div v-for="transfer in detail.transfers" :key="transfer.id">
                <div>{{ transfer.fromOrgName }} → {{ transfer.toOrgName }}</div>
                <small v-if="transfer.carrierOrgName">承运：{{ transfer.carrierOrgName }}</small>
                <StatusTag :status="transfer.status === 'PENDING' ? 'PENDING_RECEIPT' : transfer.status" />
              </div>
            </div>
            <el-empty v-else description="暂无交接" :image-size="64" />
          </el-card>

          <el-card>
            <template #header><strong>检验结论</strong></template>
            <div v-if="detail.inspections.length" class="inspection-list">
              <div v-for="item in detail.inspections" :key="item.id">
                <div><b>{{ item.overallResult }}</b><StatusTag :status="item.overallResult" /></div>
                <span>{{ item.inspectionType }} · {{ item.reportNo || item.sampleNo }}</span>
                <small>{{ eventTime(item.issuedAt) }}</small>
              </div>
            </div>
            <el-empty v-else description="暂无监管检验记录" :image-size="64" />
          </el-card>

          <el-card class="action-help">
            <template #header><strong>当前可执行操作</strong></template>
            <p>{{ canOperate ? '请按业务实际发生顺序提交事件。' : '当前批次不在本企业保管范围内，仅可查看。' }}</p>
            <div class="action-tags">
              <el-tag v-for="action in availableActions" :key="action" effect="plain">
                {{ eventSchemas[action]?.label || action }}
              </el-tag>
            </div>
          </el-card>
        </div>
      </section>

      <EventDialog
        v-if="canOperate"
        v-model="eventVisible"
        :batch="batch"
        :org-type="auth.orgType"
        :roles="auth.roles"
        @saved="load"
      />
    </template>
  </div>
</template>

<style scoped>
.back {
  margin-bottom: 12px;
  color: #947b68;
  cursor: pointer;
  font-size: 12px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
  margin-bottom: 22px;
}
.batch-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 10px;
  color: #846d5d;
}
.head-actions {
  display: flex;
  gap: 8px;
}
.verify-alert {
  margin-bottom: 18px;
}
.overview-grid,
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 18px;
}
.overview-grid {
  margin-bottom: 18px;
}
.relation-list {
  display: grid;
  gap: 10px;
}
.relation-list > div {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #efe3d5;
}
.relation-list b {
  color: #b3783f;
  font-size: 10px;
}
.child-button {
  width: 100%;
  margin-top: 14px;
}
.card-title strong,
.card-title span {
  display: block;
}
.card-title span {
  margin-top: 4px;
  color: #998575;
  font-size: 11px;
}
.event-card {
  padding: 18px;
  border: 1px solid #e9dccd;
  border-radius: 14px;
  background: #fffaf2;
}
.event-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.event-head h3 {
  margin: 0;
  font-size: 16px;
}
.event-head p {
  margin: 6px 0 0;
  color: #8b7767;
  font-size: 11px;
}
.event-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin: 14px 0;
  color: #755e4e;
  font-size: 12px;
}
.process-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.process-grid > div {
  padding: 9px 11px;
  border-radius: 9px;
  background: #f7efe4;
}
.process-grid span,
.process-grid strong {
  display: block;
}
.process-grid span {
  color: #9a8474;
  font-size: 9px;
}
.process-grid strong {
  overflow: hidden;
  margin-top: 4px;
  color: #49352c;
  font-size: 11px;
  text-overflow: ellipsis;
}
.hash-row {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 8px;
  margin-top: 13px;
  color: #9a8474;
  font-size: 10px;
}
.hash-row code {
  overflow: hidden;
  color: #715848;
  font-size: 10px;
  text-overflow: ellipsis;
}
.evidence-row {
  margin-top: 10px;
}
.failed-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding: 10px;
  border-radius: 8px;
  color: #8e3028;
  background: #f8e7e3;
  font-size: 11px;
}
.review-note {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #765e43;
  background: #f7ead4;
  font-size: 11px;
}
.review-note.rejected {
  color: #8e3028;
  background: #f8e7e3;
}
.side-stack {
  display: grid;
  align-content: start;
  gap: 18px;
}
.transfer-list,
.inspection-list {
  display: grid;
  gap: 12px;
}
.transfer-list > div,
.inspection-list > div {
  padding-bottom: 11px;
  border-bottom: 1px solid #efe3d5;
  font-size: 12px;
}
.transfer-list > div:last-child,
.inspection-list > div:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}
.transfer-list small,
.inspection-list span,
.inspection-list small {
  display: block;
}
.transfer-list small,
.inspection-list span,
.inspection-list small {
  margin: 5px 0;
  color: #947f6e;
  font-size: 10px;
}
.inspection-list > div > div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}
.trace-link {
  color: #6a2e24;
  font-family: Consolas, monospace;
  font-size: 12px;
}
.action-help p {
  color: #826c5c;
  font-size: 12px;
  line-height: 1.7;
}
.action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
@media (max-width: 1000px) {
  .overview-grid,
  .content-grid { grid-template-columns: 1fr; }
}
@media (max-width: 680px) {
  .detail-head { align-items: flex-start; flex-direction: column; }
  .process-grid { grid-template-columns: 1fr 1fr; }
}
</style>
