# 四组织 Fabric 演示网络

## 固定地址

```text
Fabric 虚拟机：192.168.116.144
Orderer：orderer.example.com:7050
SupplierMSP：peer0.supplier.example.com:7051
ProcessorMSP：peer0.processor.example.com:8051
LogisticsMSP：peer0.logistics.example.com:9051
RetailerMSP：peer0.retailer.example.com:10051
通道：tracechannel
链码：tracecc 1.0
```

四个 Peer 当前都运行在 `yuanwenping` Fabric 虚拟机的 Docker 中。原料供应商
前端和后端运行在 `192.168.116.145`，通过 `SupplierMSP` 连接供应商 Peer。

## 启动已有网络

```bash
cd /home/yuan/fabric-four-peers
./start-existing.sh
```

## 查看状态

```bash
cd /home/yuan/fabric-four-peers
./status-network.sh
```

## 停止网络

```bash
cd /home/yuan/fabric-four-peers
./stop-network.sh
```

停止脚本不会删除账本卷。正常演示不建议执行首次部署脚本 `deploy-network.sh`，
该脚本仅用于重建一个全新的四组织网络。

## 监管说明

本演示严格按照四个企业 Peer 建链。监管部门暂时不拥有独立 Peer；如果要让监管
检验、冻结、召回也使用独立 RegulatorMSP 上链，需要再增加第五个 Peer。
