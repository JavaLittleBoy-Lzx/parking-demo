# 🔄 黑名单ACMS同步功能说明

## 📋 目录

1. [功能概述](#功能概述)
2. [接口文档](#接口文档)
3. [实现逻辑](#实现逻辑)
4. [配置说明](#配置说明)
5. [使用示例](#使用示例)
6. [日志说明](#日志说明)
7. [问题排查](#问题排查)

---

## 🎯 功能概述

### 核心功能
当小程序或后台系统在违规记录中添加黑名单时，系统会自动：
1. ✅ 将黑名单信息保存到本地数据库（`black_list` 表）
2. ✅ 自动同步黑名单到 ACMS 系统（仅限东北林业大学车场）
3. ✅ 完整的日志记录和异常处理

### 同步范围
- **仅对东北林业大学车场生效**
- 其他车场只保存到本地数据库，不同步到 ACMS

### 接口参考
- **ACMS接口文档：** [4.17.添加黑名单（V2.12.1）](https://s.apifox.cn/a088c4fe-5b5c-49c9-901c-cd64316c7c11/288285170e0)
- **接口地址：** `/cxfService/external/extReq`
- **请求方式：** POST
- **命令类型：** `ADD_BLACK_LIST_CAR`

---

## 📡 接口文档

### 请求参数

根据 [ACMS 接口文档](https://s.apifox.cn/a088c4fe-5b5c-49c9-901c-cd64316c7c11/288285170e0)，请求格式如下：

```json
{
    "command": "ADD_BLACK_LIST_CAR",
    "message_id": "12345678901",
    "device_id": "YOUR_DEVICE_ID",
    "sign_type": "MD5",
    "sign": "YOUR_SIGN",
    "charset": "UTF-8",
    "timestamp": "20251004120000",
    "biz_content": {
        "vip_type_code": "703",
        "vip_type_name": "违规黑名单",
        "car_code": "黑A12345",
        "car_owner": "张三",
        "reason": "多次违规停车",
        "is_permament": 1,
        "time_period": {
            "start_time": "2025-10-04 00:00:00",
            "end_time": "2025-11-03 00:00:00"
        },
        "remark1": "违规记录ID: 123",
        "remark2": "由违规记录自动添加",
        "operator": "管理员",
        "operate_time": "2025-10-04 12:00:00"
    }
}
```

### 响应参数

```json
{
    "command": "ADD_BLACK_LIST_CAR_RETURN",
    "message_id": "12345678901",
    "sign_type": "MD5",
    "sign": "abcdef1234567890abcdef1234567890",
    "charset": "UTF-8",
    "timestamp": "20251004120001",
    "biz_content": {
        "code": "0",
        "msg": "ok"
    }
}
```

### 关键字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vip_type_code | String | 是 | 黑名单类型编码（从ACMS获取） |
| vip_type_name | String | 是 | 黑名单类型名称 |
| car_code | String | 是 | 车牌号 |
| car_owner | String | 是 | 车主姓名 |
| reason | String | 是 | 拉黑原因 |
| is_permament | Integer | 是 | 是否永久：1-永久，0-临时 |
| time_period | Object | 否 | 时间段（临时拉黑时必填） |
| time_period.start_time | String | 条件 | 开始时间（临时拉黑必填） |
| time_period.end_time | String | 条件 | 结束时间（临时拉黑必填） |
| remark1 | String | 否 | 备注1 |
| remark2 | String | 否 | 备注2 |
| operator | String | 否 | 操作人 |
| operate_time | String | 否 | 操作时间 |

---

## 🔧 实现逻辑

### 1. 整体流程

```
小程序提交违规记录
    ↓
ViolationsServiceImpl.createViolation()
    ↓
保存到 violations 表 ✅
    ↓
如果 shouldBlacklist = 1
    ↓
保存到 black_list 表 ✅
    ↓
如果是东北林业大学车场
    ↓
调用 AcmsVipService.addBlacklistToAcms() 🔄
    ↓
ACMS 接口调用
    ↓
解析响应结果
    ↓
记录同步日志 📝
```

### 2. 关键代码位置

#### AcmsVipService.java

**方法：** `addBlacklistToAcms(AddBlacklistRequest request)`

```java
/**
 * 添加黑名单到ACMS
 * 对应ACMS接口：ADD_BLACK_LIST_CAR (4.17)
 */
public boolean addBlacklistToAcms(AddBlacklistRequest request) {
    // 仅处理东北林业大学车场
    if (!DONGBEI_FORESTRY_UNIVERSITY.equals(request.getParkName())) {
        log.info("⏭️ [黑名单同步] 非东北林业大学车场，跳过ACMS同步: {}", request.getParkName());
        return false;
    }
    
    // 构建请求、调用接口、解析响应
    // ...
}
```

#### ViolationsServiceImpl.java

**方法：** `createViolation(Violations violation)`

```java
// 🚫 如果需要拉黑，同时向black_list表插入数据
if (violation.getShouldBlacklist() != null && violation.getShouldBlacklist() == 1) {
    // 1. 保存到本地数据库
    blackListMapper.insert(blackList);
    
    // 2. 同步到ACMS系统
    AcmsVipService.AddBlacklistRequest acmsRequest = new AcmsVipService.AddBlacklistRequest();
    // 设置参数...
    boolean acmsSuccess = acmsVipService.addBlacklistToAcms(acmsRequest);
}
```

---

## ⚙️ 配置说明

### application.yml 配置

需要在 `application.yml` 中配置 ACMS 接口参数：

```yaml
acms:
  api:
    # ACMS API基础地址
    url: http://your-acms-server.com
    # 设备ID
    device_id: YOUR_DEVICE_ID
    # 签名类型
    sign_type: MD5
    # 字符集
    charset: UTF-8
```

### 配置说明

| 配置项 | 说明 | 示例 |
|--------|------|------|
| acms.api.url | ACMS服务器地址 | http://192.168.1.100:8080 |
| acms.api.device_id | 设备唯一标识 | PARKING_SYSTEM_001 |
| acms.api.sign_type | 签名算法类型 | MD5 |
| acms.api.charset | 字符编码 | UTF-8 |

### 获取配置信息

请联系 ACMS 系统管理员获取：
- 设备ID（device_id）
- API地址（url）
- 签名密钥

---

## 💡 使用示例

### 示例 1：永久拉黑

#### 小程序提交数据：

```javascript
{
  plateNumber: "黑A12345",
  violationType: "超时停车",
  shouldBlacklist: 1,
  blacklistTypeCode: "703",
  blacklistTypeName: "违规黑名单",
  blacklistDurationType: "permanent",
  blacklistReason: "多次违规停车，屡教不改",
  parkName: "东北林业大学",
  createdBy: "admin"
}
```

#### 系统处理流程：

1. **保存到 violations 表：**
```sql
INSERT INTO violations (
  plate_number, violation_type, should_blacklist,
  blacklist_type_code, blacklist_type_name,
  blacklist_duration_type, blacklist_reason,
  park_name, created_by
) VALUES (
  '黑A12345', '超时停车', 1,
  '703', '违规黑名单',
  'permanent', '多次违规停车，屡教不改',
  '东北林业大学', 'admin'
);
```

2. **保存到 black_list 表：**
```sql
INSERT INTO black_list (
  car_code, owner, reason,
  special_car_type_config_name, blacklist_type_code,
  black_list_forever_flag, park_name
) VALUES (
  '黑A12345', '未知车主', '多次违规停车，屡教不改',
  '违规黑名单', '703',
  '永久', '东北林业大学'
);
```

3. **同步到 ACMS：**
```json
POST /cxfService/external/extReq
{
  "command": "ADD_BLACK_LIST_CAR",
  "biz_content": {
    "vip_type_code": "703",
    "vip_type_name": "违规黑名单",
    "car_code": "黑A12345",
    "car_owner": "未知车主",
    "reason": "多次违规停车，屡教不改",
    "is_permament": 1,
    "operator": "admin",
    "operate_time": "2025-10-04 12:00:00"
  }
}
```

#### 日志输出：

```
✅ [违规记录创建成功] violationId=123, plateNumber=黑A12345
🚫 [需要拉黑] plateNumber=黑A12345
✅ [黑名单添加成功] blacklistId=456, plateNumber=黑A12345, type=违规黑名单, duration=永久
🔄 [开始同步黑名单到ACMS] plateNumber=黑A12345, parkName=东北林业大学
📤 [ACMS请求-添加黑名单] carCode=黑A12345, url=http://...
📥 [ACMS响应-添加黑名单] carCode=黑A12345, response={...}
✅ [黑名单同步ACMS成功] plateNumber=黑A12345, blacklistId=456
```

---

### 示例 2：临时拉黑

#### 小程序提交数据：

```javascript
{
  plateNumber: "黑B67890",
  violationType: "占用消防通道",
  shouldBlacklist: 1,
  blacklistTypeCode: "704",
  blacklistTypeName: "安全黑名单",
  blacklistDurationType: "temporary",
  blacklistStartTime: "2025-10-04 00:00:00",
  blacklistEndTime: "2025-11-03 00:00:00",
  blacklistReason: "占用消防通道，存在安全隐患",
  parkName: "东北林业大学"
}
```

#### 同步到 ACMS：

```json
{
  "command": "ADD_BLACK_LIST_CAR",
  "biz_content": {
    "vip_type_code": "704",
    "vip_type_name": "安全黑名单",
    "car_code": "黑B67890",
    "is_permament": 0,
    "time_period": {
      "start_time": "2025-10-04 00:00:00",
      "end_time": "2025-11-03 00:00:00"
    }
  }
}
```

---

### 示例 3：非东北林业大学车场

#### 提交数据：

```javascript
{
  plateNumber: "京A88888",
  parkName: "中关村停车场",  // ❗ 非东北林业大学
  shouldBlacklist: 1,
  blacklistTypeCode: "703",
  blacklistTypeName: "违规黑名单"
}
```

#### 处理结果：

```
✅ 保存到 violations 表
✅ 保存到 black_list 表
⏭️ 跳过 ACMS 同步（非东北林业大学车场）
```

#### 日志输出：

```
✅ [违规记录创建成功] violationId=789, plateNumber=京A88888
✅ [黑名单添加成功] blacklistId=123, plateNumber=京A88888
⏭️ [黑名单同步] 非东北林业大学车场，跳过ACMS同步: 中关村停车场
```

---

## 📝 日志说明

### 日志级别

| 级别 | 标识 | 说明 |
|------|------|------|
| INFO | ✅ 🔄 📤 📥 ⏭️ | 正常流程信息 |
| WARN | ⚠️ | 警告信息（ACMS同步失败但本地已保存） |
| ERROR | ❌ | 错误信息（异常情况） |

### 关键日志

#### 1. 违规记录创建

```
✅ [违规记录创建成功] violationId=123, plateNumber=黑A12345
🚫 [需要拉黑] plateNumber=黑A12345, typeCode=703, typeName=违规黑名单
```

#### 2. 本地黑名单添加

```
✅ [黑名单添加成功] blacklistId=456, plateNumber=黑A12345, type=违规黑名单, duration=永久
```

#### 3. ACMS同步开始

```
🔄 [开始同步黑名单到ACMS] plateNumber=黑A12345, parkName=东北林业大学
```

#### 4. ACMS请求

```
📤 [ACMS请求-添加黑名单] carCode=黑A12345, url=http://acms-server/cxfService/external/extReq
📋 [请求详情] {"command":"ADD_BLACK_LIST_CAR",...}
```

#### 5. ACMS响应

```
📥 [ACMS响应-添加黑名单] carCode=黑A12345, response={"biz_content":{"code":"0","msg":"ok"}}
📊 [ACMS响应解析] code=0, msg=ok
```

#### 6. 同步结果

**成功：**
```
✅ [黑名单同步ACMS成功] plateNumber=黑A12345, blacklistId=456
```

**失败：**
```
⚠️ [黑名单同步ACMS失败] plateNumber=黑A12345, 但本地黑名单已添加
```

**异常：**
```
❌ [黑名单同步ACMS异常] plateNumber=黑A12345, error=Connection timeout
```

---

## 🔍 问题排查

### 问题 1：ACMS同步失败，返回 code != 0

#### 日志信息：
```
⚠️ ACMS返回错误: code=1, msg=车辆已存在黑名单
```

#### 原因：
- 该车辆已经在 ACMS 黑名单中

#### 解决方案：
1. 检查 ACMS 系统中是否已存在该车牌的黑名单记录
2. 如果需要更新，先调用移除黑名单接口（4.18），再重新添加
3. 本地黑名单仍然有效，不影响系统使用

---

### 问题 2：ACMS 连接超时

#### 日志信息：
```
❌ [黑名单同步ACMS异常] plateNumber=黑A12345, error=Connection timeout
```

#### 原因：
- ACMS服务器网络不通
- ACMS服务未启动
- 防火墙阻止连接

#### 排查步骤：

1. **检查网络连接：**
```bash
ping acms-server-ip
telnet acms-server-ip 8080
```

2. **检查配置：**
```yaml
acms:
  api:
    url: http://192.168.1.100:8080  # ✅ 确认地址正确
```

3. **查看 ACMS 服务状态：**
```bash
# 登录ACMS服务器
systemctl status acms
```

4. **测试接口可用性：**
```bash
curl -X POST http://acms-server/cxfService/external/extReq \
  -H "Content-Type: application/json" \
  -d '{"command":"ADD_BLACK_LIST_CAR",...}'
```

#### 解决方案：
- 确保 ACMS 服务正常运行
- 检查网络连接和防火墙设置
- 本地黑名单已保存，ACMS恢复后可手动同步

---

### 问题 3：ACMS 返回签名错误

#### 日志信息：
```
⚠️ ACMS返回错误: code=2, msg=签名验证失败
```

#### 原因：
- 签名（sign）计算错误
- 签名密钥不正确

#### 解决方案：

1. **检查签名配置：**
```java
// AcmsVipService.java
request.setSign("YOUR_CORRECT_SIGN");
```

2. **联系ACMS管理员：**
- 获取正确的签名密钥
- 确认签名算法（MD5/SHA256等）

3. **验证签名计算逻辑：**
```java
// 根据ACMS文档实现正确的签名算法
String sign = calculateSign(requestData, secretKey);
```

---

### 问题 4：只在本地数据库，未同步到ACMS

#### 日志信息：
```
⏭️ [黑名单同步] 非东北林业大学车场，跳过ACMS同步: 中关村停车场
```

#### 原因：
- 该车场不是东北林业大学，系统设计为仅对东北林业大学车场同步

#### 说明：
- **这是正常行为**，不是错误
- 非东北林业大学车场的黑名单仅保存在本地数据库
- 如需为其他车场也启用ACMS同步，需要修改代码逻辑

#### 扩展支持其他车场：

```java
// AcmsVipService.java
private static final List<String> ACMS_ENABLED_PARKS = Arrays.asList(
    "东北林业大学",
    "其他需要同步的车场名称"
);

public boolean addBlacklistToAcms(AddBlacklistRequest request) {
    if (!ACMS_ENABLED_PARKS.contains(request.getParkName())) {
        log.info("⏭️ [黑名单同步] 非ACMS同步车场，跳过ACMS同步: {}", request.getParkName());
        return false;
    }
    // ...
}
```

---

### 问题 5：时间格式错误

#### 日志信息：
```
❌ [ACMS响应解析] code=3, msg=时间格式错误
```

#### 原因：
- 时间格式不符合ACMS要求

#### 解决方案：

确保时间格式为 `yyyy-MM-dd HH:mm:ss`：

```java
// ViolationsServiceImpl.java
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
acmsRequest.setStartTime(violation.getBlacklistStartTime().format(formatter));
acmsRequest.setEndTime(violation.getBlacklistEndTime().format(formatter));
```

---

## 📊 监控建议

### 1. 同步成功率监控

定期统计 ACMS 同步成功率：

```sql
-- 统计黑名单同步情况
SELECT 
    COUNT(*) as total_blacklist,
    SUM(CASE WHEN remark2 LIKE '%ACMS同步成功%' THEN 1 ELSE 0 END) as acms_synced,
    ROUND(SUM(CASE WHEN remark2 LIKE '%ACMS同步成功%' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as sync_rate
FROM black_list
WHERE park_name = '东北林业大学'
AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### 2. 失败记录追踪

查找 ACMS 同步失败的记录：

```sql
-- 查找可能同步失败的黑名单记录
SELECT 
    id, car_code, reason, park_name, created_at
FROM black_list
WHERE park_name = '东北林业大学'
AND (remark2 NOT LIKE '%ACMS同步成功%' OR remark2 IS NULL)
AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY created_at DESC;
```

### 3. 日志监控

定期检查日志文件：

```bash
# 统计ACMS同步成功次数
grep "黑名单同步ACMS成功" logs/application.log | wc -l

# 统计ACMS同步失败次数
grep "黑名单同步ACMS失败\|黑名单同步ACMS异常" logs/application.log | wc -l

# 查看最近的同步失败日志
grep "黑名单同步ACMS失败\|黑名单同步ACMS异常" logs/application.log | tail -20
```

---

## 🔐 安全建议

### 1. 敏感信息保护

- ❌ **不要**在代码中硬编码签名密钥
- ✅ **使用**配置文件或环境变量
- ✅ **加密**存储敏感配置

### 2. 接口调用频率控制

- 避免短时间内大量调用 ACMS 接口
- 建议实现重试机制，但要控制重试次数

### 3. 日志脱敏

- 不要在日志中输出完整的签名密钥
- 车主个人信息适当脱敏

---

## 📚 相关文档

- **ACMS接口文档：** [https://s.apifox.cn/a088c4fe-5b5c-49c9-901c-cd64316c7c11/288285170e0](https://s.apifox.cn/a088c4fe-5b5c-49c9-901c-cd64316c7c11/288285170e0)
- **黑名单功能完整实施指南：** `黑名单功能完整实施指南.md`
- **数据库迁移脚本：** `数据库迁移-黑名单功能.sql`
- **小程序使用说明：** `小程序黑名单功能使用说明.md`

---

## ✅ 检查清单

### 部署前检查

- [ ] 配置 ACMS API地址（acms.api.url）
- [ ] 配置设备ID（acms.api.device_id）
- [ ] 配置签名密钥（acms.api.sign）
- [ ] 测试 ACMS 接口连通性
- [ ] 确认东北林业大学车场名称拼写正确

### 功能测试

- [ ] 测试永久拉黑同步
- [ ] 测试临时拉黑同步
- [ ] 测试非东北林业大学车场（不同步）
- [ ] 测试ACMS异常情况（本地仍然保存）
- [ ] 查看日志输出是否正常

### 上线后监控

- [ ] 定期检查 ACMS 同步成功率
- [ ] 监控 ACMS 接口响应时间
- [ ] 处理同步失败的记录
- [ ] 收集用户反馈

---

**文档版本：** v1.0  
**创建时间：** 2025-10-04  
**最后更新：** 2025-10-04  
**维护人员：** System  
**联系方式：** 如有问题请联系系统管理员

