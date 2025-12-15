# 🚗 即将超时提醒功能 API 文档

## 📖 功能概述

当车辆停车时长还剩15分钟就要到达2小时时，系统会自动向访客的微信发送即将超时提醒，帮助访客及时处理车辆，避免超时。

## ✨ 核心特性

- ⏰ **智能检测**: 自动检测停车时长还剩15分钟到2小时的车辆
- 📱 **微信提醒**: 向访客微信发送模板消息提醒
- 🔄 **定时任务**: 每10分钟自动检查一次，无需手动干预
- 🚫 **防重复**: 避免对同一车辆重复发送提醒消息

## 🛠️ API 接口

### 1. 手动检查并发送即将超时提醒

**接口地址**: `POST /parking/timeout/check-and-send-almost-timeout`

**功能说明**: 手动触发检查即将超时的车辆并发送提醒

**请求参数**: 无

**响应示例**:
```json
{
    "code": "0",
    "msg": "成功",
    "data": {
        "totalChecked": 5,
        "successCount": 4,
        "failCount": 1,
        "results": [
            {
                "plateNumber": "粤B12345",
                "parkName": "凯达尔停车场",
                "remainingMinutes": 12,
                "visitorName": "张三",
                "sendResult": {
                    "success": true,
                    "message": "发送成功",
                    "msgid": 1234567890
                }
            }
        ]
    }
}
```

**响应字段说明**:
- `totalChecked`: 检查的车辆总数
- `successCount`: 成功发送提醒的数量
- `failCount`: 发送失败的数量
- `results`: 详细结果列表
  - `plateNumber`: 车牌号
  - `parkName`: 停车场名称
  - `remainingMinutes`: 剩余时间（分钟）
  - `visitorName`: 访客姓名
  - `sendResult`: 发送结果详情

### 2. 获取即将超时车辆列表

**接口地址**: `GET /parking/timeout/almost-timeout-vehicles`

**功能说明**: 查询当前即将超时的车辆列表（仅查询，不发送消息）

**请求参数**: 无

**响应示例**:
```json
{
    "code": "0",
    "msg": "成功",
    "data": [
        {
            "id": 123,
            "plateNumber": "粤B12345",
            "parkName": "凯达尔停车场",
            "enterTime": "2024-01-15 10:00:00",
            "parkingMinutes": 108,
            "remainingMinutes": 12,
            "visitorName": "张三",
            "openid": "owner_13593527970_1756796430797"
        }
    ]
}
```

## 📱 微信模板消息格式

### 模板ID: `NvxG4lJ8SSfhVh1cdK0Jachz9Fr383oAHWZfO29D2Ws`

### 模板编号: `45414`

### 模板内容:
```
停车超时通知

停车场名称：{{thing2.DATA}}
车牌号：{{car_number5.DATA}}
入场时间：{{time9.DATA}}
剩余时长：{{const13.DATA}}

您的车辆即将超过停车时限，请及时处理
```

### 字段映射:
- `thing2.DATA` → 停车场名称（如：凯达尔停车场）
- `car_number5.DATA` → 车牌号（如：粤B88888）
- `time9.DATA` → 入场时间（如：2024-01-15 10:00:00）
- `const13.DATA` → 剩余时长（如：15分钟）

## ⏰ 定时任务配置

### 1. 即将超时检查任务
- **执行频率**: 每10分钟
- **检查范围**: 停车时长105-119分钟的车辆（还剩1-15分钟到2小时）
- **发送对象**: 访客（通过openid）

### 2. 已超时检查任务
- **执行频率**: 每30分钟
- **检查范围**: 停车时长超过120分钟的车辆
- **发送对象**: 管家（通过管家昵称查询openid）

### 3. 清理任务
- **执行频率**: 每天凌晨2点
- **功能**: 清理过期的通知记录

## 🔧 数据库查询逻辑

### SQL查询（即将超时车辆）:
```sql
SELECT * FROM appointment
WHERE venuestatus = '已进场'
AND arrivedate IS NOT NULL
AND leavedate IS NULL
AND TIMESTAMPDIFF(MINUTE, arrivedate, NOW()) BETWEEN 105 AND 119
ORDER BY arrivedate ASC
```

### 查询条件说明:
- `venuestatus = '已进场'`: 车辆状态为已进场
- `arrivedate IS NOT NULL`: 有进场时间
- `leavedate IS NULL`: 未离场
- `TIMESTAMPDIFF(MINUTE, arrivedate, NOW()) BETWEEN 105 AND 119`: 停车时长在105-119分钟之间

## 🧪 测试步骤

### 1. 准备测试数据
```sql
-- 插入一条即将超时的测试数据
INSERT INTO appointment (
    platenumber, community, openid, visitorname, 
    venuestatus, arrivedate, visitdate
) VALUES (
    '测试001', '凯达尔停车场', 'test_openid_001', '测试用户',
    '已进场', DATE_SUB(NOW(), INTERVAL 110 MINUTE), NOW()
);
```

### 2. 手动触发检查
```bash
curl -X POST http://www.xuerparking.cn:8543/parking/timeout/check-and-send-almost-timeout \
  -H "Content-Type: application/json"
```

### 3. 查看日志
检查应用日志中的以下信息：
```
⏰ [定时任务] 开始检查即将超时车辆...
⚠️ [定时任务] 发现 1 辆车即将超时
✅ [定时任务] 即将超时提醒发送成功 - 车牌: 测试001, 剩余: 10分钟, 访客: 测试用户
```

### 4. 验证微信消息
检查对应微信是否收到模板消息。

## 🚨 注意事项

### 1. 微信配置要求
- 确保`application.yml`中配置了正确的微信公众号信息
- 模板ID必须在微信公众平台申请并审核通过
- 访客必须关注了对应的微信公众号

### 2. 数据库字段要求
- `appointment`表必须有`openid`字段存储访客微信openid
- `appointment`表必须有`visitorname`字段存储访客姓名
- `arrivedate`字段记录准确的进场时间

### 3. 性能考虑
- 定时任务默认每10分钟执行，可根据业务需求调整
- 建议实现通知记录机制，避免重复发送
- 对于大量数据，建议添加分页查询

### 4. 容错处理
- 如果微信接口调用失败，不会影响其他车辆的检查
- 每个车辆的处理都有独立的异常捕获
- 详细的日志记录便于问题排查

## 📊 监控指标

建议监控以下指标：
- 每次检查的车辆数量
- 成功发送的消息数量
- 失败发送的消息数量和原因
- 定时任务的执行时间
- 微信接口的响应时间

## 🔄 扩展建议

1. **通知记录表**: 创建专门的表记录通知历史，避免重复发送
2. **个性化配置**: 支持不同停车场配置不同的超时阈值
3. **多渠道通知**: 除微信外，支持短信、邮件等通知方式
4. **用户设置**: 允许用户自定义是否接收提醒
5. **数据统计**: 添加通知效果统计和分析功能

---

## 📝 更新日志

- **v1.0.0** (2024-01-15): 初始版本，支持基础的即将超时提醒功能
- **v1.1.0** (计划): 添加通知记录表，避免重复发送
- **v1.2.0** (计划): 支持个性化配置和多渠道通知 