const stageNames = {
  SOURCE: '原料登记',
  QUARANTINED: '检疫完成',
  RECEIVED: '已收货',
  CREATED: '生产批次创建',
  SLAUGHTERED: '已屠宰腌制',
  MARINATED: '已腌制',
  SAUCED: '已酱卤',
  ROASTED: '已烘烤杀菌',
  PACKAGED: '已真空包装',
  WAREHOUSED: '已入库',
  DISPATCHED: '已发货',
  IN_TRANSIT: '运输中',
  DELIVERED: '已送达',
  ON_SALE: '销售中',
  SOLD: '已售出',
  FROZEN: '已冻结',
  RECALLED: '已召回',
  RELEASED: '已解冻',
  INSPECTED: '已检验',
}

const fieldNames = {
  temperature: '温度',
  storageTemperature: '存储温度',
  coldChainCompliant: '冷链合规',
  overallResult: '检验结果',
  quarantineResult: '检疫结果',
  decision: '处置决定',
  reportNo: '报告编号',
  vehicleType: '车辆类型',
  result: '结果',
}

export function stageLabel(value) {
  return stageNames[value] || value || '-'
}

export function fieldLabel(value) {
  return fieldNames[value] || value
}
