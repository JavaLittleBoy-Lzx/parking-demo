# 月票车本地数据API后端实现文档

## 📋 概述

本文档介绍为支持前端月票车搜索功能本地化而新增的4个后端API接口。这些接口直接操作 `month_tick` 表，实现了数据的本地化存储和快速查询。

## 🛠️ 技术栈

- **框架**: Spring Boot + MyBatis Plus
- **数据库**: MySQL
- **响应格式**: 统一使用 `Result<T>` 格式
- **日志**: 控制台输出，包含emoji标识便于调试

## 📡 API接口详情

### 1. 检查车场数据是否存在

**接口信息**
- **URL**: `GET /parking/monthTicket/checkParkDataExists`
- **功能**: 检查指定车场是否已有月票车数据
- **使用场景**: 前端页面初始化时判断是否需要导入数据

**请求参数**
```http
GET /parking/monthTicket/checkParkDataExists?parkName=万象上东
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parkName | String | 是 | 车场名称 |

**响应格式**
```json
{
  "code": "0",
  "msg": "成功",
  "data": {
    "exists": true,
    "count": 1250,
    "lastUpdateTime": "2024-01-15 10:30:00"
  }
}
```

**响应字段说明**
- `exists`: 是否存在数据
- `count`: 该车场现有记录数
- `lastUpdateTime`: 最后更新时间（如果有数据）

---

### 2. 批量导入车场数据

**接口信息**
- **URL**: `POST /parking/monthTicket/batchImportParkData`
- **功能**: 批量导入指定车场的月票车数据到本地数据库
- **使用场景**: 自动数据同步和手动数据刷新

**请求参数**
```json
{
  "parkName": "万象上东",
  "forceUpdate": false
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parkName | String | 是 | 车场名称 |
| forceUpdate | Boolean | 否 | 是否强制更新，默认false |

**响应格式**
```json
{
  "code": "0",
  "msg": "成功",
  "data": {
    "totalImported": 1250,
    "newRecords": 1200,
    "updatedRecords": 50,
    "skippedRecords": 0,
    "importTime": 3500,
    "success": true,
    "message": "导入完成"
  }
}
```

**导入逻辑**
1. 如果 `forceUpdate=false` 且数据已存在，跳过导入
2. 调用外部API获取车场数据
3. 分页处理大量数据
4. 去重逻辑：相同车牌号+车场名称视为重复
5. 新记录插入，已存在记录更新

**支持的车场映射**
- "万象上东" → "2KST9MNP"
- "四季上东" → "2KUG6XLU"

---

### 3. 本地数据搜索

**接口信息**
- **URL**: `POST /parking/monthTicket/searchLocalData`
- **功能**: 直接查询本地month_tick表中的数据
- **使用场景**: 替代外部API的快速本地搜索

**请求参数**
```json
{
  "keyword": "川A12345",
  "parkName": "万象上东",
  "page": 1,
  "size": 20
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 车牌号关键词（模糊匹配） |
| parkName | String | 否 | 车场名称（精确匹配） |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |

**响应格式**
```json
{
  "code": "0",
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "parkName": "万象上东",
        "carNo": "川A12345",
        "userName": "张三",
        "userPhone": "13800138000",
        "ticketName": "普通月票",
        "validStatus": 1,
        "createTime": "2024-01-15 10:30:00"
      }
    ],
    "total": 150,
    "page": 1,
    "size": 20,
    "searchTime": 45
  }
}
```

**查询优化**
- 使用 `LIKE` 进行车牌号模糊匹配
- 利用复合索引 `idx_park_car(park_name, car_no)` 优化性能
- 支持分页查询，避免大结果集

---

### 4. 获取本地车牌建议

**接口信息**
- **URL**: `GET /parking/monthTicket/getLocalPlateSuggestions`
- **功能**: 获取本地数据库中匹配的车牌号建议列表
- **使用场景**: 搜索框自动补全功能

**请求参数**
```http
GET /parking/monthTicket/getLocalPlateSuggestions?keyword=川A1&parkName=万象上东&limit=10
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| parkName | String | 否 | 车场名称限制 |
| limit | Integer | 否 | 限制数量，默认10 |

**响应格式**
```json
{
  "code": "0",
  "msg": "成功",
  "data": {
    "suggestions": [
      {
        "plateNumber": "川A12345",
        "ownerName": "张三",
        "matchScore": 0.95
      },
      {
        "plateNumber": "川A16789",
        "ownerName": "李四",
        "matchScore": 0.90
      }
    ],
    "total": 25,
    "searchTime": 12
  }
}
```

**匹配分数算法**
- 完全匹配: 1.0
- 前缀匹配: 0.9
- 包含匹配: 0.8
- 字符匹配: 0.6 * (匹配字符数 / 关键词长度)

## 🗄️ 数据库设计

### 表结构
参考 `month_tick_table.sql` 文件，主要字段：

```sql
CREATE TABLE `month_tick` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `park_name` varchar(100) NOT NULL COMMENT '车场名称',
  `car_no` varchar(20) NOT NULL COMMENT '车牌号',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `user_phone` varchar(20) DEFAULT NULL COMMENT '用户手机号',
  `ticket_name` varchar(100) DEFAULT NULL COMMENT '月票名称',
  `valid_status` int(11) DEFAULT 1 COMMENT '有效状态',
  `create_time` varchar(30) DEFAULT NULL,
  `update_time` varchar(30) DEFAULT NULL,
  -- 其他字段...
  PRIMARY KEY (`id`),
  INDEX `idx_park_car` (`park_name`, `car_no`)
);
```

### 索引优化
```sql
-- 基础索引
INDEX `idx_park_name` (`park_name`)
INDEX `idx_car_no` (`car_no`)
INDEX `idx_park_car` (`park_name`, `car_no`)

-- 性能优化索引（可选）
INDEX `idx_search_optimize` (`park_name`, `car_no`, `valid_status`)
INDEX `idx_suggestions` (`car_no`, `park_name`, `user_name`)
```

## 🔧 错误处理

### 统一错误格式
```json
{
  "code": "-1",
  "msg": "错误信息",
  "data": null
}
```

### 常见错误情况

**1. 检查数据接口**
- 车场名称为空
- 数据库连接异常

**2. 导入数据接口**
- 未知车场名称
- 外部API调用失败
- 数据解析异常
- 数据库写入失败

**3. 搜索接口**
- 查询参数异常
- 数据库查询超时

**4. 建议接口**
- 关键词为空
- 查询结果为空

## 🚀 性能优化

### 1. 数据库层面
- **索引优化**: 针对常用查询添加复合索引
- **分页查询**: 避免大结果集影响性能
- **查询优化**: 使用 MyBatis Plus 的条件构造器

### 2. 应用层面
- **异步导入**: 批量导入使用分页处理
- **缓存机制**: 可考虑添加 Redis 缓存热门搜索
- **连接池**: 合理配置数据库连接池

### 3. 导入策略
- **增量导入**: 优先检查数据是否已存在
- **批量处理**: 每页100条记录，减少API调用次数
- **事务控制**: 确保数据一致性

## 📊 监控和日志

### 日志格式
```java
System.out.println("🔍 [本地搜索] 参数: keyword=" + keyword + ", parkName=" + parkName);
System.out.println("✅ [本地搜索] 完成: 找到" + total + "条记录，耗时" + time + "ms");
System.err.println("❌ [本地搜索] 失败: " + e.getMessage());
```

### 关键指标监控
- 搜索响应时间
- 导入成功率
- 数据同步准确性
- API调用频率

## 🧪 测试

### 单元测试建议
1. **数据检查接口测试**
   - 存在数据的车场
   - 不存在数据的车场
   - 无效车场名称

2. **导入接口测试**
   - 首次导入
   - 重复导入（forceUpdate=false）
   - 强制更新（forceUpdate=true）
   - 无效车场名称

3. **搜索接口测试**
   - 精确匹配
   - 模糊匹配
   - 空关键词
   - 分页查询

4. **建议接口测试**
   - 前缀匹配
   - 部分匹配
   - 无匹配结果
   - 限制数量

### 集成测试
使用提供的 `API测试文档.html` 进行完整的接口测试。

## 🔄 部署注意事项

### 1. 数据库准备
- 执行 `month_tick_table.sql` 创建表结构
- 确保索引创建完成
- 检查表字符集为 `utf8mb4`

### 2. 配置检查
- 数据库连接配置
- MyBatis Plus 配置
- 外部API访问权限

### 3. 初始数据
- 建议在部署后立即导入一次完整数据
- 设置定时任务定期同步数据

### 4. 监控告警
- 数据库连接监控
- API响应时间监控
- 错误日志监控

## 📞 技术支持

如遇到问题，请检查：
1. 数据库表结构是否正确
2. 索引是否创建完成
3. 外部API是否可访问
4. 日志中的具体错误信息

通过控制台日志可以快速定位问题，所有关键操作都有emoji标识的日志输出。 