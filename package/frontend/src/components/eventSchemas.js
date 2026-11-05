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
  DISPATCH: {
    label: '创建发货交接',
    title: '批次发货与承运',
    fields: [
      { key: 'targetOrgId', label: '收货企业', type: 'targetOrg' },
      { key: 'carrierOrgId', label: '承运企业', type: 'carrierOrg' },
      { key: 'vehicleRef', label: '车辆或运单编号' },
      { key: 'destination', label: '目的地' },
      { key: 'temperature', label: '发货温度（℃）' },
      { key: 'sealNo', label: '封签编号' },
    ],
  },
  RECEIVE: {
    label: '确认收货',
    title: '批次收货确认',
    fields: [
      { key: 'quantityReceived', label: '实收数量' },
      { key: 'temperature', label: '到货温度（℃）' },
      { key: 'packageIntact', label: '包装完整', type: 'select', options: ['是', '否'] },
      { key: 'discrepancyCode', label: '差异代码' },
    ],
  },
  SLAUGHTER: {
    label: '屠宰腌制',
    title: '屠宰与腌制记录',
    fields: [
      { key: 'slaughterhouseId', label: '屠宰场编号' },
      { key: 'liveWeight', label: '活重（kg）' },
      { key: 'carcassWeight', label: '胴体重量（kg）' },
      { key: 'preSlaughterQuarantineResult', label: '宰前检疫结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'marinadeStartAt', label: '腌制开始', type: 'datetime' },
      { key: 'marinadeEndAt', label: '腌制结束', type: 'datetime' },
      { key: 'marinadeDurationMin', label: '腌制时长（分钟）' },
      { key: 'brineTemperature', label: '卤水温度（℃）' },
      { key: 'saltBatchNo', label: '食盐批次号' },
    ],
  },
  MARINATE: {
    label: '补充腌制记录',
    title: '腌制过程记录',
    fields: [
      { key: 'startAt', label: '开始时间', type: 'datetime' },
      { key: 'endAt', label: '结束时间', type: 'datetime' },
      { key: 'durationMin', label: '腌制时长（分钟）' },
      { key: 'temperature', label: '温度（℃）' },
      { key: 'saltBatchNo', label: '食盐批次号' },
      { key: 'equipmentId', label: '设备编号' },
      { key: 'sensorFileHash', label: '温度记录文件哈希' },
    ],
  },
  SAUCE: {
    label: '酱卤加工',
    title: '酱卤工序记录',
    fields: [
      { key: 'formulaVersion', label: '配方版本' },
      { key: 'startAt', label: '开始时间', type: 'datetime' },
      { key: 'endAt', label: '结束时间', type: 'datetime' },
      { key: 'durationMin', label: '卤制时长（分钟）' },
      { key: 'setTemperature', label: '设定温度（℃）' },
      { key: 'actualTemperature', label: '实测温度（℃）' },
      { key: 'pressureKPa', label: '压力（kPa）' },
      { key: 'pH', label: 'pH' },
      { key: 'salinity', label: '盐度（%）' },
      { key: 'ccpResult', label: '关键控制点结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'equipmentId', label: '卤制设备编号' },
    ],
  },
  ROAST: {
    label: '烘烤杀菌',
    title: '烘烤与杀菌记录',
    fields: [
      { key: 'processType', label: '工序类型', type: 'select', options: ['烘烤', '杀菌'] },
      { key: 'equipmentId', label: '设备编号' },
      { key: 'startAt', label: '开始时间', type: 'datetime' },
      { key: 'endAt', label: '结束时间', type: 'datetime' },
      { key: 'setTemperature', label: '设定温度（℃）' },
      { key: 'coreTemperature', label: '中心温度（℃）' },
      { key: 'humidity', label: '湿度（%）' },
      { key: 'ccpCompliant', label: '关键控制点达标', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'exceptionCode', label: '异常代码' },
    ],
  },
  QUALITY_CHECK: {
    label: '企业质检记录',
    title: '内部质量检查记录',
    fields: [
      { key: 'sampleNo', label: '样品编号' },
      { key: 'inspectionType', label: '检查类型', type: 'select', options: ['工序抽检', '包装复核', '微生物检测'] },
      { key: 'moisture', label: '水分（%）' },
      { key: 'microbialResult', label: '微生物结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'overallResult', label: '总体结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'remark', label: '检查说明', type: 'textarea' },
    ],
  },
  PACKAGE: {
    label: '真空包装',
    title: '包装与追溯码生成',
    fields: [
      { key: 'productCode', label: '产品编码' },
      { key: 'spec', label: '包装规格' },
      { key: 'netWeight', label: '净含量（g）' },
      { key: 'packageQuantity', label: '包装数量' },
      { key: 'packageMaterialBatchNo', label: '包装材料批次' },
      { key: 'vacuumDegree', label: '真空度' },
      { key: 'sealTemperature', label: '封口温度（℃）' },
      { key: 'productionLineNo', label: '生产线' },
      { key: 'qaResult', label: '包装质检结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'expiryDate', label: '保质期至', type: 'date' },
    ],
  },
  WAREHOUSE_IN: {
    label: '入库',
    title: '仓储入库记录',
    fields: [
      { key: 'warehouseId', label: '仓库编号' },
      { key: 'binNo', label: '库位' },
      { key: 'quantity', label: '入库数量' },
      { key: 'temperature', label: '库温（℃）' },
      { key: 'coldChainStatus', label: '冷链状态', type: 'select', options: ['NORMAL', 'ABNORMAL'] },
    ],
  },
  WAREHOUSE_OUT: {
    label: '出库',
    title: '仓储出库记录',
    fields: [
      { key: 'warehouseId', label: '仓库编号' },
      { key: 'quantity', label: '出库数量' },
      { key: 'temperature', label: '出库温度（℃）' },
      { key: 'outboundOrderNo', label: '出库单号' },
    ],
  },
  TRANSPORT: {
    label: '运输在途',
    title: '冷链运输记录',
    fields: [
      { key: 'vehicleRef', label: '车辆编号' },
      { key: 'sealNo', label: '封签编号' },
      { key: 'fromPlace', label: '始发地' },
      { key: 'toPlace', label: '目的地' },
      { key: 'temperature', label: '运输温度（℃）' },
      { key: 'alarmCount', label: '温度报警次数' },
      { key: 'coldChainCompliant', label: '冷链合规', type: 'select', options: ['PASS', 'FAIL'] },
    ],
  },
  DELIVER: {
    label: '交付送达',
    title: '运输交付记录',
    fields: [
      { key: 'deliveredAt', label: '送达时间', type: 'datetime' },
      { key: 'receiptResult', label: '交付结果', type: 'select', options: ['正常', '异常'] },
      { key: 'temperature', label: '交付温度（℃）' },
      { key: 'discrepancyCode', label: '差异代码' },
    ],
  },
  SALE: {
    label: '终端销售',
    title: '销售出库记录',
    fields: [
      { key: 'quantitySold', label: '销售数量' },
      { key: 'remainingQuantity', label: '剩余库存' },
      { key: 'salesChannel', label: '销售渠道', type: 'select', options: ['门店', '商超', '电商'] },
      { key: 'saleOrderHash', label: '订单哈希' },
      { key: 'storageTemperature', label: '销售温度（℃）' },
    ],
  },
  INSPECTION: {
    label: '监管检验',
    title: '监督抽检与检验结论',
    fields: [
      { key: 'inspectionType', label: '检验类型', type: 'select', options: ['监督抽检', '企业送检', '检疫复核'] },
      { key: 'sampleNo', label: '样品编号' },
      { key: 'overallResult', label: '总体结果', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'reportNo', label: '报告编号' },
      { key: 'decision', label: '处置决定', type: 'select', options: ['RELEASE', 'FREEZE', 'RECALL'] },
      { key: 'indicatorName', label: '检测项目' },
      { key: 'indicatorValue', label: '检测值' },
      { key: 'limitValue', label: '限量值' },
      { key: 'indicatorResult', label: '单项结论', type: 'select', options: ['PASS', 'FAIL'] },
      { key: 'remark', label: '备注', type: 'textarea' },
    ],
  },
  FREEZE: {
    label: '冻结批次',
    title: '监管冻结处置',
    fields: [
      { key: 'reasonCode', label: '冻结原因代码' },
      { key: 'decisionScope', label: '处置范围' },
      { key: 'remark', label: '处置说明', type: 'textarea' },
    ],
  },
  RELEASE: {
    label: '解除冻结',
    title: '监管解冻处置',
    fields: [
      { key: 'reasonCode', label: '解冻原因代码' },
      { key: 'remark', label: '处置说明', type: 'textarea' },
    ],
  },
  RECALL: {
    label: '启动召回',
    title: '批次召回处置',
    fields: [
      { key: 'reasonCode', label: '召回原因代码' },
      { key: 'recallScope', label: '召回范围' },
      { key: 'remark', label: '召回说明', type: 'textarea' },
    ],
  },
}

export const eventOptionsByOrg = {
  SUPPLIER: ['QUARANTINE', 'DISPATCH'],
  PROCESSOR: ['RECEIVE', 'SLAUGHTER', 'MARINATE', 'SAUCE', 'ROAST', 'PACKAGE', 'WAREHOUSE_IN', 'WAREHOUSE_OUT', 'DISPATCH'],
  LOGISTICS: ['TRANSPORT', 'DELIVER'],
  RETAILER: ['RECEIVE', 'WAREHOUSE_IN', 'WAREHOUSE_OUT', 'SALE'],
  REGULATOR: ['INSPECTION', 'FREEZE', 'RELEASE', 'RECALL'],
}

export const requiredFieldsByEvent = {
  BREEDING_RECORD: ['breedingBatchNo', 'feedBatchNo', 'withdrawalPeriodEnd', 'stockQuantity', 'healthStatus'],
  QUARANTINE: ['quarantineCertNo', 'quarantineResult', 'quarantineDate', 'issuerOrg'],
  DISPATCH: ['targetOrgId', 'vehicleRef', 'destination'],
  RECEIVE: ['quantityReceived', 'packageIntact'],
  SLAUGHTER: ['slaughterhouseId', 'liveWeight', 'carcassWeight', 'preSlaughterQuarantineResult'],
  MARINATE: ['startAt', 'endAt', 'durationMin', 'temperature'],
  SAUCE: ['formulaVersion', 'startAt', 'endAt', 'setTemperature', 'actualTemperature', 'ccpResult'],
  ROAST: ['processType', 'equipmentId', 'startAt', 'endAt', 'coreTemperature', 'ccpCompliant'],
  QUALITY_CHECK: ['sampleNo', 'inspectionType', 'overallResult'],
  PACKAGE: ['productCode', 'spec', 'netWeight', 'packageQuantity', 'qaResult', 'expiryDate'],
  WAREHOUSE_IN: ['warehouseId', 'binNo', 'quantity', 'coldChainStatus'],
  WAREHOUSE_OUT: ['warehouseId', 'quantity', 'outboundOrderNo'],
  TRANSPORT: ['vehicleRef', 'fromPlace', 'toPlace', 'temperature', 'coldChainCompliant'],
  DELIVER: ['deliveredAt', 'receiptResult', 'temperature'],
  SALE: ['quantitySold', 'salesChannel'],
  INSPECTION: ['inspectionType', 'sampleNo', 'overallResult', 'reportNo', 'decision', 'indicatorName', 'indicatorValue', 'indicatorResult'],
  FREEZE: ['reasonCode', 'decisionScope'],
  RELEASE: ['reasonCode'],
  RECALL: ['reasonCode', 'recallScope'],
}

export function eventOptionsFor(orgType, roles = []) {
  if (orgType === 'SUPPLIER') {
    const options = []
    if (roles.includes('SUPPLIER_OPERATOR')) options.push('QUARANTINE', 'DISPATCH')
    if (roles.includes('SUPPLIER_BREEDER')) options.push('BREEDING_RECORD')
    if (roles.includes('SUPPLIER_QA')) options.push('QUALITY_CHECK')
    if (roles.includes('SUPPLIER_SHIPPER')) options.push('DISPATCH')
    return [...new Set(options)]
  }
  if (orgType === 'PROCESSOR') {
    const options = []
    if (roles.includes('PROCESSOR_OPERATOR')) {
      options.push(
        'RECEIVE',
        'SLAUGHTER',
        'MARINATE',
        'SAUCE',
        'ROAST',
        'PACKAGE',
        'WAREHOUSE_IN',
        'WAREHOUSE_OUT',
        'DISPATCH',
      )
    }
    if (roles.includes('PROCESSOR_QA')) {
      options.push('QUALITY_CHECK')
    }
    if (roles.includes('PROCESSOR_WAREHOUSE')) {
      options.push('WAREHOUSE_IN', 'WAREHOUSE_OUT')
    }
    return [...new Set(options)]
  }
  if (orgType === 'LOGISTICS') {
    const options = []
    if (roles.includes('LOGISTICS_DISPATCHER')) options.push('TRANSPORT')
    if (roles.includes('LOGISTICS_OPERATOR')) options.push('TRANSPORT', 'DELIVER')
    if (roles.includes('LOGISTICS_OPERATOR') || roles.includes('LOGISTICS_DRIVER')) options.push('TRANSPORT', 'DELIVER')
    return [...new Set(options)]
  }
  if (orgType === 'RETAILER') {
    const options = []
    if (roles.includes('RETAILER_OPERATOR') || roles.includes('RETAIL_RECEIVER')) options.push('RECEIVE', 'WAREHOUSE_IN')
    if (roles.includes('RETAILER_OPERATOR') || roles.includes('RETAIL_SALESPERSON')) options.push('WAREHOUSE_OUT', 'SALE')
    return [...new Set(options)]
  }
  if (orgType === 'REGULATOR') {
    return roles.some((role) => ['REGULATOR', 'REGULATOR_INSPECTOR'].includes(role))
      ? ['INSPECTION', 'FREEZE', 'RELEASE', 'RECALL']
      : []
  }
  return eventOptionsByOrg[orgType] || []
}
