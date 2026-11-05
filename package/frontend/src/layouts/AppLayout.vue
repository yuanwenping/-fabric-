<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Box,
  DataAnalysis,
  Management,
  Plus,
  SwitchButton,
  UserFilled,
  Link,
  Menu as MenuIcon,
  Lock,
  User,
  CircleCheck,
  Tickets,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'
import { orgTypeLabels, primaryJobRole, roleNames } from '../utils/roles'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const chainState = ref('检测中')
const passwordVisible = ref(false)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const navItems = computed(() => {
  const items = [
    { to: '/dashboard', label: '工作台', icon: DataAnalysis },
    { to: '/batches', label: '溯源批次', icon: Box },
  ]
  if (auth.canCreateBatch) items.push({ to: '/batches/new', label: '创建批次', icon: Plus })
  if (auth.roles.length) items.push({ to: '/submissions', label: '我的提交', icon: Tickets })
  if (auth.roles.includes('ORG_MANAGER')) items.push({ to: '/reviews', label: '业务审核', icon: CircleCheck })
  if (auth.roles.includes('ORG_MANAGER')) items.push({ to: '/team', label: '员工账号', icon: User })
  if (auth.user?.orgType === 'ADMIN') items.push({ to: '/admin', label: '平台管理', icon: Management })
  return items
})

const roleLabel = computed(() => orgTypeLabels[auth.user?.orgType] || '企业用户')
const jobLabel = computed(() => primaryJobRole(auth.roles, auth.orgType))
const allRoleLabels = computed(() => roleNames(auth.roles, auth.orgType).join(' / '))

const pageTitle = computed(() => {
  if (route.path.includes('/batches/new')) return '创建溯源批次'
  if (route.path.match(/\/batches\/[^/]+/)) return '批次详情'
  if (route.path.includes('/batches')) return '溯源批次'
  if (route.path.includes('/team')) return '企业员工账号'
  if (route.path.includes('/submissions')) return '我的业务提交'
  if (route.path.includes('/reviews')) return '业务审核中心'
  if (route.path.includes('/admin')) return '平台管理'
  return '工作台'
})

onMounted(async () => {
  await auth.refresh()
  try {
    const status = await http.get('/system/chain')
    chainState.value = status.state?.startsWith('CONNECTED') ? '区块链已连接' : '链路待检查'
  } catch {
    chainState.value = '链路待检查'
  }
})

function logout() {
  auth.logout()
  router.replace('/login')
}

async function changePassword() {
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('新密码至少需要 8 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  await http.post('/auth/change-password', {
    currentPassword: passwordForm.currentPassword,
    newPassword: passwordForm.newPassword,
  })
  ElMessage.success('密码修改成功，请重新登录')
  passwordVisible.value = false
  logout()
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar desktop">
      <div class="logo">
        <div class="logo-mark">溯</div>
        <div>
          <strong>常德酱板鸭</strong>
          <span>可信溯源平台</span>
        </div>
      </div>
      <nav>
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          :class="{ active: route.path === item.to || (item.to !== '/dashboard' && route.path.startsWith(item.to)) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </router-link>
      </nav>
      <div class="side-bottom">
        <router-link to="/trace" class="public-link">消费者追溯查询</router-link>
        <div class="org-box">
          <el-icon><UserFilled /></el-icon>
          <div>
            <strong>{{ auth.user?.displayName }} · {{ jobLabel }}</strong>
            <span>{{ auth.user?.orgName }}</span>
            <em>{{ roleLabel }} · {{ allRoleLabels }}</em>
          </div>
        </div>
        <button class="change-password" @click="passwordVisible = true">
          <el-icon><Lock /></el-icon>修改密码
        </button>
        <button class="logout" @click="logout"><el-icon><SwitchButton /></el-icon>退出登录</button>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <button class="mobile-menu" @click="mobileOpen = true"><el-icon><MenuIcon /></el-icon></button>
        <div>
          <div class="breadcrumb">{{ auth.user?.orgName }} / {{ allRoleLabels }}</div>
          <h2>{{ pageTitle }}</h2>
        </div>
        <div class="chain-state">
          <span class="chain-dot" />
          <div>
            <strong>{{ chainState }}</strong>
            <small>tracechannel · tracecc</small>
          </div>
        </div>
      </header>
      <main class="page-content">
        <router-view />
      </main>
    </div>

    <el-drawer v-model="mobileOpen" direction="ltr" size="280px" :with-header="false">
      <div class="mobile-side">
        <div class="logo">
          <div class="logo-mark">溯</div>
          <div><strong>常德酱板鸭</strong><span>可信溯源平台</span></div>
        </div>
        <nav>
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            @click="mobileOpen = false"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </router-link>
        </nav>
      </div>
    </el-drawer>

    <el-dialog v-model="passwordVisible" title="修改登录密码" width="460px">
      <el-form label-position="top">
        <el-form-item label="当前密码" required>
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少8位" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
}
.sidebar {
  width: 250px;
  min-height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  padding: 28px 20px 22px;
  color: #f9efe1;
  background:
    linear-gradient(180deg, rgba(106, 46, 36, 0.24), transparent 32%),
    #2b1a15;
}
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px 28px;
}
.logo-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(233, 187, 113, 0.7);
  color: #e8ba6d;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 21px;
}
.logo strong,
.logo span {
  display: block;
}
.logo strong {
  font-family: "Source Han Serif SC", "SimSun", serif;
  letter-spacing: 0.1em;
}
.logo span {
  margin-top: 4px;
  color: rgba(255, 247, 235, 0.48);
  font-size: 10px;
  letter-spacing: 0.2em;
}
nav {
  display: grid;
  gap: 7px;
}
nav a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  color: rgba(255, 247, 235, 0.64);
  transition: 0.2s ease;
}
nav a:hover,
nav a.active {
  color: #fff8ed;
  background: rgba(255, 247, 235, 0.08);
}
nav a.active {
  box-shadow: inset 3px 0 #d3a151;
}
.side-bottom {
  margin-top: auto;
}
.public-link {
  display: block;
  margin: 0 8px 20px;
  color: #d9ac62;
  font-size: 12px;
}
.org-box {
  display: flex;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(255, 247, 235, 0.08);
  border-radius: 12px;
  background: rgba(255, 247, 235, 0.035);
}
.org-box strong,
.org-box span,
.org-box em {
  display: block;
}
.org-box strong {
  font-size: 13px;
}
.org-box span {
  overflow: hidden;
  max-width: 150px;
  margin-top: 4px;
  color: rgba(255, 247, 235, 0.48);
  font-size: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.org-box em {
  margin-top: 7px;
  color: #d5a659;
  font-size: 10px;
  font-style: normal;
}
.change-password,
.logout {
  width: 100%;
  padding: 10px;
  border: 0;
  background: transparent;
  color: rgba(255, 247, 235, 0.46);
  text-align: left;
  cursor: pointer;
}
.change-password {
  margin-top: 10px;
}
.logout {
  margin-top: 2px;
}
.change-password .el-icon,
.logout .el-icon {
  margin-right: 8px;
}
.main-area {
  flex: 1;
  min-width: 0;
}
.topbar {
  height: 94px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 36px;
  border-bottom: 1px solid rgba(106, 46, 36, 0.08);
  background: rgba(247, 240, 228, 0.82);
  backdrop-filter: blur(10px);
}
.breadcrumb {
  color: #9a8675;
  font-size: 10px;
  letter-spacing: 0.12em;
}
.topbar h2 {
  margin: 5px 0 0;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 23px;
}
.chain-state {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border: 1px solid #e6d8c7;
  border-radius: 10px;
  background: rgba(255, 250, 242, 0.8);
}
.chain-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #456756;
  box-shadow: 0 0 0 5px rgba(69, 103, 86, 0.1);
}
.chain-state strong,
.chain-state small {
  display: block;
}
.chain-state strong {
  font-size: 11px;
}
.chain-state small {
  margin-top: 3px;
  color: #9c8979;
  font-family: Consolas, monospace;
  font-size: 9px;
}
.page-content {
  padding: 30px 36px 48px;
}
.mobile-menu {
  display: none;
}
.mobile-side {
  min-height: 100%;
  margin: -20px;
  padding: 28px 20px;
  color: #fff8ed;
  background: #2b1a15;
}
@media (max-width: 860px) {
  .desktop { display: none; }
  .mobile-menu {
    display: grid;
    place-items: center;
    width: 38px;
    height: 38px;
    margin-right: 12px;
    border: 1px solid #e2d4c3;
    border-radius: 9px;
    background: #fffaf2;
  }
  .topbar { padding: 0 18px; }
  .page-content { padding: 22px 16px 36px; }
  .chain-state small { display: none; }
}
</style>
