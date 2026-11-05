export const roleLabels = {
  SUPPLIER_OPERATOR: '原料供应商操作员',
  SUPPLIER_BREEDER: '养殖记录员',
  SUPPLIER_QA: '供应商内部质检员',
  SUPPLIER_SHIPPER: '仓储发货员',
  ORG_MANAGER: '企业账号管理员',
}

export const orgTypeLabels = {
  SUPPLIER: '原料供应商',
}

export const managerRoleLabels = {
  SUPPLIER: '原料供应商管理员',
}

export const roleCodesByOrgType = {
  SUPPLIER: ['ORG_MANAGER', 'SUPPLIER_OPERATOR', 'SUPPLIER_BREEDER', 'SUPPLIER_QA', 'SUPPLIER_SHIPPER'],
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
    'SUPPLIER_OPERATOR',
    'SUPPLIER_BREEDER',
    'SUPPLIER_QA',
    'SUPPLIER_SHIPPER',
  ]
  return roleName(priority.find((role) => roles.includes(role)), orgType) || '企业员工'
}
