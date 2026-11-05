# 常德酱板鸭全链条可信溯源系统

本项目将原料采购、屠宰腌制、酱卤、烘烤杀菌、真空包装、仓储、物流、销售、监管检验和消费者查询串成一条可核验的业务链。结构化业务数据保存在 MySQL，关键事件哈希与批次状态写入 Hyperledger Fabric。

## 目录

```text
pakeage/
  backend/      Spring Boot 3 + Spring Security + Fabric Java SDK
  frontend/     Vue 3 + Vite + Element Plus
  chaincode/    Go 链码 tracecc
  database/     可手动导入的建表与基础数据
  scripts/      Windows 启动和构建脚本
  runtime/      预留的本地文件目录
```

## 账号注册与审核

系统不再提供业务演示账号。旧演示账号已停用并改名为 `legacy_*` 历史审计账号，以保留原有链上事件的操作人外键。

首次启动会创建平台管理员：

```text
账号：platform_admin
初始密码：Admin@15347361610
```

平台管理员登录后可以：

- 审核供应商、加工企业、物流企业和销售商的注册申请。
- 为现有企业创建业务账号。
- 创建监管部门账号。
- 启用或停用账号、重置密码。

企业自助注册后为 `PENDING` 状态，审核通过前不能登录。审核通过后，企业主账号会获得 `ORG_MANAGER` 角色，并在“员工账号”页面添加本企业的管理员和岗位员工。

员工账号登录后，界面会明确显示“所属企业 + 岗位身份”，并只开放该岗位允许的业务操作：

- 原料供应商：管理员、操作员、养殖记录员、内部质检员、仓储发货员。
- 加工企业：管理员、生产操作员、生产质检员、加工仓储员。
- 物流企业：管理员、调度员、运输员。
- 销售商：管理员、门店收货员、门店销售员。
- 监管部门：管理员、监管人员、监管抽检员。

监管部门、平台管理账号不能自助注册，只能由平台管理员创建。管理员与所有用户都可以在左下角“修改密码”中修改自己的密码。

企业员工统一使用中国大陆 11 位手机号码作为登录账号。平台管理员账号 `platform_admin` 是保留的特殊管理账号。

### 已创建岗位员工账号

以下账号密码统一为 `12345678`。管理员账号会自动附加本企业的基础业务角色，例如原料供应商管理员同时拥有 `ORG_MANAGER` 和 `SUPPLIER_OPERATOR`。

| 企业编号 | 登录手机号 | 姓名 | 岗位 |
|---|---|---|---|
| SUP003 | 13910000001 | 张三 | 原料供应商管理员 |
| SUP003 | 13910000002 | 李四 | 养殖记录员 |
| SUP003 | 13910000003 | 王五 | 供应商内部质检员 |
| SUP003 | 13910000004 | 赵六 | 仓储发货员 |
| SUP003 | 13910000005 | 陈七 | 原料供应商操作员 |
| PRO001 | 13920000001 | 刘洋 | 加工企业管理员 |
| PRO001 | 13920000002 | 周强 | 生产加工操作员 |
| PRO001 | 13920000003 | 吴敏 | 生产质检员 |
| PRO001 | 13920000004 | 郑伟 | 加工仓储员 |
| LOG001 | 13930000001 | 孙磊 | 物流企业管理员 |
| LOG001 | 13930000002 | 何静 | 物流调度员 |
| LOG001 | 13930000003 | 林涛 | 运输员 |
| RET001 | 13940000001 | 徐丽 | 销售企业管理员 |
| RET001 | 13940000002 | 马超 | 门店收货员 |
| RET001 | 13940000003 | 朱琳 | 门店销售员 |
| REG001 | 13950000001 | 高峰 | 监管部门管理员 |
| REG001 | 13950000002 | 罗敏 | 监管抽检员 |

如需在空数据库中重新创建这批账号，先启动后端，再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-employee-accounts.ps1 -BaseUrl http://127.0.0.1:8080
```

企业编号会根据组织类型自动补充前缀：

| 企业类型 | 编号前缀 |
|---|---|
| 原料供应商 | `SUP-` |
| 生产加工企业 | `PRO-` |
| 物流企业 | `LOG-` |
| 销售商 | `RET-` |
| 监管部门 | `REG-` |
| 平台管理 | `PLATFORM-` |

注册时企业编号后缀可以自行填写，也可以留空。留空会自动生成类似 `SUP-20261101-A1B2` 的唯一编号。

## 普通消费者

普通消费者使用与企业用户分开的账号体系：

- 使用11位手机号码注册和登录。
- 第一次进入消费者中心时必须完善昵称、性别和地区信息。
- 可以查看产品公开溯源信息。
- 可以保存最近查询记录和收藏产品。
- 可以修改自己的登录密码。
- 游客仍然可以不登录直接查询追溯码。

普通用户入口：

```text
http://127.0.0.1:5173/user/login
```

企业用户入口：

```text
http://127.0.0.1:5173/login
```

当前版本未接入短信验证码服务，注册时通过本地手机号格式校验和密码校验完成注册。接入短信平台后可以在注册接口前增加短信验证码校验。

## 一键启动

在 Windows PowerShell 中执行：

```powershell
cd E:\多机部署\pakeage
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

脚本会启动 Fabric 虚拟机、Fabric 容器、项目 MySQL、Spring Boot 和 Vue 开发服务器，并打开登录页。停止应用进程：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
```

## 分别启动

虚拟机 `yanyuanyuanfabric` 上的四组织 Fabric 网络应先启动：

```bash
cd /home/yuan/fabric-four-peers
docker-compose -p trace4 -f docker/docker-compose.yaml up -d
```

Windows 上分别打开两个 PowerShell：

```powershell
cd E:\多机部署\pakeage
powershell -ExecutionPolicy Bypass -File .\scripts\start-mysql.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

```powershell
cd E:\多机部署\pakeage
powershell -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
```

访问 `http://localhost:5173`。

项目数据库使用 Windows 本机 `MySQL80` 服务，连接地址为 `127.0.0.1:3306`，数据库名为 `changde_trace_multipeer`，与独立原料供应商节点共用业务数据。

四组织身份对应关系：

| 企业类型 | Fabric MSP | Peer |
|---|---|---|
| 原料供应商 | `SupplierMSP` | `peer0.supplier.example.com:7051` |
| 加工企业 | `ProcessorMSP` | `peer0.processor.example.com:8051` |
| 物流企业 | `LogisticsMSP` | `peer0.logistics.example.com:9051` |
| 销售商 | `RetailerMSP` | `peer0.retailer.example.com:10051` |

## 核心业务流

1. 供应商管理员或操作员创建 `RAW_DUCK` 原料批次；岗位员工登记养殖、质检、检疫或发货信息。
2. 员工提交的业务先保存为 `PENDING_REVIEW`，此时不写区块链、不推进批次、不创建下游任务。
3. 本企业管理员在“业务审核”中查看原始填报；通过后才提交 Fabric，成功推进批次并生成交接任务，驳回时必须填写原因。
4. 上链失败会标记为 `CHAIN_FAILED`，管理员可重试；此时批次仍停留在审核前状态。
5. 供应商选择加工企业发货。
6. 加工企业确认收货，以原料批次创建加工子批次，依次登记屠宰腌制、酱卤、烘烤杀菌和真空包装。
7. 包装事件自动生成消费者追溯码。
8. 加工企业创建物流和销售交接；物流登记调度、运输与送达，销售商确认收货。
9. 销售商登记终端销售，消费者通过追溯码查看公开摘要。
10. 监管部门账号可以登录、查看全辖批次和填写监管业务。当前四组织 Fabric 网络未部署 `RegulatorMSP`，因此监管事件上链需要下一步扩展 Fabric 组织与背书策略。

## 上链设计

- `CreateBatch`：创建批次起点，记录批次 ID、批次关系、所属企业及初始哈希。
- `AppendEvent`：追加业务事件，校验 `prevEventId`、`prevHash`、事件重复和提交组织权限。
- `GetTrace` / `GetEvent`：查询链上批次与事件。
- `VerifyEvent`：对比业务内容哈希，定位链下数据是否被篡改。
- 图片、视频和报告保存在 `backend/uploads`，链上只保存 SHA-256 证据根。

业务事件采用“先审核、后上链”：员工提交后 MySQL 保存为 `PENDING_REVIEW`；管理员通过后再提交 Fabric，成功后更新为 `COMMITTED`。未审核数据不会进入账本，上链失败会保留错误信息和重试入口。

### 岗位填报内容隔离

员工打开业务登记窗口时，可选择的业务类型由后端 `GET /api/events/allowed` 根据当前账号岗位返回。例如：

- 养殖记录员只能选择“养殖过程记录”，填写养殖批次、饲料批次、疫苗、用药、休药期、存栏和健康状态等内容。
- 供应商质检员只能选择“企业质检记录”，不能提交养殖、检疫或发货内容。
- 仓储发货员只能选择“创建发货交接”，不能提交养殖或质检内容。
- 加工仓储员只能选择“入库”和“出库”，不能提交生产工序或销售内容。
- 门店收货员只能选择“确认收货”和“入库”，门店销售员只能选择“出库”和“终端销售”。

后端还会对每条业务执行字段白名单和必填项校验。即使绕过前端直接调用接口，只要业务类型不属于当前岗位、夹带其他业务的字段或缺少必填内容，请求都会被拒绝。

## 链码重新部署

链码位于 `chaincode/tracecc`。修改后重新上传：

四组织网络、证书与链码的部署入口位于相邻目录 `fabric-four-peers`。不要使用旧的 `fabric-samples/test-network` 命令覆盖当前账本。

`backend/src/main/resources/fabric` 中包含四组织客户端证书，仅适用于本机演示，不应提交到公开仓库。
