export const eventSchemas = {
  BREEDING_RECORD: {
    label: '养殖过程记录',
    title: '养殖与用药记录',
    fields: [
      { key: 'breedingBatchNo', label: '养殖批次号' },
      { key: 'feedBatchNo', label: '饲料批次号' },
      { key: 'vaccineRecord', label: '疫苗记录编号' },
      { key: 'medicationRecord', label: '用药记录编号' },
      { key: 'withdrawalPeriodEnd', label: '休药期截止日', type: 'date' },
      { key: 'stockQuantity', label: '当前存栏数量' },
      { key: 'healthStatus', label: '健康状态', type: 'select', options: ['正常', '异常'] },
      { key: 'remark', label: '养殖记录说明', type: 'textarea' },
    ],
  },
  QUARANTINE: {
    label: '登记检疫',
    title: '原料检疫记录',
    fields: [
      { key: 'quarantineCertNo', label: '检疫证明编号' },
      { key: 'quarantineResult', label: '检疫结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'quarantineDate', label: '检疫日期', type: 'date' },
      { key: 'issuerOrg', label: '检疫机构' },
    ],
  },
  QUALITY_CHECK: {
    label: '企业质检记录',
    title: '原料内部质量检查记录',
    fields: [
      { key: 'sampleNo', label: '样品编号' },
      { key: 'inspectionType', label: '检查类型', type: 'select', options: ['原料抽检', '养殖环节抽检', '微生物检测'] },
      { key: 'moisture', label: '水分（%）' },
      { key: 'microbialResult', label: '微生物结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'overallResult', label: '总体结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'remark', label: '检查说明', type: 'textarea' },
    ],
  },
  DISPATCH: {
    label: '创建发货交接',
    title: '原料批次发货与承运',
    fields: [
      { key: 'targetOrgId', label: '收货加工企业', type: 'targetOrg' },
      { key: 'carrierOrgId', label: '承运物流企业', type: 'carrierOrg' },
      { key: 'vehicleRef', label: '车辆或运单编号' },
      { key: 'destination', label: '目的地' },
      { key: 'temperature', label: '发货温度（℃）' },
      { key: 'sealNo', label: '封签编号' },
    ],
  },
}

export const requiredFieldsByEvent = {
  BREEDING_RECORD: ['breedingBatchNo', 'feedBatchNo', 'withdrawalPeriodEnd', 'stockQuantity', 'healthStatus'],
  QUARANTINE: ['quarantineCertNo', 'quarantineResult', 'quarantineDate', 'issuerOrg'],
  QUALITY_CHECK: ['sampleNo', 'inspectionType', 'overallResult'],
  DISPATCH: ['targetOrgId', 'vehicleRef', 'destination'],
}

export function eventOptionsFor(orgType, roles = []) {
  if (orgType !== 'SUPPLIER') return []
  const options = []
  if (roles.includes('SUPPLIER_OPERATOR')) options.push('QUARANTINE', 'DISPATCH')
  if (roles.includes('SUPPLIER_BREEDER')) options.push('BREEDING_RECORD')
  if (roles.includes('SUPPLIER_QA')) options.push('QUALITY_CHECK')
  if (roles.includes('SUPPLIER_SHIPPER')) options.push('DISPATCH')
  return [...new Set(options)]
}
