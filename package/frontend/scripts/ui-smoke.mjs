import { chromium } from 'playwright-core'
import path from 'node:path'

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const runtimeDir = path.resolve(process.cwd(), '..', 'runtime')
const baseUrl = process.env.UI_BASE_URL || 'http://127.0.0.1:5173'
const browser = await chromium.launch({ headless: true, executablePath: edgePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
const browserErrors = []
page.on('pageerror', (error) => browserErrors.push(error.message))
page.on('console', (message) => {
  if (message.type() === 'error') browserErrors.push(message.text())
})
page.on('response', async (response) => {
  if (response.url().includes('/api/')) {
    const body = await response.text().catch(() => '')
    console.log(`API ${response.status()} ${response.request().method()} ${response.url()} ${body.slice(0, 300)}`)
  }
})

try {
  await page.goto(`${baseUrl}/register`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(600)
  const registerText = await page.locator('body').innerText()
  if (!registerText.includes('企业注册申请')) {
    throw new Error('Enterprise registration page did not render')
  }

  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  const accountInput = page.getByPlaceholder('手机号码或平台管理账号')
  const passwordInput = page.getByPlaceholder('请输入登录密码')
  if ((await accountInput.inputValue()) !== '' || (await passwordInput.inputValue()) !== '') {
    throw new Error('Login account and password should not be prefilled')
  }
  await accountInput.fill('platform_admin')
  await passwordInput.fill('Admin@15347361610')
  console.log('INPUTS=' + JSON.stringify(await page.locator('input').evaluateAll((nodes) =>
    nodes.map((node) => ({ placeholder: node.placeholder, value: node.value, type: node.type })))))
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.waitForTimeout(2500)
  if (!page.url().endsWith('/dashboard')) {
    await page.screenshot({ path: path.join(runtimeDir, 'login-smoke-failed.png'), fullPage: true })
    console.error('URL_AFTER_LOGIN=' + page.url())
    console.error('BODY=' + await page.locator('body').innerText())
    console.error('BROWSER_ERRORS=' + JSON.stringify(browserErrors))
    throw new Error('Login did not navigate to dashboard')
  }
  await page.waitForTimeout(1200)
  await page.screenshot({ path: path.join(runtimeDir, 'dashboard-smoke.png'), fullPage: true })

  const body = await page.locator('body').innerText()
  if (!body.includes('溯源平台管理中心')) {
    throw new Error('Dashboard did not render the platform organization')
  }

  await page.goto(`${baseUrl}/admin`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(800)
  const batchText = await page.locator('body').innerText()
  if (!batchText.includes('平台管理')) {
    throw new Error('Admin page did not render')
  }
  await page.screenshot({ path: path.join(runtimeDir, 'admin-smoke.png'), fullPage: true })
  console.log('UI_SMOKE_OK')
} finally {
  await browser.close()
}
