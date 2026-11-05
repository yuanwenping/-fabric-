<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' },
})

const config = computed(() => {
  const value = props.status || ''
  if (['COMMITTED', 'ACTIVE', 'ON_SALE', 'PASS', 'RECEIVED'].includes(value)) {
    return { label: statusLabel(value), type: 'success', color: '#456756' }
  }
  if (['FAILED', 'REJECTED', 'FROZEN', 'RECALLED', 'FAIL', 'ERROR'].includes(value)) {
    return { label: statusLabel(value), type: 'danger', color: '#b54235' }
  }
  if (['PENDING', 'PENDING_REVIEW', 'CHAIN_FAILED', 'DISPATCHED', 'IN_TRANSIT', 'INSPECTED'].includes(value)) {
    return { label: statusLabel(value), type: 'warning', color: '#c98a3b' }
  }
  return { label: statusLabel(value), type: 'info', color: '#8b7767' }
})

function statusLabel(value) {
  return {
    COMMITTED: '已上链',
    FAILED: '上链失败',
    CHAIN_FAILED: '上链失败',
    PENDING: '待上链',
    ACTIVE: '正常',
    DISPATCHED: '已发货',
    IN_TRANSIT: '运输中',
    DELIVERED: '已送达',
    RECEIVED: '已收货',
    ON_SALE: '销售中',
    SOLD: '已售出',
    FROZEN: '已冻结',
    RECALLED: '已召回',
    INSPECTED: '已检验',
    PASS: '合格',
    FAIL: '不合格',
    ERROR: '连接异常',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
  }[value] || value
}
</script>

<template>
  <span class="status-pill" :style="{ '--status-color': config.color }">
    <span class="status-dot" :style="{ background: config.color }" />
    {{ config.label }}
  </span>
</template>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 27px;
  padding: 2px 10px;
  border: 1px solid color-mix(in srgb, var(--status-color) 28%, transparent);
  border-radius: 999px;
  color: var(--status-color);
  background: color-mix(in srgb, var(--status-color) 9%, white);
  font-size: 12px;
  white-space: nowrap;
}
</style>
