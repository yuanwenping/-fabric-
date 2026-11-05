<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import duckImage from '../assets/changde-duck.svg'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function login() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success(`欢迎，${auth.user.displayName}`)
    router.replace(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}

</script>

<template>
  <main class="login-page">
    <section class="story-panel">
      <img class="duck-image" :src="duckImage" alt="常德酱板鸭抽象主视觉" />
      <div class="story-overlay" />
      <div class="brand">
        <div class="brand-mark">溯</div>
        <div>
          <div class="brand-name">常德酱板鸭</div>
          <div class="brand-en">CHANGDE DUCK TRACE</div>
        </div>
      </div>
      <div class="story-copy">
        <div class="eyebrow">沅水酱香 · 全链可信</div>
        <h1>一只酱板鸭，<br />一条可信的来路。</h1>
        <p>从养殖、加工、冷链到餐桌，每个关键环节均形成可核验的链上记录。</p>
        <div class="chain-line">
          <span>养殖</span><i /><span>加工</span><i /><span>物流</span><i /><span>销售</span>
        </div>
      </div>
      <div class="tech-note">
        <span class="tech-dot" />
        Hyperledger Fabric 联盟链存证
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="login-heading">
          <div class="small-title">企业账号登录</div>
          <h2>欢迎回到可信溯源平台</h2>
          <p>系统会根据所属企业自动匹配对应的工作台与操作权限。</p>
        </div>

        <el-form label-position="top" autocomplete="off" @submit.prevent="login">
          <el-form-item label="手机号码 / 管理账号">
            <el-input
              v-model="form.username"
              name="trace_login_phone"
              size="large"
              autocomplete="off"
              placeholder="手机号码或平台管理账号"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              name="trace_login_password"
              size="large"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入登录密码"
              @keyup.enter="login"
            />
          </el-form-item>
          <div class="login-options">
            <el-checkbox>记住账号</el-checkbox>
            <span>身份由企业账号自动识别</span>
          </div>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="login">
            登录系统
          </el-button>
        </el-form>

        <div class="account-links">
          <router-link to="/register">企业首次使用，申请注册</router-link>
          <router-link to="/user/login">普通用户登录</router-link>
          <router-link to="/trace">消费者溯源查询</router-link>
        </div>
      </div>
      <div class="security-note">企业注册需平台管理员审核 · 监管账号由平台管理员创建</div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(430px, 0.9fr);
  background: #f7f0e4;
}
.story-panel {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  background: #2b1a15;
}
.duck-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: settle 1.2s ease-out both;
}
.story-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(24, 12, 9, 0.75), rgba(24, 12, 9, 0.12) 50%, rgba(24, 12, 9, 0.45));
}
.brand {
  position: absolute;
  left: 54px;
  top: 42px;
  display: flex;
  gap: 14px;
  align-items: center;
  color: #fff8ec;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border: 1px solid rgba(255, 231, 193, 0.65);
  border-radius: 50%;
  color: #f1c87f;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 24px;
}
.brand-name {
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 20px;
  letter-spacing: 0.16em;
}
.brand-en {
  margin-top: 3px;
  color: rgba(255, 248, 236, 0.6);
  font-size: 9px;
  letter-spacing: 0.24em;
}
.story-copy {
  position: absolute;
  left: clamp(48px, 7vw, 110px);
  bottom: 17%;
  max-width: 590px;
  color: #fff8ec;
}
.eyebrow {
  color: #e2b96f;
  font-size: 12px;
  letter-spacing: 0.28em;
}
h1 {
  margin: 22px 0 20px;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: clamp(38px, 4.4vw, 66px);
  line-height: 1.25;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.story-copy p {
  max-width: 520px;
  color: rgba(255, 248, 236, 0.72);
  line-height: 1.9;
  font-size: 15px;
}
.chain-line {
  margin-top: 38px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 248, 236, 0.72);
  font-size: 12px;
  letter-spacing: 0.12em;
}
.chain-line i {
  width: 44px;
  height: 1px;
  background: linear-gradient(90deg, #d89b45, rgba(216, 155, 69, 0.15));
}
.tech-note {
  position: absolute;
  left: 54px;
  bottom: 42px;
  display: flex;
  gap: 9px;
  color: rgba(255, 248, 236, 0.58);
  font-size: 12px;
}
.tech-dot {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  border-radius: 50%;
  background: #8eaa8b;
  box-shadow: 0 0 0 5px rgba(142, 170, 139, 0.12);
}
.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px;
}
.login-card {
  width: min(420px, 100%);
  padding: 36px;
  border: 1px solid rgba(106, 46, 36, 0.12);
  border-radius: 20px;
  background: rgba(255, 250, 242, 0.92);
  box-shadow: 0 24px 70px rgba(72, 39, 25, 0.09);
  animation: card-in 0.65s ease-out both;
}
.small-title {
  color: #8b7767;
  font-size: 12px;
  letter-spacing: 0.18em;
}
.login-heading h2 {
  margin: 12px 0 8px;
  color: #2b1a15;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 27px;
}
.login-heading p {
  margin: 0 0 28px;
  color: #8b7767;
  font-size: 13px;
  line-height: 1.7;
}
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -2px 0 20px;
  color: #9a8777;
  font-size: 12px;
}
.login-button {
  width: 100%;
  min-height: 46px;
  letter-spacing: 0.16em;
}
.account-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 14px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid #eadccb;
}
.account-links a {
  color: #8f5034;
  font-size: 11px;
}
.security-note {
  margin-top: 20px;
  color: #a18e7e;
  font-size: 11px;
}
@keyframes settle {
  from { transform: scale(1.04); opacity: 0.55; }
  to { transform: scale(1); opacity: 1; }
}
@keyframes card-in {
  from { transform: translateY(12px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (max-width: 960px) {
  .login-page { grid-template-columns: 1fr; }
  .story-panel { min-height: 38vh; }
  .story-copy { bottom: 40px; }
  .story-copy h1 { font-size: 36px; }
  .story-copy p, .chain-line { display: none; }
  .tech-note { display: none; }
  .login-panel { padding: 28px 18px 42px; }
}
@media (max-width: 560px) {
  .brand { left: 24px; top: 24px; }
  .story-copy { left: 24px; right: 24px; }
  .story-copy h1 { margin: 12px 0; }
  .login-card { padding: 26px 22px; }
}
</style>
