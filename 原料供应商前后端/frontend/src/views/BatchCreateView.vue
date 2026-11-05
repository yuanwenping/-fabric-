<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const submitting = ref(false)
const form = reactive({
  batchType: 'RAW_DUCK',
  productName: '常德麻鸭',
  spec: '',
  quantity: 100,
  unit: 'kg',
  productionDate: '',
  expiryDate: '',
})

async function submit() {
  if (!form.quantity || Number(form.quantity) <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  submitting.value = true
  try {
    const data = await http.post('/batches', {
      ...form,
      productionDate: form.productionDate || null,
      expiryDate: form.expiryDate || null,
      parentBatchIds: [],
      relationType: null,
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
    <p class="page-subtitle">原料鸭批次创建后，会在 Fabric 账本中生成不可替换的批次起点。</p>

    <el-card class="create-card">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="批次类型" required>
            <el-select v-model="form.batchType" disabled>
              <el-option value="RAW_DUCK" label="原料鸭批次" />
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
