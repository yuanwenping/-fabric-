<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import http from '../api/http'
import StatusTag from '../components/StatusTag.vue'
import { useAuthStore } from '../stores/auth'
import { roleNames } from '../utils/roles'

const auth = useAuthStore()
const users = ref([])
const roles = ref([])
const createVisible = ref(false)
const passwordVisible = ref(false)
const saving = ref(false)
const selectedUser = ref(null)
const form = reactive({
  phone: '',
  password: '',
  displayName: '',
  roleCode: '',
})
const resetForm = reactive({ newPassword: '' })

async function load() {
  const [userData, roleData] = await Promise.all([
    http.get('/org/users'),
    http.get('/org/roles'),
  ])
  users.value = userData
  roles.value = roleData
  if (!form.roleCode && roleData.length) form.roleCode = roleData[0].role_code
}

async function createUser() {
  if (!form.phone || !form.password || !form.displayName || !form.roleCode) {
    ElMessage.warning('请填写完整账号信息')
    return
  }
  saving.value = true
  try {
    await http.post('/org/users', form)
    ElMessage.success('企业员工账号已创建')
    createVisible.value = false
    Object.assign(form, { phone: '', password: '', displayName: '', roleCode: roles.value[0]?.role_code || '' })
    await load()
  } finally {
    saving.value = false
  }
}

async function toggle(user) {
  await http.put(`/org/users/${user.id}/status`, { enabled: !user.enabled })
  await load()
}

function openReset(user) {
  selectedUser.value = user
  resetForm.newPassword = ''
  passwordVisible.value = true
}

function userRoles(user) {
  return roleNames(String(user.roles || '').split(',').filter(Boolean), auth.orgType)
}

async function resetPassword() {
  if (resetForm.newPassword.length < 8) {
    ElMessage.warning('新密码至少需要 8 位')
    return
  }
  await http.put(`/org/users/${selectedUser.value.id}/password`, {
    newPassword: resetForm.newPassword,
  })
  passwordVisible.value = false
  ElMessage.success('密码已重置')
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <div>
        <h1 class="page-title">企业员工账号</h1>
        <p class="page-subtitle">管理员可为当前企业配置各岗位员工；员工填报后须由本企业管理员审核，通过后才会上链和流转。</p>
      </div>
      <div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="createVisible = true">添加员工</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="users">
        <el-table-column prop="username" label="登录账号" min-width="140" />
        <el-table-column prop="display_name" label="姓名" min-width="120" />
        <el-table-column label="岗位角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="name in userRoles(row)" :key="name" effect="plain" class="role-tag">
              {{ name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.enabled ? 'ACTIVE' : 'FROZEN'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button link :type="row.enabled ? 'danger' : 'success'" @click="toggle(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="添加企业员工" width="560px">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="手机号码（登录账号）" required>
            <el-input
              v-model="form.phone"
              maxlength="11"
              inputmode="numeric"
              autocomplete="off"
              placeholder="请输入11位手机号码"
            />
          </el-form-item>
          <el-form-item label="员工姓名" required>
            <el-input v-model="form.displayName" />
          </el-form-item>
          <el-form-item label="初始密码" required>
            <el-input v-model="form.password" type="password" show-password placeholder="至少8位" />
          </el-form-item>
          <el-form-item label="岗位角色" required>
            <el-select v-model="form.roleCode" style="width: 100%">
              <el-option
                v-for="role in roles"
                :key="role.role_code"
                :value="role.role_code"
                :label="roleNames([role.role_code], auth.orgType)[0]"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createUser">创建员工账号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置员工密码" width="460px">
      <p class="reset-hint">正在重置：{{ selectedUser?.display_name }}（{{ selectedUser?.username }}）</p>
      <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="新密码，至少8位" />
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="resetPassword">确认重置</el-button>
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
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.reset-hint {
  margin: 0 0 16px;
  color: #7f6958;
  font-size: 12px;
}
.role-tag {
  margin: 2px 5px 2px 0;
}
@media (max-width: 720px) {
  .form-grid { grid-template-columns: 1fr; }
  .header-row { flex-direction: column; }
}
</style>
