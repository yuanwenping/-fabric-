<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useConsumerAuthStore } from '../stores/consumerAuth'
import duckImage from '../assets/changde-duck.svg'

const route = useRoute()
const router = useRouter()
const auth = useConsumerAuthStore()
const loading = ref(false)
const registrationComplete = ref(false)
const loginForm = reactive({ phone: '', password: '' })
const registerForm = reactive({ phone: '', password: '', confirmPassword: '' })
const isRegister = computed(() => route.path === '/user/register')
watch(() => route.path, () => {
  registrationComplete.value = false
  loginForm.password = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
})

async function login() {
  loading.value = true
  try {
    await auth.login(loginForm.phone, loginForm.password)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/user')
  } finally {
    loading.value = false
  }
}

async function register() {
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await http.post('/consumer/register', {
      phone: registerForm.phone,
      password: registerForm.password,
    })
    registrationComplete.value = true
    loginForm.phone = registerForm.phone
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="consumer-auth-page">
    <section class="visual-side">
      <img :src="duckImage" alt="常德酱板鸭" />
      <div class="shade" />
      <router-link to="/trace" class="consumer-brand">
        <span>溯</span>
        <div><strong>常德酱板鸭</strong><small>消费者溯源中心</small></div>
      </router-link>
      <div class="visual-copy">
        <span>FROM SOURCE TO TABLE</span>
        <h1>看得见的来路，<br />吃得下的安心。</h1>
        <p>登录后可保存查询记录和收藏产品，每一次查询仍然对游客开放。</p>
      </div>
    </section>

    <section class="form-side">
      <div class="auth-card">
        <div class="portal-links">
          <router-link to="/user/login">普通用户登录</router-link>
          <router-link to="/user/register">注册账号</router-link>
        </div>

        <template v-if="!registrationComplete">
          <div v-if="!isRegister">
            <div class="eyebrow">CONSUMER LOGIN</div>
            <h2>欢迎回来</h2>
            <p>使用手机号码登录消费者溯源中心。</p>
            <el-form label-position="top" autocomplete="off" @submit.prevent="login">
              <el-form-item label="手机号码">
                <el-input
                  v-model="loginForm.phone"
                  name="consumer_login_phone"
                  size="large"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="off"
                  placeholder="请输入11位手机号码"
                />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="loginForm.password"
                  name="consumer_login_password"
                  size="large"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请输入密码"
                  @keyup.enter="login"
                />
              </el-form-item>
              <el-button type="primary" size="large" class="action-button" :loading="loading" @click="login">
                登录
              </el-button>
            </el-form>
            <div class="helper">
              还没有账号？<router-link to="/user/register">立即注册</router-link>
            </div>
          </div>

          <div v-else>
            <div class="eyebrow">CREATE ACCOUNT</div>
            <h2>注册普通用户</h2>
            <p>首次登录后需要补充昵称等基本资料。</p>
            <el-form label-position="top" autocomplete="off">
              <el-form-item label="手机号码">
                <el-input
                  v-model="registerForm.phone"
                  name="consumer_register_phone"
                  size="large"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="off"
                  placeholder="请输入11位手机号码"
                />
              </el-form-item>
              <el-form-item label="设置密码">
                <el-input
                  v-model="registerForm.password"
                  size="large"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="至少8位"
                />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input
                  v-model="registerForm.confirmPassword"
                  size="large"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  @keyup.enter="register"
                />
              </el-form-item>
              <el-button type="primary" size="large" class="action-button" :loading="loading" @click="register">
                注册普通用户
              </el-button>
            </el-form>
            <div class="helper">
              已有账号？<router-link to="/user/login">返回登录</router-link>
            </div>
          </div>
        </template>

        <div v-else class="success">
          <div class="success-mark">✓</div>
          <h2>注册成功</h2>
          <p>现在登录并完成首次个人信息设置。</p>
          <el-button type="primary" @click="router.push('/user/login')">前往登录</el-button>
        </div>

        <div class="bottom-links">
          <router-link to="/login">企业人员登录</router-link>
          <router-link to="/trace">游客直接查溯源</router-link>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.consumer-auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.22fr) minmax(430px, .78fr);
  background: #f7f0e4;
}
.visual-side {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #2b1a15;
}
.visual-side img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(24, 12, 9, .68), rgba(24, 12, 9, .15) 58%, rgba(24, 12, 9, .45));
}
.consumer-brand {
  position: absolute;
  top: 38px;
  left: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff8ec;
}
.consumer-brand > span {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid #d8b071;
  border-radius: 50%;
  color: #efc77d;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 22px;
}
.consumer-brand strong,
.consumer-brand small {
  display: block;
}
.consumer-brand strong {
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.consumer-brand small {
  margin-top: 4px;
  color: rgba(255, 248, 236, .56);
  font-size: 10px;
}
.visual-copy {
  position: absolute;
  left: 8%;
  bottom: 16%;
  color: #fff8ec;
}
.visual-copy > span {
  color: #e0b66a;
  font-size: 10px;
  letter-spacing: .24em;
}
.visual-copy h1 {
  margin: 18px 0;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: clamp(38px, 4.6vw, 62px);
  line-height: 1.3;
}
.visual-copy p {
  max-width: 480px;
  color: rgba(255, 248, 236, .66);
  line-height: 1.8;
}
.form-side {
  display: grid;
  place-items: center;
  padding: 36px;
}
.auth-card {
  width: min(420px, 100%);
  padding: 32px;
  border: 1px solid #e5d5c2;
  border-radius: 20px;
  background: rgba(255, 250, 242, .96);
  box-shadow: 0 24px 65px rgba(70, 37, 24, .09);
}
.portal-links {
  display: flex;
  gap: 8px;
  margin-bottom: 30px;
}
.portal-links a {
  padding: 7px 12px;
  border-radius: 999px;
  color: #8f7867;
  font-size: 11px;
}
.portal-links a.router-link-active {
  color: #fff8ed;
  background: #6a2e24;
}
.eyebrow {
  color: #9b7955;
  font-size: 10px;
  letter-spacing: .18em;
}
h2 {
  margin: 10px 0 8px;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 28px;
}
.auth-card > div > p,
.auth-card > p {
  margin: 0 0 22px;
  color: #8b7666;
  font-size: 12px;
}
.action-button {
  width: 100%;
}
.helper {
  margin-top: 18px;
  color: #8d7867;
  text-align: center;
  font-size: 11px;
}
.helper a {
  color: #8b4a33;
}
.bottom-links {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid #eadccb;
  font-size: 11px;
}
.bottom-links a {
  color: #8d6b53;
}
.success {
  min-height: 350px;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}
.success-mark {
  display: grid;
  place-items: center;
  width: 66px;
  height: 66px;
  margin-bottom: 15px;
  border: 2px solid #456756;
  border-radius: 50%;
  color: #456756;
  font-size: 28px;
}
@media (max-width: 960px) {
  .consumer-auth-page { grid-template-columns: 1fr; }
  .visual-side { min-height: 38vh; }
  .visual-copy { bottom: 36px; left: 30px; }
  .visual-copy h1 { font-size: 34px; }
  .visual-copy p { display: none; }
  .form-side { padding: 25px 16px 40px; }
}
</style>
