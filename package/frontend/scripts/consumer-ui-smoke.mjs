import { chromium } from 'playwright-core'
import path from 'node:path'

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const runtimeDir = path.resolve(process.cwd(), '..', 'runtime')
const baseUrl = process.env.UI_BASE_URL || 'http://127.0.0.1:5173'
const phone = process.env.CONSUMER_TEST_PHONE || '13900007002'
const password = 'Consumer12345'
const browser = await chromium.launch({ headless: true, executablePath: edgePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })

try {
  await page.goto(`${baseUrl}/user/register`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('请输入11位手机号码').fill(phone)
  const passwordInputs = page.locator('input[type="password"]')
  await passwordInputs.nth(0).fill(password)
  await passwordInputs.nth(1).fill(password)
  await page.getByRole('button', { name: '注册普通用户' }).click()
  await page.getByText('注册成功').waitFor()

  await page.getByRole('button', { name: '前往登录' }).click()
  await page.getByPlaceholder('请输入11位手机号码').fill(phone)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/user')

  const dialog = page.getByRole('dialog', { name: '完善个人信息' })
  await dialog.waitFor()
  await dialog.getByPlaceholder('请输入2到30个字符').fill('普通消费者测试')
  await dialog.getByRole('button', { name: '保存个人信息' }).click()
  await page.getByText('普通消费者测试').first().waitFor()
  await page.screenshot({ path: path.join(runtimeDir, 'consumer-center-smoke.png'), fullPage: true })
  console.log('CONSUMER_UI_SMOKE_OK')
} finally {
  await browser.close()
}
