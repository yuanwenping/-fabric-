<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { eventSchemas, requiredFieldsByEvent } from './eventSchemas'

const props = defineProps({
  modelValue: Boolean,
  batch: { type: Object, required: true },
  orgType: { type: String, default: '' },
  roles: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const submitting = ref(false)
const organizations = ref([])
const allowedEventTypes = ref([])
const uploads = ref([])
const form = reactive({
  eventType: '',
  businessTime: '',
  locationCode: '',
  targetOrgId: null,
  carrierOrgId: null,
  processData: {},
})

const options = computed(() =>
  allowedEventTypes.value.filter((type) => Boolean(eventSchemas[type])),
)
const schema = computed(() => eventSchemas[form.eventType] || { fields: [] })
const requiredFields = computed(() => requiredFieldsByEvent[form.eventType] || [])
const allowedTargetTypes = computed(() => {
  if (props.orgType === 'SUPPLIER') return ['PROCESSOR']
  if (props.orgType === 'PROCESSOR') return ['LOGISTICS', 'RETAILER']
  return []
})
const targetOrganizations = computed(() =>
  organizations.value.filter((org) =>
    org.id !== props.batch.custodian_org_id && allowedTargetTypes.value.includes(org.org_type),
  ),
)
const carrierOrganizations = computed(() =>
  organizations.value.filter((org) => org.org_type === 'LOGISTICS' && org.id !== props.batch.custodian_org_id),
)

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      uploads.value = []
      const organizationRequest = organizations.value.length
        ? Promise.resolve(organizations.value)
        : http.get('/directory/organizations')
      const [allowed, organizationData] = await Promise.all([
        http.get('/events/allowed'),
        organizationRequest,
      ])
      allowedEventTypes.value = allowed
      organizations.value = organizationData
      form.eventType = options.value[0] || ''
      if (!options.value.length) {
        ElMessage.warning('当前岗位没有可提交的业务类型，请联系企业管理员检查岗位权限')
        emit('update:modelValue', false)
        return
      }
      form.businessTime = new Date().toISOString().slice(0, 19)
      form.locationCode = ''
      form.targetOrgId = null
      form.carrierOrgId = null
      form.processData = {}
      chooseEvent(form.eventType)
    }
  },
)

function chooseEvent(type) {
  form.processData = {}
  for (const field of eventSchemas[type]?.fields || []) {
    if (['targetOrg', 'carrierOrg'].includes(field.type)) continue
    form.processData[field.key] = field.type === 'select' ? field.options?.[0] : ''
  }
}

async function uploadFile(file) {
  const data = new FormData()
  data.append('file', file.raw)
  const evidence = await http.post(`/batches/${props.batch.id}/evidence`, data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  uploads.value.push(evidence)
  ElMessage.success(`附件 ${evidence.original_name} 已保存`)
}

async function submit() {
  if (!form.eventType) return
  await formRef.value.validate()
  const missingLabels = schema.value.fields
    .filter((field) => requiredFields.value.includes(field.key))
    .filter((field) => {
      if (field.type === 'targetOrg') return !form.targetOrgId
      if (field.type === 'carrierOrg') return false
      const value = form.processData[field.key]
      return value === null || value === undefined || String(value).trim() === ''
    })
    .map((field) => field.label)
  if (!form.businessTime) missingLabels.unshift('业务发生时间')
  if (missingLabels.length) {
    ElMessage.warning(`请填写：${missingLabels.join('、')}`)
    return
  }
  submitting.value = true
  try {
    const data = { ...form.processData }
    if (form.eventType === 'INSPECTION') {
      data.indicators = [{
        name: data.indicatorName,
        value: data.indicatorValue,
        limit: data.limitValue,
        result: data.indicatorResult,
      }]
    }
    const request = {
      eventType: form.eventType,
      businessTime: form.businessTime || null,
      locationCode: form.locationCode,
      processData: data,
      evidenceIds: uploads.value.map((item) => item.id),
      requestId: crypto.randomUUID(),
      targetOrgId: form.eventType === 'DISPATCH' ? form.targetOrgId : null,
      carrierOrgId: form.eventType === 'DISPATCH' ? form.carrierOrgId : null,
      discrepancyCode: data.discrepancyCode || null,
    }
    await http.post(`/batches/${props.batch.id}/events`, request)
    ElMessage.success('业务记录已提交，等待企业管理员审核')
    emit('saved')
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="720px"
    class="event-dialog"
    :title="schema.title || '登记业务事件'"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" label-position="top" class="event-form">
      <el-alert
        :title="`当前岗位仅可提交：${options.map((type) => eventSchemas[type]?.label || type).join('、')}`"
        type="info"
        :closable="false"
        class="role-scope"
      />
      <div class="form-grid">
        <el-form-item label="业务动作" required>
          <el-select v-model="form.eventType" :disabled="options.length === 1" @change="chooseEvent">
            <el-option
              v-for="type in options"
              :key="type"
              :value="type"
              :label="eventSchemas[type]?.label || type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务发生时间" required>
          <el-date-picker
            v-model="form.businessTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="地点或节点编码">
          <el-input v-model="form.locationCode" placeholder="例如：常德加工一车间" />
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item
          v-for="field in schema.fields"
          :key="field.key"
          :label="field.label"
          :required="requiredFields.includes(field.key)"
        >
          <el-select
            v-if="field.type === 'select'"
            v-model="form.processData[field.key]"
            style="width: 100%"
          >
            <el-option v-for="option in field.options" :key="option" :value="option" :label="option" />
          </el-select>
          <el-select
            v-else-if="field.type === 'targetOrg'"
            v-model="form.targetOrgId"
            placeholder="选择收货企业"
            style="width: 100%"
          >
            <el-option
              v-for="org in targetOrganizations"
              :key="org.id"
              :value="org.id"
              :label="`${org.org_name}（${org.org_type}）`"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'carrierOrg'"
            v-model="form.carrierOrgId"
            clearable
            placeholder="选择承运企业"
            style="width: 100%"
          >
            <el-option v-for="org in carrierOrganizations" :key="org.id" :value="org.id" :label="org.org_name" />
          </el-select>
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form.processData[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form.processData[field.key]"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form.processData[field.key]"
            type="textarea"
            :rows="3"
          />
          <el-input v-else v-model="form.processData[field.key]" :placeholder="field.label" />
        </el-form-item>
      </div>

      <el-form-item label="现场附件">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="uploadFile"
          multiple
        >
          <el-button plain>上传图片、视频或检验报告</el-button>
        </el-upload>
        <div class="upload-list">
          <el-tag
            v-for="file in uploads"
            :key="file.id"
            closable
            @close="uploads = uploads.filter((item) => item.id !== file.id)"
          >
            {{ file.original_name }}
          </el-tag>
        </div>
        <div class="field-hint">文件不会直接写入区块链，链上保存 SHA-256 证据根。</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交管理员审核</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.role-scope {
  margin-bottom: 18px;
}
.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
}
.field-hint {
  margin-top: 8px;
  color: #8b7767;
  font-size: 12px;
}
@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
