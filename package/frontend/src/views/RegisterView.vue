<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const router = useRouter()
const submitting = ref(false)
const submitted = ref(false)
const generatedOrgCode = ref('')
const form = reactive({
  orgName: '',
  orgCode: '',
  orgType: 'SUPPLIER',
  regionCode: '430700',
  displayName: '',
  phone: '',
  password: '',
  confirmPassword: '',
})
const prefix = computed(() => ({
  SUPPLIER: 'SUP',
  PROCESSOR: 'PRO',
  LOGISTICS: 'LOG',
  RETAILER: 'RET',
}[form.orgType] || 'ORG'))

async function submit() {
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (form.password.length < 8) {
    ElMessage.warning('密码至少需要 8 位')
    return
  }
  submitting.value = true
  try {
    const result = await http.post('/auth/register', {
      orgName: form.orgName,
      orgCode: form.orgCode ? form.orgCode.toUpperCase() : null,
      orgType: form.orgType,
      regionCode: form.regionCode,
      displayName: form.displayName,
      phone: form.phone,
      password: form.password,
    })
    generatedOrgCode.value = result.orgCode
    submitted.value = true
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <header>
      <router-link to="/login" class="brand-link">
        <span class="seal">溯</span>
        <div><strong>常德酱板鸭</strong><small>全链条可信溯源</small></div>
      </router-link>
      <router-link to="/login" class="back">返回登录</router-link>
    </header>

    <section class="register-wrap">
      <div class="intro">
        <span>ENTERPRISE ONBOARDING</span>
        <h1>加入可信溯源链</h1>
        <p>供应商、生产加工企业、物流企业和销售商可提交入驻申请。平台管理员审核通过后，账号即可登录并参与批次协同。</p>
        <ul>
          <li>企业独立账号与数据范围</li>
          <li>关键业务事件写入 Fabric 账本</li>
          <li>企业主账号可继续添加本企业员工</li>
        </ul>
        <div class="rule">
          监管部门和平台管理账号不允许自助注册，只能由平台管理员创建。
        </div>
      </div>

      <el-card class="register-card">
        <template v-if="!submitted">
          <div class="card-head">
            <h2>企业注册申请</h2>
            <p>提交后需要平台管理员审核，审核期间不能登录。</p>
          </div>
          <el-form :model="form" label-position="top">
            <div class="form-grid">
              <el-form-item label="企业名称" required>
                <el-input v-model="form.orgName" placeholder="营业执照上的企业名称" />
              </el-form-item>
              <el-form-item label="企业编号后缀">
                <el-input v-model="form.orgCode" placeholder="留空自动生成">
                  <template #prepend>{{ prefix }}-</template>
                </el-input>
                <div class="field-hint">只需填写唯一后缀；留空时系统自动生成完整企业编号。</div>
              </el-form-item>
              <el-form-item label="企业类型" required>
                <el-select v-model="form.orgType" style="width: 100%">
                  <el-option value="SUPPLIER" label="原料供应商" />
                  <el-option value="PROCESSOR" label="生产加工企业" />
                  <el-option value="LOGISTICS" label="物流企业" />
                  <el-option value="RETAILER" label="销售商" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属地区代码">
                <el-input v-model="form.regionCode" placeholder="例如：430700" />
              </el-form-item>
              <el-form-item label="联系人姓名" required>
                <el-input v-model="form.displayName" />
              </el-form-item>
              <el-form-item label="手机号码（登录账号）" required>
                <el-input
                  v-model="form.phone"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="off"
                  placeholder="请输入11位手机号码"
                />
              </el-form-item>
              <el-form-item label="登录密码" required>
                <el-input v-model="form.password" type="password" show-password placeholder="至少8位" />
              </el-form-item>
              <el-form-item label="确认密码" required>
                <el-input v-model="form.confirmPassword" type="password" show-password />
              </el-form-item>
            </div>
            <el-button type="primary" class="submit-button" :loading="submitting" @click="submit">
              提交注册申请
            </el-button>
          </el-form>
        </template>

        <div v-else class="success-panel">
          <div class="success-mark">✓</div>
          <h2>申请已提交</h2>
          <p>企业编号为 <strong>{{ generatedOrgCode }}</strong>。平台管理员审核通过后，你可以使用手机号码登录。</p>
          <el-button type="primary" @click="router.push('/login')">返回登录页</el-button>
        </div>
      </el-card>
    </section>
  </main>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 18%, rgba(194, 123, 54, 0.18), transparent 24rem),
    #f7f0e4;
}
header {
  height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(22px, 5vw, 72px);
  border-bottom: 1px solid #e4d4c2;
}
.brand-link {
  display: flex;
  align-items: center;
  gap: 12px;
}
.seal {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #f5cf89;
  background: #6a2e24;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 20px;
}
.brand-link strong,
.brand-link small {
  display: block;
}
.brand-link strong {
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.brand-link small {
  margin-top: 3px;
  color: #967f6e;
  font-size: 10px;
}
.back {
  color: #7b5b47;
  font-size: 12px;
}
.register-wrap {
  width: min(1120px, calc(100% - 36px));
  margin: 56px auto;
  display: grid;
  grid-template-columns: minmax(0, 0.78fr) minmax(520px, 1.22fr);
  gap: 44px;
  align-items: start;
}
.intro {
  padding: 32px 10px;
}
.intro > span {
  color: #9a6e43;
  font-size: 10px;
  letter-spacing: 0.24em;
}
.intro h1 {
  margin: 18px 0;
  font-family: "Source Han Serif SC", "SimSun", serif;
  font-size: 43px;
}
.intro p {
  color: #705b4c;
  line-height: 1.9;
  font-size: 14px;
}
.intro ul {
  margin: 28px 0;
  padding-left: 20px;
  color: #5e493d;
  line-height: 2.2;
  font-size: 13px;
}
.rule {
  padding: 14px 16px;
  border-left: 3px solid #b67a42;
  color: #806651;
  background: rgba(255, 250, 242, 0.7);
  line-height: 1.7;
  font-size: 12px;
}
.register-card {
  padding: 8px;
}
.card-head h2,
.success-panel h2 {
  margin: 4px 0 8px;
  font-family: "Source Han Serif SC", "SimSun", serif;
}
.card-head p,
.success-panel p {
  margin: 0 0 22px;
  color: #8c7767;
  font-size: 12px;
  line-height: 1.7;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.field-hint {
  margin-top: 5px;
  color: #947e6d;
  font-size: 10px;
}
.submit-button {
  width: 100%;
  margin-top: 6px;
}
.success-panel {
  min-height: 440px;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}
.success-mark {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  margin-bottom: 18px;
  border: 2px solid #456756;
  border-radius: 50%;
  color: #456756;
  font-size: 30px;
}
@media (max-width: 960px) {
  .register-wrap { grid-template-columns: 1fr; margin-top: 28px; }
  .intro { padding: 8px; }
  .intro h1 { font-size: 34px; }
}
@media (max-width: 620px) {
  .form-grid { grid-template-columns: 1fr; }
  .register-wrap { width: calc(100% - 24px); }
}
</style>
