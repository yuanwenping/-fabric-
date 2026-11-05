import { chromium } from 'playwright-core'
import path from 'node:path'

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const runtimeDir = path.resolve(process.cwd(), '..', 'runtime')
const baseUrl = process.env.UI_BASE_URL || 'http://127.0.0.1:5173'
const phone = process.env.EMPLOYEE_TEST_PHONE || '13900008001'
const password = process.env.EMPLOYEE_TEST_PASSWORD || 'Staff12345'
const expectedOrg = process.env.EMPLOYEE_EXPECTED_ORG || 'yuanyuanyuan供应商'
const expectedRole = process.env.EMPLOYEE_EXPECTED_ROLE || '原料供应商操作员'
const screenshotName = process.env.EMPLOYEE_SMOKE_NAME || 'employee-dashboard-smoke.png'
const browser = await chromium.launch({ headless: true, executablePath: edgePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('手机号码或平台管理账号').fill(phone)
  await page.getByPlaceholder('请输入登录密码').fill(password)
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.waitForURL('**/dashboard')
  await page.waitForTimeout(1000)
  const body = await page.locator('body').innerText()
  if (!body.includes(expectedOrg) || !body.includes(expectedRole)) {
    throw new Error('Employee organization or role is not visible')
  }
  await page.screenshot({ path: path.join(runtimeDir, screenshotName), fullPage: true })
  console.log('EMPLOYEE_UI_SMOKE_OK')
} finally {
  await browser.close()
}
