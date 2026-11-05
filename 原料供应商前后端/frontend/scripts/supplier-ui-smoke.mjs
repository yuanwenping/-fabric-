import { chromium } from 'playwright-core'
import path from 'node:path'

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const runtimeDir = path.resolve(process.cwd(), '..', 'runtime')
const baseUrl = process.env.UI_BASE_URL || 'http://127.0.0.1:15173'
const phone = process.env.SUPPLIER_TEST_PHONE || '13910000001'
const password = process.env.SUPPLIER_TEST_PASSWORD || '12345678'

const browser = await chromium.launch({ headless: true, executablePath: edgePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
const browserErrors = []
page.on('pageerror', (error) => browserErrors.push(error.message))
page.on('console', (message) => {
  if (message.type() === 'error') browserErrors.push(message.text())
})

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('请输入11位手机号码').fill(phone)
  await page.getByPlaceholder('请输入登录密码').fill(password)
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.waitForTimeout(2500)
  if (!page.url().endsWith('/dashboard')) {
    throw new Error(`Login did not navigate to dashboard. URL=${page.url()} BODY=${await page.locator('body').innerText()}`)
  }

  const dashboardText = await page.locator('body').innerText()
  if (!dashboardText.includes('yuanyuanyuan供应商') || !dashboardText.includes('原料供应商管理员')) {
    throw new Error('Supplier organization or role is not visible')
  }
  if (dashboardText.includes('普通用户登录') || dashboardText.includes('企业首次使用')) {
    throw new Error('Non-supplier login entries are still visible')
  }
  await page.screenshot({ path: path.join(runtimeDir, 'supplier-dashboard.png'), fullPage: true })

  await page.goto(`${baseUrl}/batches`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(800)
  const batchText = await page.locator('body').innerText()
  if (!batchText.includes('原料供应商完整闭环验证鸭')) {
    throw new Error('Supplier batch list did not render the verified batch')
  }
  await page.screenshot({ path: path.join(runtimeDir, 'supplier-batches.png'), fullPage: true })

  const firstBatchId = await page.evaluate(async () => {
    const response = await fetch('/api/batches?page=1&size=1', {
      headers: { Authorization: `Bearer ${localStorage.getItem('trace_token')}` },
    })
    const data = await response.json()
    return data.items?.[0]?.id
  })
  if (!firstBatchId) throw new Error('No supplier batch was returned by the API')
  await page.goto(`${baseUrl}/batches/${firstBatchId}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(800)
  const detailText = await page.locator('body').innerText()
  if (!detailText.includes('全链路事件') || !detailText.includes('交接记录')) {
    throw new Error('Supplier batch detail did not render')
  }
  await page.screenshot({ path: path.join(runtimeDir, 'supplier-batch-detail.png'), fullPage: true })

  await page.goto(`${baseUrl}/team`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(700)
  const teamText = await page.locator('body').innerText()
  if (!teamText.includes('养殖记录员') || !teamText.includes('供应商内部质检员')) {
    throw new Error('Supplier employee roles did not render')
  }

  if (browserErrors.length) {
    throw new Error(`Browser errors: ${browserErrors.join(' | ')}`)
  }
  console.log('SUPPLIER_UI_SMOKE_OK')
} finally {
  await browser.close()
}
