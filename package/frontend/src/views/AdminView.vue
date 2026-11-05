<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, Plus, Refresh } from '@element-plus/icons-vue'
import http from '../api/http'
import StatusTag from '../components/StatusTag.vue'
import { orgTypeLabels, roleCodesByOrgType, roleNames } from '../utils/roles'

const users = ref([])
const organizations = ref([])
const roles = ref([])
const dialog = ref(false)
const creating = ref(false)
const form = reactive({
  username: '',
  password: '',
  displayName: '',
  orgId: null,
  roleCode: '',
})
const selectedOrgType = computed(() =>
  organizations.value.find((org) => org.id === form.orgId)?.org_type || '',
)
const availableRoles = computed(() => {
  const allowed = roleCodesByOrgType[selectedOrgType.value] || []
  return allowed
    .map((code) => roles.value.find((role) => role.role_code === code))
    .filter(Boolean)
})

async function load() {
  const [userData, orgData, roleData] = await Promise.all([
    http.get('/admin/users'),
    http.get('/admin/organizations'),
    http.get('/admin/roles'),
  ])
  users.value = userData
  organizations.value = orgData
  roles.value = roleData
}

async function createUser() {
  if (!form.username || !form.password || !form.displayName || !form.orgId || !form.roleCode) {
    ElMessage.warning('请填写完整账号信息')
    return
  }
  creating.value = true
  try {
    await http.post('/admin/users', form)
    ElMessage.success('账号已创建')
    dialog.value = false
    Object.assign(form, { username: '', password: '', displayName: '', orgId: null, roleCode: '' })
    await load()
  } finally {
    creating.value = false
  }
}

function changeOrganization() {
  form.roleCode = availableRoles.value[0]?.role_code || ''
}

function formatRoles(row) {
  return roleNames(String(row.roles || '').split(',').filter(Boolean), row.org_type)
}

async function toggle(user) {
  await http.put(`/admin/users/${user.id}/status`, { enabled: !user.enabled })
  await load()
}

async function approve(org) {
  await http.put(`/admin/organizations/${org.id}/approve`)
  ElMessage.success(`已通过企业审核：${org.org_name || org.orgName}`)
  await load()
}

async function reject(org) {
  await http.put(`/admin/organizations/${org.id}/reject`)
  ElMessage.success(`已驳回企业申请：${org.org_name || org.orgName}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <div>
        <h1 class="page-title">平台管理</h1>
        <p class="page-subtitle">维护参与企业、账号和岗位。管理员不能代替企业写入业务事件。</p>
      </div>
      <div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="dialog = true">新建账号</el-button>
      </div>
    </div>

    <el-card v-if="organizations.some((org) => org.status === 'PENDING')" class="pending-card">
      <template #header><strong>待审核企业注册</strong></template>
      <el-table :data="organizations.filter((org) => org.status === 'PENDING')">
        <el-table-column prop="org_code" label="企业代码" width="120" />
        <el-table-column prop="org_name" label="企业名称" min-width="190" />
        <el-table-column prop="org_type" label="企业类型" width="120" />
        <el-table-column prop="region_code" label="地区代码" width="100" />
        <el-table-column prop="created_at" label="申请时间" min-width="170" />
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button type="success" link :icon="Check" @click="approve(row)">通过</el-button>
            <el-button type="danger" link :icon="Close" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="org-card">
      <template #header><strong>联盟链参与组织</strong></template>
      <div class="org-grid">
        <div v-for="org in organizations" :key="org.id">
          <span>{{ org.orgCode || org.org_code }}</span>
          <strong>{{ org.orgName || org.org_name }}</strong>
          <small>{{ org.orgType || org.org_type }} · {{ org.status }}</small>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header><strong>企业账号与角色</strong></template>
      <el-table :data="users">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="display_name" label="姓名" min-width="110" />
        <el-table-column prop="org_name" label="所属企业" min-width="190" />
        <el-table-column label="组织类型" width="130">
          <template #default="{ row }">{{ orgTypeLabels[row.org_type] || row.org_type }}</template>
        </el-table-column>
        <el-table-column prop="org_status" label="企业状态" width="110" />
        <el-table-column label="岗位角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="name in formatRoles(row)" :key="name" effect="plain" class="role-tag">
              {{ name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.enabled ? 'ACTIVE' : 'FROZEN'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggle(row)">{{ row.enabled ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新建企业账号" width="560px">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="手机号码 / 管理账号" required>
            <el-input v-model="form.username" autocomplete="off" placeholder="企业账号填写手机号码" />
          </el-form-item>
          <el-form-item label="显示姓名" required><el-input v-model="form.displayName" /></el-form-item>
          <el-form-item label="初始密码" required>
            <el-input v-model="form.password" type="password" show-password placeholder="至少8位" />
          </el-form-item>
          <el-form-item label="所属企业" required>
            <el-select v-model="form.orgId" style="width: 100%" @change="changeOrganization">
              <el-option v-for="org in organizations" :key="org.id" :value="org.id" :label="org.org_name || org.orgName" />
            </el-select>
          </el-form-item>
          <el-form-item label="岗位角色" required>
            <el-select v-model="form.roleCode" style="width: 100%">
              <el-option
                v-for="role in availableRoles"
                :key="role.role_code"
                :value="role.role_code"
                :label="roleNames([role.role_code], selectedOrgType)[0]"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createUser">创建账号</el-button>
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
.org-card {
  margin-bottom: 18px;
}
.pending-card {
  margin-bottom: 18px;
  border-color: #d7a66e;
}
.org-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.org-grid > div {
  padding: 15px;
  border-radius: 11px;
  background: #f6ecdf;
}
.org-grid span,
.org-grid strong,
.org-grid small {
  display: block;
}
.org-grid span,
.org-grid small {
  color: #947d6b;
  font-size: 10px;
}
.org-grid strong {
  margin: 7px 0;
  font-size: 13px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.role-tag {
  margin: 2px 5px 2px 0;
}
@media (max-width: 760px) {
  .org-grid, .form-grid { grid-template-columns: 1fr; }
  .header-row { flex-direction: column; }
}
</style>
