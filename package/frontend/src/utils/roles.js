export const roleLabels = {
  SUPPLIER_OPERATOR: '原料供应商操作员',
  SUPPLIER_BREEDER: '养殖记录员',
  SUPPLIER_QA: '供应商内部质检员',
  SUPPLIER_SHIPPER: '仓储发货员',
  PROCESSOR_OPERATOR: '生产加工操作员',
  PROCESSOR_QA: '生产质检员',
  PROCESSOR_WAREHOUSE: '加工仓储员',
  LOGISTICS_OPERATOR: '物流操作员',
  LOGISTICS_DISPATCHER: '物流调度员',
  LOGISTICS_DRIVER: '运输员',
  RETAILER_OPERATOR: '销售商操作员',
  RETAIL_RECEIVER: '门店收货员',
  RETAIL_SALESPERSON: '门店销售员',
  REGULATOR: '监管人员',
  REGULATOR_INSPECTOR: '监管抽检员',
  ORG_MANAGER: '企业账号管理员',
  PLATFORM_ADMIN: '平台管理员',
}

export const orgTypeLabels = {
  SUPPLIER: '原料供应商',
  PROCESSOR: '生产加工企业',
  LOGISTICS: '物流企业',
  RETAILER: '销售商',
  REGULATOR: '监管部门',
  ADMIN: '平台管理中心',
}

export const managerRoleLabels = {
  SUPPLIER: '原料供应商管理员',
  PROCESSOR: '加工企业管理员',
  LOGISTICS: '物流企业管理员',
  RETAILER: '销售企业管理员',
  REGULATOR: '监管部门管理员',
  ADMIN: '平台管理员',
}

export const roleCodesByOrgType = {
  SUPPLIER: ['ORG_MANAGER', 'SUPPLIER_OPERATOR', 'SUPPLIER_BREEDER', 'SUPPLIER_QA', 'SUPPLIER_SHIPPER'],
  PROCESSOR: ['ORG_MANAGER', 'PROCESSOR_OPERATOR', 'PROCESSOR_QA', 'PROCESSOR_WAREHOUSE'],
  LOGISTICS: ['ORG_MANAGER', 'LOGISTICS_OPERATOR', 'LOGISTICS_DISPATCHER', 'LOGISTICS_DRIVER'],
  RETAILER: ['ORG_MANAGER', 'RETAILER_OPERATOR', 'RETAIL_RECEIVER', 'RETAIL_SALESPERSON'],
  REGULATOR: ['ORG_MANAGER', 'REGULATOR', 'REGULATOR_INSPECTOR'],
  ADMIN: ['PLATFORM_ADMIN'],
}

export function roleName(role, orgType = '') {
  if (role === 'ORG_MANAGER' && managerRoleLabels[orgType]) {
    return managerRoleLabels[orgType]
  }
  return roleLabels[role] || role
}

export function roleNames(roles = [], orgType = '') {
  return roles.map((role) => roleName(role, orgType))
}

export function primaryJobRole(roles = [], orgType = '') {
  if (roles.includes('ORG_MANAGER')) {
    return managerRoleLabels[orgType] || roleLabels.ORG_MANAGER
  }
  const priority = [
    'PLATFORM_ADMIN',
    'REGULATOR',
    'PROCESSOR_QA',
    'PROCESSOR_OPERATOR',
    'PROCESSOR_WAREHOUSE',
    'SUPPLIER_OPERATOR',
    'SUPPLIER_BREEDER',
    'SUPPLIER_QA',
    'SUPPLIER_SHIPPER',
    'LOGISTICS_OPERATOR',
    'LOGISTICS_DISPATCHER',
    'LOGISTICS_DRIVER',
    'RETAILER_OPERATOR',
    'RETAIL_RECEIVER',
    'RETAIL_SALESPERSON',
  ]
  return roleName(priority.find((role) => roles.includes(role)), orgType) || '企业员工'
}
