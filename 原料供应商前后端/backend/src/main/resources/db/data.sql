INSERT INTO organization (id, org_code, org_name, org_type, region_code, msp_id)
VALUES
  (1, 'SUP001', '常德金鸭养殖基地', 'SUPPLIER', '430700', 'Org1MSP'),
  (2, 'PRO001', '常德酱板鸭食品有限公司', 'PROCESSOR', '430700', 'Org1MSP'),
  (3, 'LOG001', '沅水冷链物流有限公司', 'LOGISTICS', '430700', 'Org1MSP'),
  (4, 'RET001', '常德老街特产店', 'RETAILER', '430700', 'Org1MSP'),
  (5, 'REG001', '常德市市场监督管理局', 'REGULATOR', '430700', 'Org2MSP'),
  (6, 'PLATFORM001', '溯源平台管理中心', 'ADMIN', '430700', 'Org1MSP')
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name), org_type = VALUES(org_type);

INSERT INTO sys_role (id, role_code, role_name)
VALUES
  (1, 'SUPPLIER_OPERATOR', '原料供应商操作员'),
  (2, 'PROCESSOR_OPERATOR', '生产加工操作员'),
  (3, 'PROCESSOR_QA', '生产质检员'),
  (4, 'LOGISTICS_OPERATOR', '物流操作员'),
  (5, 'RETAILER_OPERATOR', '销售商操作员'),
  (6, 'REGULATOR', '监管人员'),
  (7, 'PLATFORM_ADMIN', '平台管理员'),
  (8, 'ORG_MANAGER', '企业管理员'),
  (9, 'SUPPLIER_BREEDER', '养殖记录员'),
  (10, 'SUPPLIER_QA', '供应商内部质检员'),
  (11, 'SUPPLIER_SHIPPER', '仓储发货员'),
  (12, 'PROCESSOR_WAREHOUSE', '加工仓储员'),
  (13, 'LOGISTICS_DISPATCHER', '物流调度员'),
  (14, 'LOGISTICS_DRIVER', '运输员'),
  (15, 'RETAIL_RECEIVER', '门店收货员'),
  (16, 'RETAIL_SALESPERSON', '门店销售员'),
  (17, 'REGULATOR_INSPECTOR', '监管抽检员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

UPDATE sys_user SET display_name = '张三' WHERE username = '13910000001';
UPDATE sys_user SET display_name = '李四' WHERE username = '13910000002';
UPDATE sys_user SET display_name = '王五' WHERE username = '13910000003';
UPDATE sys_user SET display_name = '赵六' WHERE username = '13910000004';
UPDATE sys_user SET display_name = '陈七' WHERE username = '13910000005';
UPDATE sys_user SET display_name = '刘洋' WHERE username = '13920000001';
UPDATE sys_user SET display_name = '周强' WHERE username = '13920000002';
UPDATE sys_user SET display_name = '吴敏' WHERE username = '13920000003';
UPDATE sys_user SET display_name = '郑伟' WHERE username = '13920000004';
UPDATE sys_user SET display_name = '孙磊' WHERE username = '13930000001';
UPDATE sys_user SET display_name = '何静' WHERE username = '13930000002';
UPDATE sys_user SET display_name = '林涛' WHERE username = '13930000003';
UPDATE sys_user SET display_name = '徐丽' WHERE username = '13940000001';
UPDATE sys_user SET display_name = '马超' WHERE username = '13940000002';
UPDATE sys_user SET display_name = '朱琳' WHERE username = '13940000003';
UPDATE sys_user SET display_name = '高峰' WHERE username = '13950000001';
UPDATE sys_user SET display_name = '罗敏' WHERE username = '13950000002';
