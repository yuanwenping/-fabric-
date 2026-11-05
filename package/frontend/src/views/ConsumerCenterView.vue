<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Star, Clock, User, SwitchButton } from '@element-plus/icons-vue'
import http from '../api/http'
import { useConsumerAuthStore } from '../stores/consumerAuth'

const router = useRouter()
const auth = useConsumerAuthStore()
const loading = ref(true)
const profile = ref(null)
const history = ref([])
const favorites = ref([])
const activeTab = ref('history')
const profileDialog = ref(false)
const passwordDialog = ref(false)
const traceCode = ref('')
const profileForm = reactive({ nickname: '', gender: '不公开', regionCode: '430700' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const initial = computed(() => (profile.value?.nickname || '用户').slice(0, 1))
const profileCompleted = computed(() => Boolean(profile.value?.profile_completed))

async function load() {
  loading.value = true
  try {
    const [profileData, historyData, favoriteData] = await Promise.all([
      http.get('/consumer/profile'),
      http.get('/consumer/history'),
      http.get('/consumer/favorites'),
    ])
    profile.value = profileData
    history.value = historyData
    favorites.value = favoriteData
    Object.assign(profileForm, {
      nickname: profileData.nickname || '',
      gender: profileData.gender || '不公开',
      regionCode: profileData.region_code || '430700',
    })
    profileDialog.value = !profileCompleted.value
  } finally {
    loading.value = false
  }
}

async function completeProfile() {
  if (profileForm.nickname.trim().length < 2) {
    ElMessage.warning('昵称至少需要2个字符')
    return
  }
  await http.put('/consumer/profile', profileForm)
  ElMessage.success('个人信息已完善')
  profileDialog.value = false
  await load()
}

async function queryTrace() {
  const code = traceCode.value.trim()
  if (!code) return
  const data = await http.get(`/public/trace/${encodeURIComponent(code)}`)
  if (!data) return
  await http.post('/consumer/history', { traceCode: code })
  router.push(`/trace/${code}`)
}

async function favoriteCurrent() {
  const code = traceCode.value.trim()
  if (!code) {
    ElMessage.warning('请先输入追溯码')
    return
  }
  const result = await http.post('/consumer/favorites/toggle', { traceCode: code })
  ElMessage.success(result.favorited ? '已加入收藏' : '已取消收藏')
  favorites.value = await http.get('/consumer/favorites')
}

async function changePassword() {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  await http.post('/consumer/change-password', {
    currentPassword: passwordForm.currentPassword,
    newPassword: passwordForm.newPassword,
  })
  ElMessage.success('密码修改成功')
  passwordDialog.value = false
}

function logout() {
  auth.logout()
  router.replace('/user/login')
}

onMounted(load)
</script>

<template>
  <main class="consumer-center" v-loading="loading">
    <header>
      <router-link to="/trace" class="brand">
        <span>溯</span>
        <div><strong>常德酱板鸭</strong><small>消费者溯源中心</small></div>
      </router-link>
      <div class="header-actions">
        <router-link to="/trace">游客查询</router-link>
        <router-link to="/login">企业入口</router-link>
        <el-button link :icon="SwitchButton" @click="logout">退出</el-button>
      </div>
    </header>

    <section class="hero">
      <div>
        <span>MY TRACE CENTER</span>
        <h1>你好，{{ profile?.nickname || '溯源用户' }}</h1>
        <p>在这里保存你查看过的产品，了解食品从产地到销售的完整链路。</p>
      </div>
      <div class="profile-brief">
        <div class="avatar"><el-icon><User /></el-icon></div>
        <div>
          <strong>{{ profile?.nickname }}</strong>
          <span>{{ profile?.phone }}</span>
        </div>
        <el-button plain size="small" @click="profileDialog = true">编辑资料</el-button>
        <el-button link size="small" @click="passwordDialog = true">修改密码</el-button>
      </div>
    </section>

    <section class="search-panel">
      <div>
        <h2>查询产品溯源</h2>
        <p>输入包装上的追溯码，查询记录会自动保存。</p>
      </div>
      <div class="trace-search">
        <el-input
          v-model="traceCode"
          size="large"
          :prefix-icon="Search"
          placeholder="请输入追溯码"
          @keyup.enter="queryTrace"
        />
        <el-button type="primary" size="large" @click="queryTrace">立即查询</el-button>
        <el-button size="large" :icon="Star" @click="favoriteCurrent">收藏</el-button>
      </div>
    </section>

    <section class="content">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="history">
          <template #label><span><el-icon><Clock /></el-icon> 最近查询</span></template>
          <div v-if="history.length" class="trace-list">
            <article v-for="item in history" :key="item.batchId" @click="router.push(`/trace/${item.traceCode}`)">
              <div>
                <span>追溯码</span>
                <strong class="mono">{{ item.traceCode }}</strong>
              </div>
              <div><span>产品</span><strong>{{ item.productName }}</strong></div>
              <div><span>生产企业</span><strong>{{ item.producerName }}</strong></div>
              <div><span>查询时间</span><strong>{{ item.viewedAt }}</strong></div>
            </article>
          </div>
          <el-empty v-else description="还没有查询记录" />
        </el-tab-pane>

        <el-tab-pane name="favorites">
          <template #label><span><el-icon><Star /></el-icon> 我的收藏</span></template>
          <div v-if="favorites.length" class="trace-list">
            <article v-for="item in favorites" :key="item.batchId" @click="router.push(`/trace/${item.traceCode}`)">
              <div>
                <span>追溯码</span>
                <strong class="mono">{{ item.traceCode }}</strong>
              </div>
              <div><span>产品</span><strong>{{ item.productName }}</strong></div>
              <div><span>规格</span><strong>{{ item.spec || '-' }}</strong></div>
              <div><span>收藏时间</span><strong>{{ item.favoritedAt }}</strong></div>
            </article>
          </div>
          <el-empty v-else description="还没有收藏产品" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="profileDialog"
      title="完善个人信息"
      width="520px"
      :show-close="profileCompleted"
      :close-on-click-modal="profileCompleted"
      :close-on-press-escape="profileCompleted"
    >
      <div class="first-login-note">
        第一次进入消费者中心，请先设置昵称和基本地区信息。无需填写身份证号等敏感信息。
      </div>
      <el-form label-position="top">
        <el-form-item label="昵称" required>
          <el-input v-model="profileForm.nickname" maxlength="30" placeholder="请输入2到30个字符" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.gender">
            <el-radio-button value="男">男</el-radio-button>
            <el-radio-button value="女">女</el-radio-button>
            <el-radio-button value="其他">其他</el-radio-button>
            <el-radio-button value="不公开">不公开</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所在地区代码">
          <el-input v-model="profileForm.regionCode" placeholder="例如：430700" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="profileCompleted" @click="profileDialog = false">取消</el-button>
        <el-button type="primary" @click="completeProfile">保存个人信息</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialog" title="修改密码" width="460px">
      <el-form label-position="top">
        <el-form-item label="当前密码"><el-input v-model="passwordForm.currentPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.consumer-center {
  min-height: 100vh;
  padding-bottom: 60px;
  background:
    radial-gradient(circle at 85% 8%, rgba(198, 131, 56, .14), transparent 24rem),
    #f7f0e4;
}
header {
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(22px, 5vw, 72px);
  border-bottom: 1px solid #e4d5c5;
  background: #fffaf2;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.brand > span {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #f4ca80;
  background: #6a2e24;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 20px;
}
.brand strong,
.brand small {
  display: block;
}
.brand small {
  margin-top: 3px;
  color: #967f6f;
  font-size: 10px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  color: #876f5e;
  font-size: 12px;
}
.hero {
  width: min(1120px, calc(100% - 32px));
  margin: 32px auto 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 34px;
  border-radius: 20px;
  color: #fff8ed;
  background:
    radial-gradient(circle at 88% 30%, rgba(220, 162, 75, .35), transparent 18rem),
    linear-gradient(120deg, #351e18, #6a2e24);
}
.hero > div > span {
  color: #dfb66c;
  font-size: 10px;
  letter-spacing: .2em;
}
.hero h1 {
  margin: 11px 0;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 31px;
}
.hero p {
  margin: 0;
  color: rgba(255, 248, 237, .64);
  font-size: 12px;
}
.profile-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border: 1px solid rgba(255,255,255,.13);
  border-radius: 13px;
  background: rgba(255,255,255,.055);
}
.avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255,255,255,.1);
  font-size: 19px;
}
.profile-brief strong,
.profile-brief span {
  display: block;
}
.profile-brief strong {
  font-size: 13px;
}
.profile-brief span {
  margin-top: 3px;
  color: rgba(255,255,255,.48);
  font-size: 10px;
}
.search-panel,
.content {
  width: min(1120px, calc(100% - 32px));
  margin: 18px auto 0;
  border: 1px solid #e7d7c6;
  border-radius: 16px;
  background: #fffaf2;
}
.search-panel {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 24px;
  align-items: center;
  padding: 24px;
}
.search-panel h2 {
  margin: 0 0 6px;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 18px;
}
.search-panel p {
  margin: 0;
  color: #927d6d;
  font-size: 11px;
}
.trace-search {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
}
.content {
  padding: 12px 24px 24px;
}
.content :deep(.el-tabs__item span) {
  display: flex;
  align-items: center;
  gap: 5px;
}
.trace-list {
  display: grid;
  gap: 10px;
}
.trace-list article {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1.2fr .9fr;
  gap: 14px;
  padding: 15px 16px;
  border-radius: 11px;
  background: #f7eee2;
  cursor: pointer;
  transition: .2s ease;
}
.trace-list article:hover {
  transform: translateY(-1px);
  box-shadow: 0 7px 18px rgba(78, 44, 27, .07);
}
.trace-list span,
.trace-list strong {
  display: block;
}
.trace-list span {
  color: #9a8472;
  font-size: 9px;
}
.trace-list strong {
  overflow: hidden;
  margin-top: 4px;
  color: #4d382d;
  font-size: 11px;
  text-overflow: ellipsis;
}
.first-login-note {
  margin-bottom: 18px;
  padding: 11px 13px;
  border-left: 3px solid #b27a42;
  color: #7d6553;
  background: #f8eee2;
  line-height: 1.7;
  font-size: 11px;
}
@media (max-width: 800px) {
  .hero { align-items: flex-start; flex-direction: column; }
  .search-panel { grid-template-columns: 1fr; }
  .trace-search { grid-template-columns: 1fr; }
  .trace-list article { grid-template-columns: 1fr 1fr; }
  .header-actions a { display: none; }
}
</style>
