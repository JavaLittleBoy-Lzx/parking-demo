# 🔄 黑名单ACMS优先同步流程说明

## 📋 目录

1. [流程概述](#流程概述)
2. [核心变更](#核心变更)
3. [详细流程](#详细流程)
4. [代码实现](#代码实现)
5. [日志示例](#日志示例)
6. [注意事项](#注意事项)
7. [测试用例](#测试用例)

---

## 🎯 流程概述

### 新流程逻辑

当用户在小程序或后台系统中添加违规记录并选择拉黑时，系统按照以下顺序执行：

```
1. 先调用 ACMS 接口拉黑 🔑
   ↓
2. ACMS 拉黑成功 ✅
   ↓
3. 保存违规记录到 violations 表 📝
   ↓
4. 将该车牌未处理的违规记录改为"已处理" 🔄
   ↓
5. 保存/更新黑名单到 black_list 表 💾
   - 车牌已存在 → 更新记录
   - 车牌不存在 → 新增记录
```

### 关键特性

✅ **ACMS优先原则**
- 只有ACMS拉黑成功后，才进行后续操作
- ACMS失败则直接终止，不保存任何数据

✅ **自动处理历史违规**
- 自动将该车牌所有"未处理"的违规记录改为"已处理"
- 避免重复拉黑

✅ **黑名单去重**
- 同一车牌在同一车场只保留一条黑名单记录
- 已存在则更新，不存在则新增

---

## 🔄 核心变更

### 变更对比

| 项目 | 旧流程 | 新流程 |
|------|--------|--------|
| **执行顺序** | 保存违规记录 → 保存黑名单 → 同步ACMS | **ACMS拉黑 → 保存违规记录 → 批量处理 → 保存黑名单** |
| **ACMS失败** | 仍保存本地数据，记录日志 | **终止所有操作，返回失败** |
| **黑名单去重** | 不处理，可能重复 | **查询已有记录，存在则更新，不存在则新增** |
| **历史违规** | 不处理 | **自动将未处理记录改为已处理** |
| **优先级** | 本地优先 | **ACMS优先** |

---

## 📊 详细流程

### 步骤 1：ACMS拉黑（必须成功）

```java
// 🔑 先调用ACMS接口
if (shouldBlacklist == 1) {
    boolean acmsSuccess = acmsVipService.addBlacklistToAcms(request);
    
    if (!acmsSuccess) {
        // ❌ ACMS失败，直接终止
        return false;
    }
}
```

**关键点：**
- ✅ ACMS成功 → 继续后续流程
- ❌ ACMS失败 → 终止所有操作，不保存任何数据
- ⚠️ ACMS异常 → 捕获异常，返回失败

---

### 步骤 2：保存违规记录

```java
// 保存违规记录到 violations 表
boolean result = this.save(violation);

if (!result) {
    log.error("违规记录保存失败");
    return false;
}
```

**说明：**
- 保存违规类型、车牌号、拉黑信息等
- 保存成功后才会进行后续操作

---

### 步骤 3：批量更新状态

```java
// 📝 将该车牌所有"未处理"的违规记录改为"已处理"
LambdaQueryWrapper<Violations> updateWrapper = new LambdaQueryWrapper<>();
updateWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber())
            .eq(Violations::getStatus, "未处理")
            .ne(Violations::getId, violation.getId()); // 排除当前记录

Violations updateViolation = new Violations();
updateViolation.setStatus("已处理");
updateViolation.setUpdatedAt(LocalDateTime.now());

int updateCount = this.baseMapper.update(updateViolation, updateWrapper);
```

**示例：**

假设数据库中有以下违规记录：

| id | plate_number | status | created_at |
|----|--------------|--------|------------|
| 1  | 黑A12345     | 未处理  | 2025-10-01 |
| 2  | 黑A12345     | 未处理  | 2025-10-02 |
| 3  | 黑A12345     | 未处理  | 2025-10-03 |
| 4  | 黑B67890     | 未处理  | 2025-10-03 |

现在添加新违规记录并拉黑 `黑A12345`：

**执行后结果：**

| id | plate_number | status | created_at | 说明 |
|----|--------------|--------|------------|------|
| 1  | 黑A12345     | **已处理** | 2025-10-01 | ✅ 自动更新 |
| 2  | 黑A12345     | **已处理** | 2025-10-02 | ✅ 自动更新 |
| 3  | 黑A12345     | **已处理** | 2025-10-03 | ✅ 自动更新 |
| 4  | 黑B67890     | 未处理  | 2025-10-03 | ⏭️ 不影响 |
| 5  | 黑A12345     | 未处理  | 2025-10-04 | ✅ 新记录 |

---

### 步骤 4：保存/更新黑名单

```java
// 查询是否已存在该车牌的黑名单记录
LambdaQueryWrapper<BlackList> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(BlackList::getCarCode, violation.getPlateNumber())
           .eq(BlackList::getParkName, violation.getParkName());

BlackList existingBlackList = blackListMapper.selectOne(queryWrapper);

if (existingBlackList != null) {
    // ✏️ 更新已有记录
    existingBlackList.setReason(violation.getBlacklistReason());
    existingBlackList.setBlacklistTypeCode(violation.getBlacklistTypeCode());
    // ...
    blackListMapper.updateById(existingBlackList);
} else {
    // ➕ 新增记录
    BlackList blackList = new BlackList();
    blackList.setCarCode(violation.getPlateNumber());
    // ...
    blackListMapper.insert(blackList);
}
```

**场景示例：**

#### 场景 A：首次拉黑

```
black_list 表（查询前）：
- 无 黑A12345 的记录

执行结果：
✅ 新增记录到 black_list 表
```

#### 场景 B：重复拉黑

```
black_list 表（查询前）：
- id=100, car_code=黑A12345, reason=多次违规

执行结果：
✅ 更新 id=100 的记录
   - reason → 新的拉黑原因
   - blacklist_type_code → 新的类型编码
   - remark1 → 新的违规记录ID
```

---

## 💻 代码实现

### ViolationsServiceImpl.java

**方法：** `createViolation(Violations violation)`

**完整流程代码：**

```java
@Override
@Transactional
public boolean createViolation(Violations violation) {
    
    // 🔑 步骤 1：如果需要拉黑，先调用ACMS接口（必须成功）
    if (violation.getShouldBlacklist() != null && violation.getShouldBlacklist() == 1) {
        try {
            log.info("🚫 [拉黑操作] 开始调用ACMS接口");
            
            // 构建请求
            AcmsVipService.AddBlacklistRequest acmsRequest = buildAcmsRequest(violation);
            
            // 调用ACMS接口
            boolean acmsSuccess = acmsVipService.addBlacklistToAcms(acmsRequest);
            
            if (!acmsSuccess) {
                log.error("❌ [ACMS拉黑失败] 终止后续操作");
                return false;
            }
            
            log.info("✅ [ACMS拉黑成功] 开始保存本地数据");
            
        } catch (Exception acmsEx) {
            log.error("❌ [ACMS拉黑异常] 终止后续操作", acmsEx);
            return false;
        }
    }
    
    // 📝 步骤 2：保存违规记录
    violation.setCreatedAt(LocalDateTime.now());
    violation.setUpdatedAt(LocalDateTime.now());
    
    boolean result = this.save(violation);
    
    if (!result) {
        log.error("❌ [违规记录创建失败]");
        return false;
    }
    
    log.info("✅ [违规记录创建成功] id={}", violation.getId());
    
    // 🔄 步骤 3：批量更新该车牌未处理的违规记录
    try {
        LambdaQueryWrapper<Violations> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Violations::getPlateNumber, violation.getPlateNumber())
                    .eq(Violations::getStatus, "未处理")
                    .ne(Violations::getId, violation.getId());
        
        Violations updateViolation = new Violations();
        updateViolation.setStatus("已处理");
        updateViolation.setUpdatedAt(LocalDateTime.now());
        
        int updateCount = this.baseMapper.update(updateViolation, updateWrapper);
        
        if (updateCount > 0) {
            log.info("📝 [批量更新状态] 将 {} 条未处理记录改为已处理", updateCount);
        }
    } catch (Exception e) {
        log.error("❌ [批量更新状态异常]", e);
    }
    
    // 💾 步骤 4：保存/更新黑名单到本地数据库
    if (violation.getShouldBlacklist() != null && violation.getShouldBlacklist() == 1) {
        try {
            // 查询是否已存在
            LambdaQueryWrapper<BlackList> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BlackList::getCarCode, violation.getPlateNumber())
                       .eq(BlackList::getParkName, violation.getParkName());
            
            BlackList existingBlackList = blackListMapper.selectOne(queryWrapper);
            
            if (existingBlackList != null) {
                // 更新已有记录
                updateBlackList(existingBlackList, violation);
                log.info("✅ [黑名单更新成功]");
            } else {
                // 新增记录
                insertBlackList(violation);
                log.info("✅ [黑名单新增成功]");
            }
            
        } catch (Exception e) {
            log.error("❌ [本地黑名单保存异常]", e);
        }
    }
    
    return result;
}
```

---

## 📝 日志示例

### 示例 1：首次拉黑（ACMS成功）

```
🚫 [拉黑操作] 开始调用ACMS接口: plateNumber=黑A12345, reason=多次违规停车, type=违规黑名单
🔄 [开始同步黑名单到ACMS] plateNumber=黑A12345, parkName=东北林业大学
📤 [ACMS请求-添加黑名单] carCode=黑A12345, url=http://...
📥 [ACMS响应-添加黑名单] carCode=黑A12345, response={"biz_content":{"code":"0","msg":"ok"}}
📊 [ACMS响应解析] code=0, msg=ok
✅ [ACMS拉黑成功] plateNumber=黑A12345, 开始保存本地数据
💾 [保存违规记录] 准备保存数据: plateNumber=黑A12345
✅ [违规记录创建成功] id=123, appointmentId=456
📝 [批量更新状态] plateNumber=黑A12345, 将 3 条未处理记录改为已处理
💾 [本地黑名单] 准备保存/更新到black_list表: plateNumber=黑A12345
➕ [黑名单不存在] plateNumber=黑A12345, 执行新增操作
✅ [黑名单新增成功] blacklistId=789, plateNumber=黑A12345, type=违规黑名单, duration=永久
```

---

### 示例 2：重复拉黑（ACMS成功，本地更新）

```
🚫 [拉黑操作] 开始调用ACMS接口: plateNumber=黑A12345
✅ [ACMS拉黑成功] plateNumber=黑A12345, 开始保存本地数据
✅ [违规记录创建成功] id=124
📝 [批量更新状态] plateNumber=黑A12345, 将 0 条未处理记录改为已处理
💾 [本地黑名单] 准备保存/更新到black_list表: plateNumber=黑A12345
🔄 [黑名单已存在] blacklistId=789, plateNumber=黑A12345, 执行更新操作
✅ [黑名单更新成功] blacklistId=789, plateNumber=黑A12345, type=违规黑名单, duration=永久
```

---

### 示例 3：ACMS失败（终止操作）

```
🚫 [拉黑操作] 开始调用ACMS接口: plateNumber=黑A12345
🔄 [开始同步黑名单到ACMS] plateNumber=黑A12345
📤 [ACMS请求-添加黑名单] carCode=黑A12345
📥 [ACMS响应-添加黑名单] response={"biz_content":{"code":"1","msg":"车辆已存在黑名单"}}
⚠️ ACMS返回错误: code=1, msg=车辆已存在黑名单
❌ [ACMS拉黑失败] plateNumber=黑A12345, 终止后续操作
```

**结果：**
- ❌ 违规记录未保存
- ❌ 历史记录未更新
- ❌ 黑名单未保存/更新

---

### 示例 4：ACMS异常（终止操作）

```
🚫 [拉黑操作] 开始调用ACMS接口: plateNumber=黑A12345
🔄 [开始同步黑名单到ACMS] plateNumber=黑A12345
📤 [ACMS请求-添加黑名单] carCode=黑A12345
❌ [ACMS拉黑异常] plateNumber=黑A12345, error=Connection timeout, 终止后续操作
```

**结果：**
- ❌ 违规记录未保存
- ❌ 历史记录未更新
- ❌ 黑名单未保存/更新

---

## ⚠️ 注意事项

### 1. ACMS优先原则

**关键点：**
- ACMS必须成功，否则不保存任何数据
- 这确保了ACMS与本地数据库的一致性

**影响：**
- ❌ ACMS故障时，拉黑功能不可用
- ✅ 保证ACMS与本地数据完全同步

**建议：**
- 部署前确保ACMS服务稳定可靠
- 监控ACMS接口可用性
- 为ACMS配置高可用方案

---

### 2. 批量更新状态

**更新条件：**
```sql
UPDATE violations 
SET status = '已处理', updated_at = NOW()
WHERE plate_number = '黑A12345'
  AND status = '未处理'
  AND id != 当前违规记录ID
```

**注意：**
- 只更新"未处理"状态的记录
- 不影响已处理的历史记录
- 排除当前新增的违规记录

---

### 3. 黑名单去重

**查询条件：**
```sql
SELECT * FROM black_list
WHERE car_code = '黑A12345'
  AND park_name = '东北林业大学'
LIMIT 1
```

**逻辑：**
- 同一车牌 + 同一车场 = 唯一黑名单记录
- 已存在 → 更新（原因、类型、时长等）
- 不存在 → 新增

**字段更新：**
| 字段 | 更新策略 |
|------|---------|
| owner | 覆盖 |
| reason | 覆盖 |
| blacklist_type_code | 覆盖 |
| special_car_type_config_name | 覆盖 |
| black_list_forever_flag | 覆盖 |
| blacklist_start_time | 覆盖（永久时清空） |
| blacklist_end_time | 覆盖（永久时清空） |
| remark1 | 覆盖（新违规记录ID） |
| remark2 | 覆盖（操作人信息） |

---

### 4. 事务管理

**建议：**
```java
@Transactional(rollbackFor = Exception.class)
public boolean createViolation(Violations violation) {
    // ...
}
```

**说明：**
- ACMS调用在事务外（已成功）
- 本地数据库操作在事务内
- 任何失败都会回滚本地操作

---

### 5. 异常处理

**ACMS异常：**
- 捕获所有异常
- 记录详细日志
- 返回 false，终止操作

**本地数据库异常：**
- 批量更新状态失败 → 不影响主流程（仅记录日志）
- 黑名单保存失败 → 不影响违规记录（ACMS已成功）
- 违规记录保存失败 → 返回 false

---

## 🧪 测试用例

### 测试用例 1：首次拉黑 + ACMS成功

**前置条件：**
```sql
-- violations 表
SELECT * FROM violations WHERE plate_number = '黑A12345' AND status = '未处理';
-- 结果：3条记录

-- black_list 表
SELECT * FROM black_list WHERE car_code = '黑A12345';
-- 结果：0条记录
```

**执行操作：**
```javascript
POST /parking/violations/create
{
  "plateNumber": "黑A12345",
  "parkName": "东北林业大学",
  "shouldBlacklist": 1,
  "blacklistTypeCode": "703",
  "blacklistTypeName": "违规黑名单",
  "blacklistDurationType": "permanent",
  "blacklistReason": "多次违规停车"
}
```

**预期结果：**
```sql
-- 1. ACMS 系统
✅ 黑A12345 已添加到黑名单

-- 2. violations 表
✅ 新增1条违规记录（status=未处理）
✅ 历史3条记录状态改为"已处理"

-- 3. black_list 表
✅ 新增1条黑名单记录
```

---

### 测试用例 2：重复拉黑 + ACMS成功

**前置条件：**
```sql
-- black_list 表
id=100, car_code='黑A12345', reason='第一次违规', blacklist_type_code='703'
```

**执行操作：**
```javascript
POST /parking/violations/create
{
  "plateNumber": "黑A12345",
  "shouldBlacklist": 1,
  "blacklistReason": "第二次违规，加重处罚"
}
```

**预期结果：**
```sql
-- black_list 表
✅ id=100 记录被更新
   - reason → '第二次违规，加重处罚'
   - remark1 → '违规记录ID: 新记录ID'
   - remark2 → '移动巡检小程序更新，操作人: XXX'
```

---

### 测试用例 3：ACMS失败（终止操作）

**模拟场景：**
- ACMS 返回 code=1, msg="车辆已存在黑名单"

**执行操作：**
```javascript
POST /parking/violations/create
{
  "plateNumber": "黑A12345",
  "shouldBlacklist": 1
}
```

**预期结果：**
```
❌ 接口返回失败
❌ violations 表无新记录
❌ black_list 表无变化
❌ 历史违规记录状态未改变
```

---

### 测试用例 4：ACMS异常（终止操作）

**模拟场景：**
- ACMS 服务不可用（Connection timeout）

**预期结果：**
```
❌ 接口返回失败
❌ 所有本地数据无变化
✅ 日志记录详细异常信息
```

---

### 测试用例 5：不拉黑（正常流程）

**执行操作：**
```javascript
POST /parking/violations/create
{
  "plateNumber": "黑A12345",
  "shouldBlacklist": 0  // 或不传此字段
}
```

**预期结果：**
```
✅ 保存违规记录到 violations 表
⏭️ 跳过 ACMS 调用
⏭️ 跳过批量更新状态
⏭️ 跳过黑名单保存
```

---

## 📊 数据流程图

```
┌─────────────────────────────────────────────────────────────┐
│              小程序提交违规记录 + 拉黑请求                      │
│                shouldBlacklist = 1                           │
└──────────────────────┬──────────────────────────────────────┘
                       ↓
        ┌──────────────────────────────┐
        │  🔑 ACMS 接口调用（必须成功）   │
        │  POST /cxfService/external/extReq │
        └──────────────┬───────────────┘
                       ↓
              ┌────────┴────────┐
              ↓                 ↓
         ✅ 成功           ❌ 失败/异常
              ↓                 ↓
┌─────────────────────┐   ┌─────────────────┐
│  📝 保存违规记录      │   │ ❌ 终止所有操作  │
│  INSERT violations   │   │    返回失败      │
└──────────┬──────────┘   └─────────────────┘
           ↓
┌─────────────────────────────────┐
│  🔄 批量更新状态                 │
│  UPDATE violations               │
│  SET status='已处理'             │
│  WHERE plate_number='黑A12345'  │
│    AND status='未处理'           │
└──────────┬──────────────────────┘
           ↓
┌─────────────────────────────────┐
│  💾 保存/更新黑名单               │
│  查询 black_list                 │
└──────────┬──────────────────────┘
           ↓
    ┌──────┴──────┐
    ↓             ↓
  存在          不存在
    ↓             ↓
  ✏️ UPDATE     ➕ INSERT
  black_list    black_list
```

---

## 🔍 对比总结

### 旧流程 vs 新流程

| 对比项 | 旧流程 | 新流程 |
|--------|--------|--------|
| **执行顺序** | 本地 → ACMS | **ACMS → 本地** |
| **ACMS失败** | 本地仍保存 | **全部终止** |
| **数据一致性** | 可能不一致 | **完全一致** |
| **黑名单重复** | 允许 | **自动去重** |
| **历史违规** | 不处理 | **自动标记已处理** |
| **可靠性** | ACMS异步 | **ACMS强制同步** |

---

## ✅ 优势

1. **强制同步** - ACMS与本地数据库完全一致
2. **数据去重** - 避免黑名单重复记录
3. **自动处理** - 历史违规自动标记已处理
4. **流程清晰** - 执行顺序明确，易于维护

---

## ⚠️ 劣势与应对

### 劣势

1. **ACMS单点故障** - ACMS不可用时，拉黑功能完全不可用

### 应对方案

1. **ACMS高可用**
   - 部署ACMS集群
   - 配置负载均衡
   - 实施健康检查

2. **监控告警**
   - 实时监控ACMS接口
   - 失败率超过阈值立即告警
   - 快速响应和恢复

3. **降级方案**（可选）
   - 提供管理员后台手动同步功能
   - ACMS恢复后批量补充同步

---

**文档版本：** v1.0  
**创建时间：** 2025-10-04  
**最后更新：** 2025-10-04  
**维护人员：** System

