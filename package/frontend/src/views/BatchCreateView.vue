<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const parents = ref([])
const form = reactive({
  batchType: auth.orgType === 'SUPPLIER' ? 'RAW_DUCK' : 'SEMI_FINISHED',
  productName: auth.orgType === 'SUPPLIER' ? '常德麻鸭' : '酱板鸭半成品',
  spec: '',
  quantity: 100,
  unit: 'kg',
  productionDate: '',
  expiryDate: '',
  parentBatchIds: [],
  relationType: 'MERGE',
})

const isProcessor = computed(() => auth.orgType === 'PROCESSOR')

onMounted(async () => {
  if (isProcessor.value) {
    const data = await http.get('/batches', { params: { page: 1, size: 100 } })
    parents.value = data.items.filter((item) => item.batchType !== 'FINISHED')
    if (route.query.parent) form.parentBatchIds = [route.query.parent]
  }
})

async function submit() {
  if (!form.quantity || Number(form.quantity) <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  if (isProcessor.value && !form.parentBatchIds.length) {
    ElMessage.warning('请选择上游关联批次')
    return
  }
  submitting.value = true
  try {
    const data = await http.post('/batches', {
      ...form,
      productionDate: form.productionDate || null,
      expiryDate: form.expiryDate || null,
    })
    ElMessage.success('批次已创建并提交上链')
    router.replace(`/batches/${data.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h1 class="page-title">创建溯源批次</h1>
    <p class="page-subtitle">批次创建后，会在 Fabric 账本中生成不可替换的批次起点。</p>

    <el-card class="create-card">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="批次类型" required>
            <el-select v-model="form.batchType" :disabled="!isProcessor">
              <el-option v-if="!isProcessor" value="RAW_DUCK" label="原料鸭批次" />
              <template v-else>
                <el-option value="SEMI_FINISHED" label="生产加工批次" />
                <el-option value="FINISHED" label="成品批次" />
              </template>
            </el-select>
          </el-form-item>
          <el-form-item label="产品名称">
            <el-input v-model="form.productName" />
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="form.spec" placeholder="例如：散装、300g/袋" />
          </el-form-item>
          <el-form-item label="数量" required>
            <el-input-number v-model="form.quantity" :min="0.001" :precision="3" style="width: 100%" />
          </el-form-item>
          <el-form-item label="计量单位" required>
            <el-select v-model="form.unit">
              <el-option value="kg" label="千克（kg）" />
              <el-option value="只" label="只" />
              <el-option value="袋" label="袋" />
              <el-option value="箱" label="箱" />
            </el-select>
          </el-form-item>
          <el-form-item label="生产日期">
            <el-date-picker v-model="form.productionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="保质期至">
            <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>

        <el-form-item v-if="isProcessor" label="上游关联批次" required>
          <el-select v-model="form.parentBatchIds" multiple filterable placeholder="选择原料鸭或半成品批次">
            <el-option
              v-for="batch in parents"
              :key="batch.id"
              :value="batch.id"
              :label="`${batch.batchNo} · ${batch.productName || '原料鸭'} · ${batch.currentStage}`"
            />
          </el-select>
          <div class="hint">一个加工批次可以合并多个原料批次，系统会保存批次血缘关系。</div>
        </el-form-item>

        <div class="submit-row">
          <el-button @click="router.back()">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">创建并上链</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.create-card { max-width: 920px; }
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.hint {
  margin-top: 7px;
  color: #947f6e;
  font-size: 11px;
}
.submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eadccb;
}
@media (max-width: 680px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
