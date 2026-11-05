-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: changde_trace
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `changde_trace`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `changde_trace` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `changde_trace`;

--
-- Table structure for table `access_grant`
--

DROP TABLE IF EXISTS `access_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_grant` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `grantor_org_id` bigint NOT NULL,
  `grantee_org_id` bigint NOT NULL,
  `field_scope` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valid_from` datetime(6) NOT NULL,
  `valid_until` datetime(6) DEFAULT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_grant_batch` (`batch_id`),
  KEY `fk_grant_grantor` (`grantor_org_id`),
  KEY `fk_grant_grantee` (`grantee_org_id`),
  CONSTRAINT `fk_grant_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_grant_grantee` FOREIGN KEY (`grantee_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_grant_grantor` FOREIGN KEY (`grantor_org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_grant`
--

LOCK TABLES `access_grant` WRITE;
/*!40000 ALTER TABLE `access_grant` DISABLE KEYS */;
/*!40000 ALTER TABLE `access_grant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `org_id` bigint DEFAULT NULL,
  `action` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_id` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail_json` longtext COLLATE utf8mb4_unicode_ci,
  `ip_address` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
INSERT INTO `audit_log` VALUES (1,1,1,'CREATE_BATCH','TRACE_BATCH','B-B9E25EAC78A34A149FE3','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"batchNo\":\"YL-20261101001859-BDC1\",\"batchType\":\"RAW_DUCK\",\"createdAt\":\"2026-11-01T00:18:59.084166200\",\"custodianOrgId\":\"1\",\"expiryDate\":\"\",\"lastEventId\":\"\",\"lastHash\":\"6aa68270bbd3747178c4a56ca0866a75815f0667d4b90bd0425bf79209dabf32\",\"ownerOrgId\":\"1\",\"parentBatchIds\":[],\"productName\":\"常德麻鸭\",\"productionDate\":\"2026-11-01\",\"quantity\":\"200\",\"spec\":\"整鸭\",\"stage\":\"SOURCE\",\"status\":\"ACTIVE\",\"traceCode\":\"\",\"unit\":\"kg\"}',NULL,'2026-11-01 00:19:01.214030'),(2,1,1,'QUARANTINE','TRACE_EVENT','E-05457F6A3E7C4FC392C3','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:20\",\"carrierOrgId\":\"\",\"eventId\":\"E-05457F6A3E7C4FC392C3\",\"eventType\":\"QUARANTINE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"1\",\"locationCode\":\"常德金鸭养殖基地\",\"operatorUserId\":\"1\",\"prevEventId\":\"\",\"prevHash\":\"6aa68270bbd3747178c4a56ca0866a75815f0667d4b90bd0425bf79209dabf32\",\"processData\":{\"issuerOrg\":\"常德动物卫生监督所\",\"quarantineCertNo\":\"QY-20261101-001\",\"quarantineDate\":\"2026-11-01\",\"quarantineResult\":\"PASS\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"QUARANTINED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:19:12.305143'),(3,1,1,'DISPATCH','TRACE_EVENT','E-E895546863A444E2A7CA','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:22\",\"carrierOrgId\":\"\",\"eventId\":\"E-E895546863A444E2A7CA\",\"eventType\":\"DISPATCH\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"1\",\"locationCode\":\"常德金鸭养殖基地\",\"operatorUserId\":\"1\",\"prevEventId\":\"E-05457F6A3E7C4FC392C3\",\"prevHash\":\"a1016dbbc9873378e66db03008852252f53d7e8bdd39692669290003ad9415b1\",\"processData\":{\"destination\":\"常德酱板鸭食品有限公司\",\"sealNo\":\"SEAL-001\",\"temperature\":\"12\",\"vehicleRef\":\"湘J-12345\"},\"resultStatus\":\"DISPATCHED\",\"stage\":\"DISPATCHED\",\"targetOrgId\":\"2\"}',NULL,'2026-11-01 00:19:20.060348'),(4,2,2,'RECEIVE','TRACE_EVENT','E-349415A2BB344EC1AFC3','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:25\",\"carrierOrgId\":\"\",\"eventId\":\"E-349415A2BB344EC1AFC3\",\"eventType\":\"RECEIVE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德加工一车间\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-E895546863A444E2A7CA\",\"prevHash\":\"c490d661541add2c8d4b0431f25e2a8ce12a3d9ada37a2cd2f172e6f4213cdc3\",\"processData\":{\"discrepancyCode\":\"\",\"packageIntact\":\"是\",\"quantityReceived\":\"200\",\"temperature\":\"12\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"RECEIVED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:01.153487'),(5,2,2,'CREATE_BATCH','TRACE_BATCH','B-E564BA8B6A4A485FAB62','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"batchNo\":\"BC-20261101002032-4717\",\"batchType\":\"SEMI_FINISHED\",\"createdAt\":\"2026-11-01T00:20:32.085511200\",\"custodianOrgId\":\"2\",\"expiryDate\":\"2027-05-01\",\"lastEventId\":\"\",\"lastHash\":\"8c10742e781ee3d81fc62a3ef675f82b4ebb1d2d1e76a2f84b08b13b78ff0657\",\"ownerOrgId\":\"2\",\"parentBatchIds\":[\"B-B9E25EAC78A34A149FE3\"],\"productName\":\"酱板鸭加工批次\",\"productionDate\":\"2026-11-01\",\"quantity\":\"180\",\"spec\":\"待包装\",\"stage\":\"CREATED\",\"status\":\"ACTIVE\",\"traceCode\":\"\",\"unit\":\"kg\"}',NULL,'2026-11-01 00:20:34.141262'),(6,2,2,'SLAUGHTER','TRACE_EVENT','E-644235B0264342D58587','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T07:10\",\"carrierOrgId\":\"\",\"eventId\":\"E-644235B0264342D58587\",\"eventType\":\"SLAUGHTER\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"\",\"prevHash\":\"8c10742e781ee3d81fc62a3ef675f82b4ebb1d2d1e76a2f84b08b13b78ff0657\",\"processData\":{\"brineTemperature\":\"4\",\"carcassWeight\":\"165\",\"liveWeight\":\"200\",\"marinadeDurationMin\":\"360\",\"marinadeEndAt\":\"2026-11-01T07:00:00\",\"marinadeStartAt\":\"2026-11-01T01:00:00\",\"preSlaughterQuarantineResult\":\"PASS\",\"saltBatchNo\":\"SALT-01\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"SLAUGHTERED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:36.235578'),(7,2,2,'MARINATE','TRACE_EVENT','E-7415ECD554CF47BFAAEB','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T07:15\",\"carrierOrgId\":\"\",\"eventId\":\"E-7415ECD554CF47BFAAEB\",\"eventType\":\"MARINATE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-644235B0264342D58587\",\"prevHash\":\"6406a6ea1f147710d8402a88b988b6feb85d5be12757c14e98d487468b51c617\",\"processData\":{\"durationMin\":\"360\",\"endAt\":\"2026-11-01T07:00:00\",\"equipmentId\":\"M-01\",\"saltBatchNo\":\"SALT-01\",\"startAt\":\"2026-11-01T01:00:00\",\"temperature\":\"4\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"MARINATED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:38.329365'),(8,2,2,'SAUCE','TRACE_EVENT','E-C08191BE171248E1BE8B','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T11:05\",\"carrierOrgId\":\"\",\"eventId\":\"E-C08191BE171248E1BE8B\",\"eventType\":\"SAUCE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-7415ECD554CF47BFAAEB\",\"prevHash\":\"61501e708a6e72cae060030a3f26c9bec23c075b1c1b4f85c5bdff5c1b5ab93f\",\"processData\":{\"actualTemperature\":\"91.5\",\"ccpResult\":\"PASS\",\"durationMin\":\"180\",\"endAt\":\"2026-11-01T11:00:00\",\"equipmentId\":\"C-01\",\"formulaVersion\":\"V1.0\",\"pH\":\"6.2\",\"pressureKPa\":\"101\",\"salinity\":\"3.5\",\"setTemperature\":\"92\",\"startAt\":\"2026-11-01T08:00:00\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"SAUCED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:40.396897'),(9,2,2,'ROAST','TRACE_EVENT','E-73C279E4FE2D4CCBA465','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T13:25\",\"carrierOrgId\":\"\",\"eventId\":\"E-73C279E4FE2D4CCBA465\",\"eventType\":\"ROAST\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-C08191BE171248E1BE8B\",\"prevHash\":\"b0b6b0cffb3c85d7ca5eb6c5e5b103db228f62d1a3ee0a22d5aedfc082a7e5f0\",\"processData\":{\"ccpCompliant\":\"PASS\",\"coreTemperature\":\"78\",\"endAt\":\"2026-11-01T13:20:00\",\"equipmentId\":\"R-01\",\"exceptionCode\":\"\",\"humidity\":\"55\",\"processType\":\"烘烤\",\"setTemperature\":\"85\",\"startAt\":\"2026-11-01T11:20:00\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"ROASTED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:42.463928'),(10,2,2,'PACKAGE','TRACE_EVENT','E-7F681640B3974F879AA2','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T14:00\",\"carrierOrgId\":\"\",\"eventId\":\"E-7F681640B3974F879AA2\",\"eventType\":\"PACKAGE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-73C279E4FE2D4CCBA465\",\"prevHash\":\"45fbdff028173c796756b24495cf328635b540d796bdbd5da0b7d313391f8996\",\"processData\":{\"expiryDate\":\"2027-05-01\",\"netWeight\":\"300\",\"packageMaterialBatchNo\":\"PKG-01\",\"packageQuantity\":\"500\",\"productCode\":\"CDJBY-300\",\"productionLineNo\":\"L1\",\"qaResult\":\"PASS\",\"sealTemperature\":\"145\",\"spec\":\"300g/袋\",\"vacuumDegree\":\"-0.09MPa\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"PACKAGED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:44.535477'),(11,2,2,'WAREHOUSE_IN','TRACE_EVENT','E-B588DD8EC9FE445BA7DD','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T14:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-B588DD8EC9FE445BA7DD\",\"eventType\":\"WAREHOUSE_IN\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-7F681640B3974F879AA2\",\"prevHash\":\"b765901bb45768fe7d31658cc150005a2fd672b605e59573347f296803ad181f\",\"processData\":{\"binNo\":\"A-01\",\"coldChainStatus\":\"NORMAL\",\"quantity\":\"150\",\"temperature\":\"4\",\"warehouseId\":\"WH-01\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"WAREHOUSED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:46.612833'),(12,2,2,'DISPATCH','TRACE_EVENT','E-15367AB11FBD43958C8F','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T15:00\",\"carrierOrgId\":\"3\",\"eventId\":\"E-15367AB11FBD43958C8F\",\"eventType\":\"DISPATCH\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-B588DD8EC9FE445BA7DD\",\"prevHash\":\"fe6f541ed88da62872e06765264c3f18e4da11ec5b54a3e43a8e55cd4add4c41\",\"processData\":{\"destination\":\"常德老街特产店\",\"sealNo\":\"SEAL-002\",\"temperature\":\"5\",\"vehicleRef\":\"湘J-LOG01\"},\"resultStatus\":\"DISPATCHED\",\"stage\":\"DISPATCHED\",\"targetOrgId\":\"4\"}',NULL,'2026-11-01 00:20:48.702190'),(13,4,3,'TRANSPORT','TRACE_EVENT','E-6A2075291FD7420FB673','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T15:20\",\"carrierOrgId\":\"\",\"eventId\":\"E-6A2075291FD7420FB673\",\"eventType\":\"TRANSPORT\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"3\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"4\",\"prevEventId\":\"E-15367AB11FBD43958C8F\",\"prevHash\":\"e80a3775bf169fe2e29e53089f2038218017f790e78c0c70b37a6e256ac88ee9\",\"processData\":{\"alarmCount\":\"0\",\"coldChainCompliant\":\"PASS\",\"fromPlace\":\"常德加工园\",\"sealNo\":\"SEAL-002\",\"temperature\":\"4\",\"toPlace\":\"常德老街特产店\",\"vehicleRef\":\"湘J-LOG01\"},\"resultStatus\":\"IN_TRANSIT\",\"stage\":\"IN_TRANSIT\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:56.875193'),(14,4,3,'DELIVER','TRACE_EVENT','E-986B72694D6445B69694','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T16:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-986B72694D6445B69694\",\"eventType\":\"DELIVER\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"3\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"4\",\"prevEventId\":\"E-6A2075291FD7420FB673\",\"prevHash\":\"1c75e1e3b4dc0369805ade738c8b12e45cda002d323b6a2e2c54a548fb17bb56\",\"processData\":{\"deliveredAt\":\"2026-11-01T16:30:00\",\"discrepancyCode\":\"\",\"receiptResult\":\"正常\",\"temperature\":\"4\"},\"resultStatus\":\"DELIVERED\",\"stage\":\"DELIVERED\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:20:58.940977'),(15,5,4,'RECEIVE','TRACE_EVENT','E-F98ACF267CC74310B3B8','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T16:45\",\"carrierOrgId\":\"\",\"eventId\":\"E-F98ACF267CC74310B3B8\",\"eventType\":\"RECEIVE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"4\",\"locationCode\":\"常德老街特产店\",\"operatorUserId\":\"5\",\"prevEventId\":\"E-986B72694D6445B69694\",\"prevHash\":\"7baea2f550ffe7838315785a2e816fa14ee79734e003f80481000d06e86ed734\",\"processData\":{\"discrepancyCode\":\"\",\"packageIntact\":\"是\",\"quantityReceived\":\"150\",\"temperature\":\"5\"},\"resultStatus\":\"ON_SALE\",\"stage\":\"ON_SALE\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:21:01.096723'),(16,5,4,'SALE','TRACE_EVENT','E-77B41695E4FD4454A46D','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T17:00\",\"carrierOrgId\":\"\",\"eventId\":\"E-77B41695E4FD4454A46D\",\"eventType\":\"SALE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"4\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"5\",\"prevEventId\":\"E-F98ACF267CC74310B3B8\",\"prevHash\":\"b960871c2d081b53ca6c139a09f99a6d94d3fc92bf0ee87951918ae0ca931189\",\"processData\":{\"quantitySold\":\"20\",\"remainingQuantity\":\"130\",\"saleOrderHash\":\"ORDER-DEMO-001\",\"salesChannel\":\"门店\",\"storageTemperature\":\"5\"},\"resultStatus\":\"SOLD\",\"stage\":\"SOLD\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:21:03.158856'),(17,6,5,'INSPECTION','TRACE_EVENT','E-87107801FBDE418E900B','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T17:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-87107801FBDE418E900B\",\"eventType\":\"INSPECTION\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"5\",\"locationCode\":\"常德市市场监督管理局\",\"operatorUserId\":\"6\",\"prevEventId\":\"E-77B41695E4FD4454A46D\",\"prevHash\":\"1ff46f6a3156aebf61618d7ac01db5d50b514fdde67eaa6619a2287ba3807c1a\",\"processData\":{\"decision\":\"RELEASE\",\"indicators\":[{\"limit\":\"10000\",\"name\":\"菌落总数\",\"result\":\"PASS\",\"unit\":\"CFU/g\",\"value\":\"100\"}],\"inspectionType\":\"监督抽检\",\"overallResult\":\"PASS\",\"remark\":\"抽检合格\",\"reportNo\":\"REP-20261101-001\",\"sampleNo\":\"JC-20261101-001\"},\"resultStatus\":\"SOLD\",\"stage\":\"SOLD\",\"targetOrgId\":\"\"}',NULL,'2026-11-01 00:22:51.290436');
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `batch_party`
--

DROP TABLE IF EXISTS `batch_party`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_party` (
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `org_id` bigint NOT NULL,
  `party_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `visible_scope` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FULL',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`batch_id`,`org_id`,`party_type`),
  KEY `fk_party_org` (`org_id`),
  CONSTRAINT `fk_party_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_party_org` FOREIGN KEY (`org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `batch_party`
--

LOCK TABLES `batch_party` WRITE;
/*!40000 ALTER TABLE `batch_party` DISABLE KEYS */;
INSERT INTO `batch_party` VALUES ('B-B9E25EAC78A34A149FE3',1,'OWNER','FULL','2026-11-01 00:18:59.104638'),('B-B9E25EAC78A34A149FE3',2,'RECEIVER','FULL','2026-11-01 00:19:18.011195'),('B-E564BA8B6A4A485FAB62',2,'OWNER','FULL','2026-11-01 00:20:32.095913'),('B-E564BA8B6A4A485FAB62',3,'CARRIER','FULL','2026-11-01 00:20:46.646280'),('B-E564BA8B6A4A485FAB62',4,'RECEIVER','FULL','2026-11-01 00:20:46.641059');
/*!40000 ALTER TABLE `batch_party` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `batch_relation`
--

DROP TABLE IF EXISTS `batch_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `child_batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `relation_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` decimal(16,3) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation` (`parent_batch_id`,`child_batch_id`),
  KEY `fk_rel_child` (`child_batch_id`),
  CONSTRAINT `fk_rel_child` FOREIGN KEY (`child_batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_rel_parent` FOREIGN KEY (`parent_batch_id`) REFERENCES `trace_batch` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `batch_relation`
--

LOCK TABLES `batch_relation` WRITE;
/*!40000 ALTER TABLE `batch_relation` DISABLE KEYS */;
INSERT INTO `batch_relation` VALUES (1,'B-B9E25EAC78A34A149FE3','B-E564BA8B6A4A485FAB62','MERGE',180.000,'2026-11-01 00:20:32.101771');
/*!40000 ALTER TABLE `batch_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `custody_transfer`
--

DROP TABLE IF EXISTS `custody_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `custody_transfer` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `from_org_id` bigint NOT NULL,
  `to_org_id` bigint NOT NULL,
  `carrier_org_id` bigint DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `outbound_at` datetime(6) DEFAULT NULL,
  `received_at` datetime(6) DEFAULT NULL,
  `discrepancy_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_transfer_from` (`from_org_id`),
  KEY `fk_transfer_to` (`to_org_id`),
  KEY `fk_transfer_carrier` (`carrier_org_id`),
  KEY `idx_transfer_batch_status` (`batch_id`,`status`),
  CONSTRAINT `fk_transfer_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_transfer_carrier` FOREIGN KEY (`carrier_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_transfer_from` FOREIGN KEY (`from_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_transfer_to` FOREIGN KEY (`to_org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `custody_transfer`
--

LOCK TABLES `custody_transfer` WRITE;
/*!40000 ALTER TABLE `custody_transfer` DISABLE KEYS */;
INSERT INTO `custody_transfer` VALUES ('T-B175BAD58D70463482B1','B-E564BA8B6A4A485FAB62','E-F98ACF267CC74310B3B8',2,4,3,'RECEIVED','2026-11-01 00:20:46.633353','2026-11-01 00:21:01.080651',NULL,'2026-11-01 00:20:46.635450'),('T-DC6B88043DA9498DA58C','B-B9E25EAC78A34A149FE3','E-349415A2BB344EC1AFC3',1,2,NULL,'RECEIVED','2026-11-01 00:19:18.004772','2026-11-01 00:20:01.144716',NULL,'2026-11-01 00:19:18.005902');
/*!40000 ALTER TABLE `custody_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evidence`
--

DROP TABLE IF EXISTS `evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evidence` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uploaded_by` bigint NOT NULL,
  `object_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint NOT NULL,
  `sha256` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_evidence_event` (`event_id`),
  KEY `fk_evidence_user` (`uploaded_by`),
  KEY `idx_evidence_batch` (`batch_id`),
  CONSTRAINT `fk_evidence_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_evidence_event` FOREIGN KEY (`event_id`) REFERENCES `trace_event` (`id`),
  CONSTRAINT `fk_evidence_user` FOREIGN KEY (`uploaded_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evidence`
--

LOCK TABLES `evidence` WRITE;
/*!40000 ALTER TABLE `evidence` DISABLE KEYS */;
INSERT INTO `evidence` VALUES ('V-2707338E982B45C8BC35','B-E564BA8B6A4A485FAB62','E-BFAACBB0D5904EA0ACD6',6,'B-E564BA8B6A4A485FAB62/V-2707338E982B45C8BC35.md','README.md','application/octet-stream',4048,'c2a8b9a6ba8432b6245744b56be43fe155ef31bd5a58a193cbdb2aa65cfa683a','2026-11-01 08:36:45.208869');
/*!40000 ALTER TABLE `evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_record`
--

DROP TABLE IF EXISTS `inspection_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_record` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspection_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_no` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `indicators_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `overall_result` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_no` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issued_by` bigint NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `decision` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_inspection_batch` (`batch_id`),
  KEY `fk_inspection_event` (`event_id`),
  KEY `fk_inspection_org` (`issued_by`),
  CONSTRAINT `fk_inspection_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_inspection_event` FOREIGN KEY (`event_id`) REFERENCES `trace_event` (`id`),
  CONSTRAINT `fk_inspection_org` FOREIGN KEY (`issued_by`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_record`
--

LOCK TABLES `inspection_record` WRITE;
/*!40000 ALTER TABLE `inspection_record` DISABLE KEYS */;
INSERT INTO `inspection_record` VALUES ('I-0BF0590FE39343C788A8','B-E564BA8B6A4A485FAB62','E-87107801FBDE418E900B','监督抽检','JC-20261101-001','[{\"limit\":\"10000\",\"name\":\"菌落总数\",\"result\":\"PASS\",\"unit\":\"CFU/g\",\"value\":\"100\"}]','PASS','REP-20261101-001',NULL,5,'2026-11-01 17:30:00.000000','RELEASE','抽检合格','2026-11-01 00:22:51.273945');
/*!40000 ALTER TABLE `inspection_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organization`
--

DROP TABLE IF EXISTS `organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `org_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `org_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `region_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `msp_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `org_code` (`org_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organization`
--

LOCK TABLES `organization` WRITE;
/*!40000 ALTER TABLE `organization` DISABLE KEYS */;
INSERT INTO `organization` VALUES (1,'SUP001','常德金鸭养殖基地','SUPPLIER','430700','Org1MSP','ACTIVE','2026-11-01 00:14:27.710112'),(2,'PRO001','常德酱板鸭食品有限公司','PROCESSOR','430700','Org1MSP','ACTIVE','2026-11-01 00:14:27.710112'),(3,'LOG001','沅水冷链物流有限公司','LOGISTICS','430700','Org1MSP','ACTIVE','2026-11-01 00:14:27.710112'),(4,'RET001','常德老街特产店','RETAILER','430700','Org1MSP','ACTIVE','2026-11-01 00:14:27.710112'),(5,'REG001','常德市市场监督管理局','REGULATOR','430700','Org2MSP','ACTIVE','2026-11-01 00:14:27.710112'),(6,'PLATFORM001','溯源平台管理中心','ADMIN','430700','Org1MSP','ACTIVE','2026-11-01 00:14:27.710112');
/*!40000 ALTER TABLE `organization` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'SUPPLIER_OPERATOR','原料供应商操作员'),(2,'PROCESSOR_OPERATOR','生产加工操作员'),(3,'PROCESSOR_QA','生产质检员'),(4,'LOGISTICS_OPERATOR','物流操作员'),(5,'RETAILER_OPERATOR','销售商操作员'),(6,'REGULATOR','监管人员'),(7,'PLATFORM_ADMIN','平台管理员');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `org_id` bigint NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `fk_user_org` (`org_id`),
  CONSTRAINT `fk_user_org` FOREIGN KEY (`org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'supplier','$2a$10$Not078x8lQmStLyHfAW6x.uhwIy4rQfw/O5sOUX6VUFLwSFZRDGHa','供应商演示账号',1,1,'2026-11-01 00:14:29.488742'),(2,'processor','$2a$10$qmO6jKMb2k1cZf6BcMBMVuKJRA.6/8WwYu5GUYSpU1QOQrvWbYn7u','加工企业演示账号',2,1,'2026-11-01 00:14:29.559243'),(3,'quality','$2a$10$XkeRat2b.d4zl74w3sYppe0zQRSRe93dyxTfeyPCyr683ZS8ydx9m','加工质检演示账号',2,1,'2026-11-01 00:14:29.630130'),(4,'logistics','$2a$10$jwDk/b0OdpZY0vgCwG4B9Oq7N/DNCiwAaIDtHxTZpG9iklw3WyTla','物流演示账号',3,1,'2026-11-01 00:14:29.700493'),(5,'retailer','$2a$10$OrzsYvsv1ea0yHrTqIAk7OINsLib6kHkTg4ieAerw8KJYtHi5mLsu','销售演示账号',4,1,'2026-11-01 00:14:29.771165'),(6,'regulator','$2a$10$S1pV0VtnZykWEu.im0aWLOo4I0qp.ZMBuDb3qjBu710BR5oS4E0Gm','监管演示账号',5,1,'2026-11-01 00:14:29.846472'),(7,'admin','$2a$10$9oz.5PoGUtPp1tGZlgy7pOcth0eSB3LuaN98VAhLYE6GM2zGEX0l6','平台管理员',6,1,'2026-11-01 00:14:29.925112');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_ur_role` (`role_id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trace_batch`
--

DROP TABLE IF EXISTS `trace_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_batch` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trace_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `batch_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_org_id` bigint NOT NULL,
  `custodian_org_id` bigint NOT NULL,
  `product_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `spec` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` decimal(16,3) NOT NULL DEFAULT '0.000',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'kg',
  `current_stage` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `last_event_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chain_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `tx_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_no` (`batch_no`),
  UNIQUE KEY `trace_code` (`trace_code`),
  KEY `fk_batch_owner` (`owner_org_id`),
  KEY `fk_batch_custodian` (`custodian_org_id`),
  KEY `fk_batch_user` (`created_by`),
  KEY `idx_batch_status` (`status`),
  KEY `idx_batch_stage` (`current_stage`),
  KEY `idx_batch_created` (`created_at`),
  CONSTRAINT `fk_batch_custodian` FOREIGN KEY (`custodian_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_batch_owner` FOREIGN KEY (`owner_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_batch_user` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trace_batch`
--

LOCK TABLES `trace_batch` WRITE;
/*!40000 ALTER TABLE `trace_batch` DISABLE KEYS */;
INSERT INTO `trace_batch` VALUES ('B-B9E25EAC78A34A149FE3','YL-20261101001859-BDC1',NULL,'RAW_DUCK',1,2,'常德麻鸭','整鸭',200.000,'kg','RECEIVED','ACTIVE','2026-11-01',NULL,'E-349415A2BB344EC1AFC3','fccbafd62f87df5b2a1b7763fa86694d8935b83189548c0e96fed4d356ec18be','COMMITTED','931e92c7176f26a2843cb3de37d898c8933c043d47c981b92feab288812a9aea',1,'2026-11-01 00:18:59.098772','2026-11-01 00:20:01.146592'),('B-E564BA8B6A4A485FAB62','BC-20261101002032-4717','CDJBY-20261101002044-E8FAC5','FINISHED',2,4,'酱板鸭加工批次','待包装',180.000,'kg','SOLD','SOLD','2026-11-01','2027-05-01','E-87107801FBDE418E900B','dbae9f2bb77dca9b02ed9efdf3be4c6bc646e61053b672b1ae9633b481188078','COMMITTED','2ef2c6f4cff8797ce6ec7cb8ac04613b8ded759de1da99ee4afdc22061bf824a',2,'2026-11-01 00:20:32.088370','2026-11-01 00:22:51.271882');
/*!40000 ALTER TABLE `trace_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trace_event`
--

DROP TABLE IF EXISTS `trace_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_event` (
  `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issuer_org_id` bigint NOT NULL,
  `operator_user_id` bigint NOT NULL,
  `business_time` datetime(6) NOT NULL,
  `location_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `evidence_root` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prev_event_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prev_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `chain_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `tx_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `block_number` bigint DEFAULT NULL,
  `chain_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correction_of` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `request_id` (`request_id`),
  KEY `fk_event_org` (`issuer_org_id`),
  KEY `fk_event_user` (`operator_user_id`),
  KEY `idx_event_batch_time` (`batch_id`,`business_time`),
  CONSTRAINT `fk_event_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_batch` (`id`),
  CONSTRAINT `fk_event_org` FOREIGN KEY (`issuer_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_event_user` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trace_event`
--

LOCK TABLES `trace_event` WRITE;
/*!40000 ALTER TABLE `trace_event` DISABLE KEYS */;
INSERT INTO `trace_event` VALUES ('E-05457F6A3E7C4FC392C3','B-B9E25EAC78A34A149FE3','QUARANTINE','QUARANTINED','ACTIVE',1,1,'2026-11-01 00:20:00.000000','常德金鸭养殖基地','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:20\",\"carrierOrgId\":\"\",\"eventId\":\"E-05457F6A3E7C4FC392C3\",\"eventType\":\"QUARANTINE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"1\",\"locationCode\":\"常德金鸭养殖基地\",\"operatorUserId\":\"1\",\"prevEventId\":\"\",\"prevHash\":\"6aa68270bbd3747178c4a56ca0866a75815f0667d4b90bd0425bf79209dabf32\",\"processData\":{\"issuerOrg\":\"常德动物卫生监督所\",\"quarantineCertNo\":\"QY-20261101-001\",\"quarantineDate\":\"2026-11-01\",\"quarantineResult\":\"PASS\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"QUARANTINED\",\"targetOrgId\":\"\"}','a1016dbbc9873378e66db03008852252f53d7e8bdd39692669290003ad9415b1',NULL,NULL,'6aa68270bbd3747178c4a56ca0866a75815f0667d4b90bd0425bf79209dabf32','e43c014a-8977-47f1-99a2-8d943be9a3de','COMMITTED','37af2b8f4d505565de0b08af6e22c90244870ea99fd78c27928f1871de0faaff',10,NULL,NULL,'2026-11-01 00:19:10.257348'),('E-15367AB11FBD43958C8F','B-E564BA8B6A4A485FAB62','DISPATCH','DISPATCHED','DISPATCHED',2,2,'2026-11-01 15:00:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T15:00\",\"carrierOrgId\":\"3\",\"eventId\":\"E-15367AB11FBD43958C8F\",\"eventType\":\"DISPATCH\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-B588DD8EC9FE445BA7DD\",\"prevHash\":\"fe6f541ed88da62872e06765264c3f18e4da11ec5b54a3e43a8e55cd4add4c41\",\"processData\":{\"destination\":\"常德老街特产店\",\"sealNo\":\"SEAL-002\",\"temperature\":\"5\",\"vehicleRef\":\"湘J-LOG01\"},\"resultStatus\":\"DISPATCHED\",\"stage\":\"DISPATCHED\",\"targetOrgId\":\"4\"}','e80a3775bf169fe2e29e53089f2038218017f790e78c0c70b37a6e256ac88ee9',NULL,'E-B588DD8EC9FE445BA7DD','fe6f541ed88da62872e06765264c3f18e4da11ec5b54a3e43a8e55cd4add4c41','1cee80bd-4649-4f75-a98f-b4143d17d6df','COMMITTED','8682a681899aa48b7bc0848a6d239d98ec32fac7049585da038815a9f5f2f269',20,NULL,NULL,'2026-11-01 00:20:46.652556'),('E-349415A2BB344EC1AFC3','B-B9E25EAC78A34A149FE3','RECEIVE','RECEIVED','ACTIVE',2,2,'2026-11-01 00:25:00.000000','常德加工一车间','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:25\",\"carrierOrgId\":\"\",\"eventId\":\"E-349415A2BB344EC1AFC3\",\"eventType\":\"RECEIVE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德加工一车间\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-E895546863A444E2A7CA\",\"prevHash\":\"c490d661541add2c8d4b0431f25e2a8ce12a3d9ada37a2cd2f172e6f4213cdc3\",\"processData\":{\"discrepancyCode\":\"\",\"packageIntact\":\"是\",\"quantityReceived\":\"200\",\"temperature\":\"12\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"RECEIVED\",\"targetOrgId\":\"\"}','fccbafd62f87df5b2a1b7763fa86694d8935b83189548c0e96fed4d356ec18be',NULL,'E-E895546863A444E2A7CA','c490d661541add2c8d4b0431f25e2a8ce12a3d9ada37a2cd2f172e6f4213cdc3','d10cbef8-4f97-4982-8d5b-7b5cecf56fb3','COMMITTED','931e92c7176f26a2843cb3de37d898c8933c043d47c981b92feab288812a9aea',12,NULL,NULL,'2026-11-01 00:19:57.207602'),('E-644235B0264342D58587','B-E564BA8B6A4A485FAB62','SLAUGHTER','SLAUGHTERED','ACTIVE',2,2,'2026-11-01 07:10:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T07:10\",\"carrierOrgId\":\"\",\"eventId\":\"E-644235B0264342D58587\",\"eventType\":\"SLAUGHTER\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"\",\"prevHash\":\"8c10742e781ee3d81fc62a3ef675f82b4ebb1d2d1e76a2f84b08b13b78ff0657\",\"processData\":{\"brineTemperature\":\"4\",\"carcassWeight\":\"165\",\"liveWeight\":\"200\",\"marinadeDurationMin\":\"360\",\"marinadeEndAt\":\"2026-11-01T07:00:00\",\"marinadeStartAt\":\"2026-11-01T01:00:00\",\"preSlaughterQuarantineResult\":\"PASS\",\"saltBatchNo\":\"SALT-01\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"SLAUGHTERED\",\"targetOrgId\":\"\"}','6406a6ea1f147710d8402a88b988b6feb85d5be12757c14e98d487468b51c617',NULL,NULL,'8c10742e781ee3d81fc62a3ef675f82b4ebb1d2d1e76a2f84b08b13b78ff0657','dba0952a-6a8e-4569-b072-3077217bdbe7','COMMITTED','4062102be3ed7e2ba12f447baeac599b4f415d665b672b69402042e6ee592861',14,NULL,NULL,'2026-11-01 00:20:34.185261'),('E-6A2075291FD7420FB673','B-E564BA8B6A4A485FAB62','TRANSPORT','IN_TRANSIT','IN_TRANSIT',3,4,'2026-11-01 15:20:00.000000','沅水冷链运输节点','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T15:20\",\"carrierOrgId\":\"\",\"eventId\":\"E-6A2075291FD7420FB673\",\"eventType\":\"TRANSPORT\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"3\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"4\",\"prevEventId\":\"E-15367AB11FBD43958C8F\",\"prevHash\":\"e80a3775bf169fe2e29e53089f2038218017f790e78c0c70b37a6e256ac88ee9\",\"processData\":{\"alarmCount\":\"0\",\"coldChainCompliant\":\"PASS\",\"fromPlace\":\"常德加工园\",\"sealNo\":\"SEAL-002\",\"temperature\":\"4\",\"toPlace\":\"常德老街特产店\",\"vehicleRef\":\"湘J-LOG01\"},\"resultStatus\":\"IN_TRANSIT\",\"stage\":\"IN_TRANSIT\",\"targetOrgId\":\"\"}','1c75e1e3b4dc0369805ade738c8b12e45cda002d323b6a2e2c54a548fb17bb56',NULL,'E-15367AB11FBD43958C8F','e80a3775bf169fe2e29e53089f2038218017f790e78c0c70b37a6e256ac88ee9','fb0323a9-5396-4097-8ab4-2fe5f29a8ed2','COMMITTED','496ccf0135f6611d1c384cc2c1ee7eb8c9cfa7e8c6ab2650a2386efcc67e99a3',21,NULL,NULL,'2026-11-01 00:20:54.830301'),('E-73C279E4FE2D4CCBA465','B-E564BA8B6A4A485FAB62','ROAST','ROASTED','ACTIVE',2,2,'2026-11-01 13:25:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T13:25\",\"carrierOrgId\":\"\",\"eventId\":\"E-73C279E4FE2D4CCBA465\",\"eventType\":\"ROAST\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-C08191BE171248E1BE8B\",\"prevHash\":\"b0b6b0cffb3c85d7ca5eb6c5e5b103db228f62d1a3ee0a22d5aedfc082a7e5f0\",\"processData\":{\"ccpCompliant\":\"PASS\",\"coreTemperature\":\"78\",\"endAt\":\"2026-11-01T13:20:00\",\"equipmentId\":\"R-01\",\"exceptionCode\":\"\",\"humidity\":\"55\",\"processType\":\"烘烤\",\"setTemperature\":\"85\",\"startAt\":\"2026-11-01T11:20:00\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"ROASTED\",\"targetOrgId\":\"\"}','45fbdff028173c796756b24495cf328635b540d796bdbd5da0b7d313391f8996',NULL,'E-C08191BE171248E1BE8B','b0b6b0cffb3c85d7ca5eb6c5e5b103db228f62d1a3ee0a22d5aedfc082a7e5f0','7287574c-c500-4d41-a01c-48645a0d9fe5','COMMITTED','94f088fc4004f95e728c2eeab7eb81679218f2ad4cf37bee9962ae18f207dd7b',17,NULL,NULL,'2026-11-01 00:20:40.419608'),('E-7415ECD554CF47BFAAEB','B-E564BA8B6A4A485FAB62','MARINATE','MARINATED','ACTIVE',2,2,'2026-11-01 07:15:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T07:15\",\"carrierOrgId\":\"\",\"eventId\":\"E-7415ECD554CF47BFAAEB\",\"eventType\":\"MARINATE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-644235B0264342D58587\",\"prevHash\":\"6406a6ea1f147710d8402a88b988b6feb85d5be12757c14e98d487468b51c617\",\"processData\":{\"durationMin\":\"360\",\"endAt\":\"2026-11-01T07:00:00\",\"equipmentId\":\"M-01\",\"saltBatchNo\":\"SALT-01\",\"startAt\":\"2026-11-01T01:00:00\",\"temperature\":\"4\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"MARINATED\",\"targetOrgId\":\"\"}','61501e708a6e72cae060030a3f26c9bec23c075b1c1b4f85c5bdff5c1b5ab93f',NULL,'E-644235B0264342D58587','6406a6ea1f147710d8402a88b988b6feb85d5be12757c14e98d487468b51c617','cf6cc235-3f95-4686-a5d3-fef0f221e275','COMMITTED','92a1b4eb1c5ced9caca4682207799be52f2704e1dd7acfce7bf05768867f4735',15,NULL,NULL,'2026-11-01 00:20:36.267760'),('E-77B41695E4FD4454A46D','B-E564BA8B6A4A485FAB62','SALE','SOLD','SOLD',4,5,'2026-11-01 17:00:00.000000','沅水冷链运输节点','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T17:00\",\"carrierOrgId\":\"\",\"eventId\":\"E-77B41695E4FD4454A46D\",\"eventType\":\"SALE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"4\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"5\",\"prevEventId\":\"E-F98ACF267CC74310B3B8\",\"prevHash\":\"b960871c2d081b53ca6c139a09f99a6d94d3fc92bf0ee87951918ae0ca931189\",\"processData\":{\"quantitySold\":\"20\",\"remainingQuantity\":\"130\",\"saleOrderHash\":\"ORDER-DEMO-001\",\"salesChannel\":\"门店\",\"storageTemperature\":\"5\"},\"resultStatus\":\"SOLD\",\"stage\":\"SOLD\",\"targetOrgId\":\"\"}','1ff46f6a3156aebf61618d7ac01db5d50b514fdde67eaa6619a2287ba3807c1a',NULL,'E-F98ACF267CC74310B3B8','b960871c2d081b53ca6c139a09f99a6d94d3fc92bf0ee87951918ae0ca931189','dc2a6555-be65-4504-a5db-743673e6bfe3','COMMITTED','f5c65f407115a0114ed0231be5a816af9b62aab2e4a28d0c797db4d2b8d2e10e',24,NULL,NULL,'2026-11-01 00:21:01.117911'),('E-7F681640B3974F879AA2','B-E564BA8B6A4A485FAB62','PACKAGE','PACKAGED','ACTIVE',2,2,'2026-11-01 14:00:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T14:00\",\"carrierOrgId\":\"\",\"eventId\":\"E-7F681640B3974F879AA2\",\"eventType\":\"PACKAGE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-73C279E4FE2D4CCBA465\",\"prevHash\":\"45fbdff028173c796756b24495cf328635b540d796bdbd5da0b7d313391f8996\",\"processData\":{\"expiryDate\":\"2027-05-01\",\"netWeight\":\"300\",\"packageMaterialBatchNo\":\"PKG-01\",\"packageQuantity\":\"500\",\"productCode\":\"CDJBY-300\",\"productionLineNo\":\"L1\",\"qaResult\":\"PASS\",\"sealTemperature\":\"145\",\"spec\":\"300g/袋\",\"vacuumDegree\":\"-0.09MPa\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"PACKAGED\",\"targetOrgId\":\"\"}','b765901bb45768fe7d31658cc150005a2fd672b605e59573347f296803ad181f',NULL,'E-73C279E4FE2D4CCBA465','45fbdff028173c796756b24495cf328635b540d796bdbd5da0b7d313391f8996','50ff6cfc-f0e7-4b32-a39c-bf6a2503071c','COMMITTED','0b1c5898cb518109dc9fd92716e021262c0110cd3cb26a041b0549f510dd40e4',18,NULL,NULL,'2026-11-01 00:20:42.486313'),('E-87107801FBDE418E900B','B-E564BA8B6A4A485FAB62','INSPECTION','SOLD','SOLD',5,6,'2026-11-01 17:30:00.000000','常德市市场监督管理局','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T17:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-87107801FBDE418E900B\",\"eventType\":\"INSPECTION\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"5\",\"locationCode\":\"常德市市场监督管理局\",\"operatorUserId\":\"6\",\"prevEventId\":\"E-77B41695E4FD4454A46D\",\"prevHash\":\"1ff46f6a3156aebf61618d7ac01db5d50b514fdde67eaa6619a2287ba3807c1a\",\"processData\":{\"decision\":\"RELEASE\",\"indicators\":[{\"limit\":\"10000\",\"name\":\"菌落总数\",\"result\":\"PASS\",\"unit\":\"CFU/g\",\"value\":\"100\"}],\"inspectionType\":\"监督抽检\",\"overallResult\":\"PASS\",\"remark\":\"抽检合格\",\"reportNo\":\"REP-20261101-001\",\"sampleNo\":\"JC-20261101-001\"},\"resultStatus\":\"SOLD\",\"stage\":\"SOLD\",\"targetOrgId\":\"\"}','dbae9f2bb77dca9b02ed9efdf3be4c6bc646e61053b672b1ae9633b481188078',NULL,'E-77B41695E4FD4454A46D','1ff46f6a3156aebf61618d7ac01db5d50b514fdde67eaa6619a2287ba3807c1a','ef2e79e3-a9e4-4c5d-902f-a7cec84adf1e','COMMITTED','2ef2c6f4cff8797ce6ec7cb8ac04613b8ded759de1da99ee4afdc22061bf824a',25,NULL,NULL,'2026-11-01 00:22:47.483568'),('E-986B72694D6445B69694','B-E564BA8B6A4A485FAB62','DELIVER','DELIVERED','DELIVERED',3,4,'2026-11-01 16:30:00.000000','沅水冷链运输节点','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T16:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-986B72694D6445B69694\",\"eventType\":\"DELIVER\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"3\",\"locationCode\":\"沅水冷链运输节点\",\"operatorUserId\":\"4\",\"prevEventId\":\"E-6A2075291FD7420FB673\",\"prevHash\":\"1c75e1e3b4dc0369805ade738c8b12e45cda002d323b6a2e2c54a548fb17bb56\",\"processData\":{\"deliveredAt\":\"2026-11-01T16:30:00\",\"discrepancyCode\":\"\",\"receiptResult\":\"正常\",\"temperature\":\"4\"},\"resultStatus\":\"DELIVERED\",\"stage\":\"DELIVERED\",\"targetOrgId\":\"\"}','7baea2f550ffe7838315785a2e816fa14ee79734e003f80481000d06e86ed734',NULL,'E-6A2075291FD7420FB673','1c75e1e3b4dc0369805ade738c8b12e45cda002d323b6a2e2c54a548fb17bb56','b78b54e7-ea8b-46ca-9d3d-c9a50e744085','COMMITTED','46ada76b251c73cd600c83d064cd3f36a16d3896761636a4269d5713fa22e48d',22,NULL,NULL,'2026-11-01 00:20:56.900350'),('E-B588DD8EC9FE445BA7DD','B-E564BA8B6A4A485FAB62','WAREHOUSE_IN','WAREHOUSED','ACTIVE',2,2,'2026-11-01 14:30:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T14:30\",\"carrierOrgId\":\"\",\"eventId\":\"E-B588DD8EC9FE445BA7DD\",\"eventType\":\"WAREHOUSE_IN\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-7F681640B3974F879AA2\",\"prevHash\":\"b765901bb45768fe7d31658cc150005a2fd672b605e59573347f296803ad181f\",\"processData\":{\"binNo\":\"A-01\",\"coldChainStatus\":\"NORMAL\",\"quantity\":\"150\",\"temperature\":\"4\",\"warehouseId\":\"WH-01\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"WAREHOUSED\",\"targetOrgId\":\"\"}','fe6f541ed88da62872e06765264c3f18e4da11ec5b54a3e43a8e55cd4add4c41',NULL,'E-7F681640B3974F879AA2','b765901bb45768fe7d31658cc150005a2fd672b605e59573347f296803ad181f','a31c6e19-f481-43aa-8d07-713794192580','COMMITTED','a9f1b2e9ef2e118f549c7cb303fba374781191844532f91612c3cacff7e94a88',19,NULL,NULL,'2026-11-01 00:20:44.558088'),('E-BFAACBB0D5904EA0ACD6','B-E564BA8B6A4A485FAB62','INSPECTION','SOLD','SOLD',5,6,'2026-11-01 18:00:00.000000','常德市市场监督管理局','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T18:00\",\"carrierOrgId\":\"\",\"eventId\":\"E-BFAACBB0D5904EA0ACD6\",\"eventType\":\"INSPECTION\",\"evidenceRoot\":\"ee7cbc47ce5b5e02151f46172f85718304ad636606045b9bd8314e377d5ffb44\",\"issuerOrgId\":\"5\",\"locationCode\":\"常德市市场监督管理局\",\"operatorUserId\":\"6\",\"prevEventId\":\"E-87107801FBDE418E900B\",\"prevHash\":\"dbae9f2bb77dca9b02ed9efdf3be4c6bc646e61053b672b1ae9633b481188078\",\"processData\":{\"decision\":\"RELEASE\",\"indicators\":[{\"limit\":\"30\",\"name\":\"亚硝酸盐\",\"result\":\"PASS\",\"unit\":\"mg/kg\",\"value\":\"2\"}],\"inspectionType\":\"监督复核\",\"overallResult\":\"PASS\",\"reportNo\":\"REP-20261101-002\",\"sampleNo\":\"JC-20261101-002\"},\"resultStatus\":\"SOLD\",\"stage\":\"SOLD\",\"targetOrgId\":\"\"}','f6f2eb9f1d1302f5531274440b5337f06e450a1cf76577b68803c75cce7a5f35','ee7cbc47ce5b5e02151f46172f85718304ad636606045b9bd8314e377d5ffb44','E-87107801FBDE418E900B','dbae9f2bb77dca9b02ed9efdf3be4c6bc646e61053b672b1ae9633b481188078','aa602201-7a1c-4f44-911e-1d288fc4a3fc','FAILED',NULL,NULL,'区块链提交失败：getConfigBlock for channel tracechannel failed with peer peer0.org2.example.com.  Status FAILURE, details: Channel Channel{id: 18, name: tracechannel} Sending proposal with transaction: 0bc7586df0a670da758c0964e81122630935b6f57e08438434b20dc2fbb26668 to Peer{ id: 16, name: peer0.org2.example.com, channelName: tracechannel, url: grpcs://192.168.116.144:9051} failed because of: gRPC failure=Status{code=UNAVAILABLE, description=io exception, cause=io.grpc.netty.shaded.io.netty.channel.Abstr',NULL,'2026-11-01 08:36:45.332252'),('E-C08191BE171248E1BE8B','B-E564BA8B6A4A485FAB62','SAUCE','SAUCED','ACTIVE',2,2,'2026-11-01 11:05:00.000000','常德酱板鸭食品有限公司','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T11:05\",\"carrierOrgId\":\"\",\"eventId\":\"E-C08191BE171248E1BE8B\",\"eventType\":\"SAUCE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"2\",\"locationCode\":\"常德酱板鸭食品有限公司\",\"operatorUserId\":\"2\",\"prevEventId\":\"E-7415ECD554CF47BFAAEB\",\"prevHash\":\"61501e708a6e72cae060030a3f26c9bec23c075b1c1b4f85c5bdff5c1b5ab93f\",\"processData\":{\"actualTemperature\":\"91.5\",\"ccpResult\":\"PASS\",\"durationMin\":\"180\",\"endAt\":\"2026-11-01T11:00:00\",\"equipmentId\":\"C-01\",\"formulaVersion\":\"V1.0\",\"pH\":\"6.2\",\"pressureKPa\":\"101\",\"salinity\":\"3.5\",\"setTemperature\":\"92\",\"startAt\":\"2026-11-01T08:00:00\"},\"resultStatus\":\"ACTIVE\",\"stage\":\"SAUCED\",\"targetOrgId\":\"\"}','b0b6b0cffb3c85d7ca5eb6c5e5b103db228f62d1a3ee0a22d5aedfc082a7e5f0',NULL,'E-7415ECD554CF47BFAAEB','61501e708a6e72cae060030a3f26c9bec23c075b1c1b4f85c5bdff5c1b5ab93f','82314b9c-f50a-4428-a36a-70b50af6cff0','COMMITTED','c22fef0cd8daa0b9cb3d7bdc5d80cc6726b8fbae7ac473aeef2aabc188e3986c',16,NULL,NULL,'2026-11-01 00:20:38.352220'),('E-E895546863A444E2A7CA','B-B9E25EAC78A34A149FE3','DISPATCH','DISPATCHED','DISPATCHED',1,1,'2026-11-01 00:22:00.000000','常德金鸭养殖基地','{\"batchId\":\"B-B9E25EAC78A34A149FE3\",\"businessTime\":\"2026-11-01T00:22\",\"carrierOrgId\":\"\",\"eventId\":\"E-E895546863A444E2A7CA\",\"eventType\":\"DISPATCH\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"1\",\"locationCode\":\"常德金鸭养殖基地\",\"operatorUserId\":\"1\",\"prevEventId\":\"E-05457F6A3E7C4FC392C3\",\"prevHash\":\"a1016dbbc9873378e66db03008852252f53d7e8bdd39692669290003ad9415b1\",\"processData\":{\"destination\":\"常德酱板鸭食品有限公司\",\"sealNo\":\"SEAL-001\",\"temperature\":\"12\",\"vehicleRef\":\"湘J-12345\"},\"resultStatus\":\"DISPATCHED\",\"stage\":\"DISPATCHED\",\"targetOrgId\":\"2\"}','c490d661541add2c8d4b0431f25e2a8ce12a3d9ada37a2cd2f172e6f4213cdc3',NULL,'E-05457F6A3E7C4FC392C3','a1016dbbc9873378e66db03008852252f53d7e8bdd39692669290003ad9415b1','412e7d11-09a5-44b4-bc20-9fa0756b0506','COMMITTED','27c30a2c0b100820c04246ae9150c63de0ea7ed17cd7e5b31ef01ae3dabed801',11,NULL,NULL,'2026-11-01 00:19:18.019217'),('E-F98ACF267CC74310B3B8','B-E564BA8B6A4A485FAB62','RECEIVE','ON_SALE','ON_SALE',4,5,'2026-11-01 16:45:00.000000','常德老街特产店','{\"batchId\":\"B-E564BA8B6A4A485FAB62\",\"businessTime\":\"2026-11-01T16:45\",\"carrierOrgId\":\"\",\"eventId\":\"E-F98ACF267CC74310B3B8\",\"eventType\":\"RECEIVE\",\"evidenceRoot\":\"\",\"issuerOrgId\":\"4\",\"locationCode\":\"常德老街特产店\",\"operatorUserId\":\"5\",\"prevEventId\":\"E-986B72694D6445B69694\",\"prevHash\":\"7baea2f550ffe7838315785a2e816fa14ee79734e003f80481000d06e86ed734\",\"processData\":{\"discrepancyCode\":\"\",\"packageIntact\":\"是\",\"quantityReceived\":\"150\",\"temperature\":\"5\"},\"resultStatus\":\"ON_SALE\",\"stage\":\"ON_SALE\",\"targetOrgId\":\"\"}','b960871c2d081b53ca6c139a09f99a6d94d3fc92bf0ee87951918ae0ca931189',NULL,'E-986B72694D6445B69694','7baea2f550ffe7838315785a2e816fa14ee79734e003f80481000d06e86ed734','9d7ddbc5-9bca-410f-aa0e-8b097766b457','COMMITTED','68b4533a4353441330771c2cb78344f2e9ae04a2cb0ef62ddd4bdaded612a572',23,NULL,NULL,'2026-11-01 00:20:59.040963');
/*!40000 ALTER TABLE `trace_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'changde_trace'
--

--
-- Dumping routines for database 'changde_trace'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-01  9:19:05
