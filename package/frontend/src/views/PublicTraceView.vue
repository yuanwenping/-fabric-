<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'
import StatusTag from '../components/StatusTag.vue'
import { fieldLabel, stageLabel } from '../utils/labels'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const traceCode = ref(route.params.traceCode || '')
const loading = ref(false)
const data = ref(null)
const message = ref('')

async function search() {
  const code = traceCode.value.trim()
  if (!code) return
  loading.value = true
  message.value = ''
  data.value = null
  try {
    data.value = await http.get(`/public/trace/${encodeURIComponent(code)}`)
    if (route.params.traceCode !== code) {
      router.replace(`/trace/${code}`)
    }
  } catch (error) {
    message.value = error.response?.data?.message || '未查询到该追溯码'
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

watch(() => route.params.traceCode, (value) => {
  traceCode.value = value || ''
  if (traceCode.value) search()
})

onMounted(() => {
  if (traceCode.value) search()
})
</script>

<template>
  <main class="public-page">
    <header class="public-header">
      <router-link to="/login" class="public-brand">
        <span class="seal">溯</span>
        <div><strong>常德酱板鸭</strong><small>全链条可信溯源</small></div>
      </router-link>
      <div class="public-actions">
        <router-link to="/user">我的溯源中心</router-link>
        <div class="header-note">每一条记录都有链上凭证</div>
      </div>
    </header>

    <section class="trace-hero">
      <div class="river-lines" />
      <div class="hero-copy">
        <span>CHANGDE · HUNAN</span>
        <h1>查一只酱板鸭的来路</h1>
        <p>输入包装上的追溯码，查看原料、加工、物流、检验与销售信息。</p>
        <div class="search-box">
          <el-input
            v-model="traceCode"
            size="large"
            placeholder="例如：CDJBY-20261031083000-ABC123"
            :prefix-icon="Search"
            @keyup.enter="search"
          />
          <el-button type="primary" size="large" :loading="loading" @click="search">立即查询</el-button>
        </div>
        <span class="search-hint">无需登录 · 查询结果由 Hyperledger Fabric 账本核验</span>
      </div>
    </section>

    <section v-if="data" class="trace-result">
      <div class="product-summary">
        <div>
          <span class="section-label">追溯码</span>
          <div class="mono">{{ data.traceCode }}</div>
        </div>
        <div>
          <h2>{{ data.productName }}</h2>
          <p>{{ data.spec || '常德特色酱卤肉制品' }} · {{ data.quantity }} {{ data.unit }}</p>
        </div>
        <StatusTag :status="data.status" />
      </div>

      <div class="summary-grid">
        <div><span>生产企业</span><strong>{{ data.ownerOrgName }}</strong></div>
        <div><span>当前阶段</span><strong>{{ stageLabel(data.currentStage) }}</strong></div>
        <div><span>生产日期</span><strong>{{ data.productionDate || '-' }}</strong></div>
        <div><span>保质期至</span><strong>{{ data.expiryDate || '-' }}</strong></div>
      </div>

      <div class="result-grid">
        <article class="timeline-panel">
          <div class="panel-title"><h3>产品经历</h3><span>{{ data.events.length }} 个可信节点</span></div>
          <div class="public-timeline">
            <div v-for="(event, index) in data.events" :key="`${event.eventType}-${index}`">
              <span class="timeline-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div>
                <div class="event-title">
                  <strong>{{ stageLabel(event.stage) }}</strong>
                  <StatusTag :status="event.chainStatus" />
                </div>
                <p>{{ event.issuerOrgName }} · {{ formatTime(event.businessTime) }}</p>
                <small>{{ event.locationCode || '常德' }}</small>
                <div v-if="event.summary" class="public-data">
                  <span v-for="(value, key) in event.summary" :key="key">{{ fieldLabel(key) }}：{{ value }}</span>
                </div>
              </div>
            </div>
          </div>
        </article>

        <aside class="verify-panel">
          <span class="section-label">链上凭证</span>
          <div class="verify-seal">已验证</div>
          <p>最新业务哈希</p>
          <code>{{ data.lastHash }}</code>
          <p>最近交易编号</p>
          <code>{{ data.txId }}</code>
          <div class="inspection-box" v-if="data.inspections.length">
            <h4>检验结论</h4>
            <div v-for="item in data.inspections" :key="item.reportNo">
              <StatusTag :status="item.overallResult" />
              <span>{{ item.inspectionType }} · {{ item.reportNo || '-' }}</span>
            </div>
          </div>
          <p class="consumer-note">页面只展示与消费者相关的溯源摘要，不公开企业配方和经办人个人信息。</p>
        </aside>
      </div>
    </section>

    <section v-else-if="message && !loading" class="empty-result">
      <h3>{{ message }}</h3>
      <p>请核对包装上的追溯码是否完整，或联系销售商确认。</p>
    </section>
  </main>
</template>

<style scoped>
.public-page {
  min-height: 100vh;
  background: #f7f0e4;
}
.public-header {
  height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(22px, 5vw, 72px);
  border-bottom: 1px solid rgba(106, 46, 36, 0.1);
  background: #fffaf2;
}
.public-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.seal {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #f5cf89;
  background: #6a2e24;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 20px;
}
.public-brand strong,
.public-brand small {
  display: block;
}
.public-brand strong {
  font-family: "Source Han Serif SC", "SimSun", serif;
  letter-spacing: 0.08em;
}
.public-brand small,
.header-note {
  margin-top: 3px;
  color: #967f6e;
  font-size: 10px;
  letter-spacing: 0.12em;
}
.public-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.public-actions a {
  color: #7d5038;
  font-size: 12px;
}
.trace-hero {
  position: relative;
  overflow: hidden;
  min-height: 340px;
  display: grid;
  place-items: center;
  padding: 54px 20px;
  color: #fff8ed;
  background:
    radial-gradient(circle at 18% 24%, rgba(211, 153, 74, 0.28), transparent 22rem),
    linear-gradient(118deg, #2b1a15, #5d2a21 62%, #3a2019);
}
.river-lines {
  position: absolute;
  inset: 38% -5% auto -5%;
  height: 180px;
  opacity: 0.18;
  border-top: 1px solid #efc37b;
  border-radius: 50%;
  transform: rotate(-3deg);
}
.hero-copy {
  position: relative;
  width: min(760px, 100%);
  text-align: center;
}
.hero-copy > span:first-child {
  color: #dcb263;
  font-size: 10px;
  letter-spacing: 0.32em;
}
.hero-copy h1 {
  margin: 17px 0 11px;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: clamp(34px, 4vw, 52px);
}
.hero-copy p {
  color: rgba(255, 248, 237, 0.62);
  font-size: 13px;
}
.search-box {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin-top: 28px;
  padding: 7px;
  border-radius: 13px;
  background: rgba(255, 250, 242, 0.96);
}
.search-box :deep(.el-input__wrapper) {
  box-shadow: none;
}
.search-hint {
  display: block;
  margin-top: 12px;
  color: rgba(255, 248, 237, 0.44);
  font-size: 10px;
}
.trace-result {
  width: min(1120px, calc(100% - 32px));
  margin: -30px auto 60px;
  position: relative;
}
.product-summary {
  display: grid;
  grid-template-columns: 1fr 1.5fr auto;
  gap: 22px;
  align-items: center;
  padding: 24px 26px;
  border: 1px solid #e8d9c7;
  border-radius: 16px 16px 0 0;
  background: #fffaf2;
}
.section-label {
  color: #9a8473;
  font-size: 10px;
  letter-spacing: 0.13em;
}
.product-summary h2 {
  margin: 0 0 5px;
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.product-summary p {
  margin: 0;
  color: #8c7767;
  font-size: 12px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid #e8d9c7;
  border-top: 0;
  background: #fffaf2;
}
.summary-grid div {
  padding: 17px 22px;
  border-right: 1px solid #eee1d2;
}
.summary-grid div:last-child {
  border-right: 0;
}
.summary-grid span,
.summary-grid strong {
  display: block;
}
.summary-grid span {
  color: #9a8473;
  font-size: 10px;
}
.summary-grid strong {
  margin-top: 6px;
  font-size: 13px;
}
.result-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.55fr);
  gap: 18px;
  margin-top: 18px;
}
.timeline-panel,
.verify-panel {
  padding: 26px;
  border: 1px solid #e8d9c7;
  border-radius: 16px;
  background: #fffaf2;
}
.panel-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 20px;
}
.panel-title h3 {
  margin: 0;
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.panel-title span {
  color: #9a8473;
  font-size: 10px;
}
.public-timeline > div {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 16px;
  position: relative;
  padding-bottom: 26px;
}
.public-timeline > div:not(:last-child)::before {
  content: "";
  position: absolute;
  left: 21px;
  top: 34px;
  bottom: 0;
  width: 1px;
  background: #e4d3c0;
}
.timeline-index {
  display: grid;
  place-items: center;
  width: 42px;
  height: 28px;
  border-radius: 8px;
  color: #8b4a33;
  background: #f4e5d2;
  font-family: Georgia, serif;
  font-size: 11px;
}
.event-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.public-timeline p {
  margin: 7px 0 4px;
  color: #735d4e;
  font-size: 11px;
}
.public-timeline small {
  color: #a08a79;
  font-size: 10px;
}
.public-data {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 9px;
}
.public-data span {
  padding: 5px 8px;
  border-radius: 6px;
  color: #755843;
  background: #f7eee1;
  font-size: 10px;
}
.verify-seal {
  display: grid;
  place-items: center;
  width: 104px;
  height: 104px;
  margin: 26px auto;
  border: 2px solid #456756;
  border-radius: 50%;
  color: #456756;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 19px;
  box-shadow: inset 0 0 0 7px #fffaf2, inset 0 0 0 8px #8ca293;
}
.verify-panel > p {
  margin: 18px 0 7px;
  color: #907968;
  font-size: 10px;
}
.verify-panel code {
  display: block;
  overflow: hidden;
  padding: 8px 10px;
  border-radius: 7px;
  color: #654f41;
  background: #f4eadc;
  font-size: 9px;
  line-height: 1.6;
  word-break: break-all;
}
.inspection-box {
  margin-top: 22px;
  padding-top: 17px;
  border-top: 1px solid #eadbcb;
}
.inspection-box h4 {
  margin: 0 0 10px;
}
.inspection-box div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #816a5b;
  font-size: 10px;
}
.consumer-note {
  line-height: 1.7;
}
.empty-result {
  width: min(560px, calc(100% - 32px));
  margin: 80px auto;
  text-align: center;
}
.empty-result h3 {
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.empty-result p {
  color: #937e6e;
}
@media (max-width: 900px) {
  .result-grid { grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
  .summary-grid div:nth-child(2) { border-right: 0; }
}
@media (max-width: 620px) {
  .header-note { display: none; }
  .search-box { grid-template-columns: 1fr; }
  .product-summary { grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: 1fr; }
  .summary-grid div { border-right: 0; border-bottom: 1px solid #eee1d2; }
}
</style>
