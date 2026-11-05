# 原料供应商独立溯源节点

本项目从 `pakeage` 全链路系统中拆出原料供应商业务，前后端和部署包均独立维护。节点只允许 `SUP003` 企业员工登录，并使用 `SupplierMSP` 身份访问联盟链。

## 网络拓扑

| 角色 | 地址 | 说明 |
|---|---|---|
| 原料供应商虚拟机 | `192.168.116.145` | Spring Boot、静态前端和 API 网关 |
| Fabric 虚拟机 | `192.168.116.144` | Orderer、Supplier/Processor/Logistics/Retailer 四个 Peer |
| Windows MySQL | `192.168.116.1:3306` | 共享数据库 `changde_trace_multipeer` |
| 供应商 Peer | `192.168.116.144:7051` | `peer0.supplier.example.com`、`SupplierMSP` |
| Orderer | `192.168.116.144:7050` | `orderer.example.com` |

虚拟机登录用户为 `yuan`。供应商节点访问地址：

```text
http://192.168.116.145:5173
```

## 已实现业务

- `SUP003` 企业管理员和岗位员工登录，其他企业账号即使密码正确也会被拒绝。
- 原料供应商管理员或操作员创建 `RAW_DUCK` 原料鸭批次，并写入 Fabric 批次起点。
- 养殖记录员提交养殖、饲料、疫苗、用药、休药期和存栏记录。
- 税疫岗位提交检疫证明与检疫结论。
- 供应商质检员提交内部质量检查记录。
- 仓储发货员创建到加工企业的发货交接，可指定物流企业承运。
- 现场图片、视频、检验报告保存到 VMware 本地文件目录，链上保存 SHA-256 证据根。
- 员工提交后进入 `PENDING_REVIEW`，企业管理员审核通过才上链；可填写原因驳回。
- 上链失败标记 `CHAIN_FAILED`，管理员可在批次详情中重试。
- 同一批次可连续提交多条待审核记录。审核时会按批次最新链上状态重定位前序事件并重算哈希，避免并发待审核记录产生旧哈希冲突。
- 批次列表、详情、交接记录、岗位提交记录、企业员工账号、链上哈希核验和链上连接状态均可用。

## 目录

```text
原料供应商前后端/
  backend/
    src/                    Spring Boot 源码
    certs/                  SupplierMSP 客户端与 TLS 证书
    target/                 Maven 构建目录
    trace-backend-1.0.0.jar 部署 JAR
  frontend/
    src/                    Vue 3 源码
    dist/                   生产构建
    gateway.py              静态文件与 /api 反向代理
  config/supplier.env       运行参数
  scripts/
    build.ps1               Windows 构建
    deploy.ps1              构建并上传到供应商虚拟机
    start-*.sh              虚拟机手动启动
    install-service.sh      安装或重载 systemd 服务
  systemd/                  两个 systemd 单元
```

## 构建

Windows PowerShell：

```powershell
cd E:\多机部署\原料供应商前后端
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

构建产物：

```text
backend/trace-backend-1.0.0.jar
frontend/dist/
```

## 部署

`deploy.ps1` 会构建并上传 JAR、前端、证书、配置和 systemd 单元，然后重启服务。脚本通过 SSH 交互输入 `yuan` 的密码：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1
```

部署后检查：

```bash
sudo systemctl status supplier-trace-backend --no-pager
sudo systemctl status supplier-trace-frontend --no-pager
journalctl -u supplier-trace-backend -n 100 --no-pager
```

手动启动：

```bash
cd /home/yuan/supplier-trace
SUDO_PASSWORD=123456 scripts/install-service.sh
```

## 数据库

应用读取共享库，不执行建表或初始化数据：

```text
jdbc:mysql://192.168.116.1:3306/changde_trace_multipeer
账号：supplier_app
限制来源：192.168.116.145
```

数据库参数在 `config/supplier.env` 中维护。启动前必须保证 Windows MySQL 允许虚拟网卡地址访问，并且供应商虚拟机可连接 `192.168.116.1:3306`。

## 供应商测试账号

以下账号属于 `SUP003`，密码均为 `12345678`：

| 手机号 | 姓名 | 岗位 |
|---|---|---|
| `13910000001` | 张三 | 企业管理员、原料供应商操作员 |
| `13910000002` | 李四 | 养殖记录员 |
| `13910000003` | 王五 | 供应商内部质检员 |
| `13910000004` | 赵六 | 仓储发货员 |
| `13910000005` | 陈七 | 原料供应商操作员 |

管理员不能审核本人提交的事件，因此推荐由岗位员工填报、管理员审核。

## 配置

关键配置位于 `config/supplier.env`：

| 变量 | 当前值 |
|---|---|
| `NODE_ORG_CODE` | `SUP003` |
| `DB_URL` | Windows 共享 MySQL |
| `FABRIC_MSP_ID` | `SupplierMSP` |
| `FABRIC_PEER_ENDPOINT` | `grpcs://192.168.116.144:7051` |
| `FABRIC_ORDERER_ENDPOINT` | `grpcs://192.168.116.144:7050` |
| `FABRIC_CLIENT_CERT` | `backend/certs/supplier-cert.pem` |
| `FABRIC_CLIENT_KEY` | `backend/certs/supplier-key.pem` |
| `FABRIC_PEER_TLS` | `backend/certs/supplier-tls-ca.pem` |
| `FABRIC_ORDERER_TLS` | `backend/certs/orderer-tls-ca.pem` |

前端网关剥离浏览器 `Origin` 头，将本机 `/api` 请求转发至 `127.0.0.1:8080`，因此生产访问只暴露 `5173` 端口。

## 接口范围

后端只保留供应商需要的接口：

```text
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/change-password
GET  /api/dashboard
GET  /api/batches
POST /api/batches
GET  /api/batches/{id}
POST /api/batches/{id}/events
GET  /api/batches/{id}/verify
POST /api/batches/{id}/evidence
GET  /api/evidence/{id}
GET  /api/events/allowed
GET  /api/submissions/mine
GET  /api/reviews/pending
POST /api/reviews/{id}/approve
POST /api/reviews/{id}/reject
POST /api/events/{id}/retry
GET  /api/org/users
GET  /api/org/roles
POST /api/org/users
PUT  /api/org/users/{id}/status
PUT  /api/org/users/{id}/password
GET  /api/directory/organizations
GET  /api/system/chain
GET  /actuator/health
```

注册、消费者、平台管理和监管接口均未包含在独立节点中。

## 已验证闭环

2026-11-03 使用新源码构建后端完成以下真实测试：

1. 创建原料鸭批次并写入 Fabric。
2. 上传现场附件并计算证据根。
3. 养殖记录员、内部质检员、操作员、仓储发货员分别提交本岗位事件。
4. 管理员连续审核四条事件，全部提交到 Fabric。
5. 链上核验结果为 `4/4`，全部哈希一致。
6. 发货交接生成到 `PRO001` 加工企业，承运企业为 `LOG001`，交接状态为待收货。
7. 其他企业账号登录返回 `403`。
8. 前端完成登录、工作台、批次列表、批次详情、交接记录和员工角色页面的浏览器冒烟测试。
