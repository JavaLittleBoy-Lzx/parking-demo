# 微信模板消息配置说明

## 1. 配置文件设置

在 `application.yml` 中配置微信公众号信息：

```yaml
# 微信公众号配置
wechat:
  public:
    appid: your_wechat_appid          # 微信公众号AppID
    secret: your_wechat_secret        # 微信公众号AppSecret
  
  # 模板消息配置 - 需要在微信公众平台申请对应模板
  template:
    parking:
      enter: TEMPLATE_ID_1            # 停车进场通知模板ID
      leave: TEMPLATE_ID_2            # 停车离场通知模板ID
      timeout: TEMPLATE_ID_3          # 停车即将超时提醒模板ID
      violation: TEMPLATE_ID_4        # 车辆违规停车告警通知模板ID
    booking:
      pending: TEMPLATE_ID_5          # 预约车辆待审核提醒模板ID
```

## 2. 模板消息格式

### 2.1 违规停车告警通知

**模板标题**: 车辆违规停车告警通知

**模板内容**:
```
停车场: {{thing3.DATA}}
车牌号: {{car_number1.DATA}}
发生地点: {{thing6.DATA}}
停车时长: {{time7.DATA}}

请及时处理违规情况，维护停车秩序。
```

**字段说明**:
- `thing3`: 停车场名称 (最多20个字符)
- `car_number1`: 车牌号码
- `thing6`: 违规地点 (最多20个字符)  
- `time7`: 停车时长

### 2.2 预约车辆待审核提醒

**模板标题**: 预约车辆待审核提醒

**模板内容**:
```
停车场: {{thing1.DATA}}
车牌号: {{car_number2.DATA}}
预约人: {{thing3.DATA}}
联系电话: {{phone_number8.DATA}}

有新的预约申请，请及时审核处理。
```

**字段说明**:
- `thing1`: 停车场名称 (最多20个字符)
- `car_number2`: 车牌号码
- `thing3`: 预约人姓名 (最多20个字符)
- `phone_number8`: 联系电话

### 2.3 停车进场通知

**模板标题**: 停车进场通知

**模板内容**:
```
停车场: {{thing2.DATA}}
通道名称: {{thing21.DATA}}
车牌号: {{car_number1.DATA}}
入场时间: {{time4.DATA}}

车辆已成功进场，请注意管理。
```

### 2.4 停车离场通知

**模板标题**: 停车离场通知

**模板内容**:
```
停车场: {{thing2.DATA}}
车牌号: {{car_number1.DATA}}
通道名称: {{thing29.DATA}}
出场时间: {{time4.DATA}}
停车时长: {{thing9.DATA}}

车辆已离场，感谢配合管理。
```

### 2.5 停车即将超时提醒

**模板标题**: 停车即将超时提醒

**模板内容**:
```
停车场: {{thing2.DATA}}
车牌号: {{car_number5.DATA}}
入场时间: {{time9.DATA}}
剩余时长: {{const13.DATA}}

您的车辆即将超时，请及时处理。
```

## 3. 环境变量配置

可以通过环境变量设置配置：

```bash
# 微信公众号配置
export WECHAT_APPID=your_actual_appid
export WECHAT_SECRET=your_actual_secret

# 模板ID配置
export WECHAT_TEMPLATE_ENTER=your_enter_template_id
export WECHAT_TEMPLATE_LEAVE=your_leave_template_id
export WECHAT_TEMPLATE_TIMEOUT=your_timeout_template_id
export WECHAT_TEMPLATE_VIOLATION=your_violation_template_id
export WECHAT_TEMPLATE_BOOKING_PENDING=your_booking_template_id
```

## 4. 模板申请流程

1. 登录微信公众平台 (https://mp.weixin.qq.com/)
2. 进入【功能】-【模板消息】-【模板库】
3. 搜索对应的模板关键词
4. 添加模板并获取模板ID
5. 将模板ID配置到系统中

## 5. 用户映射表

确保数据库中有 `user_mapping` 表用于存储管家昵称和微信openid的对应关系：

```sql
CREATE TABLE user_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(50) NOT NULL COMMENT '管家昵称',
    openid VARCHAR(100) NOT NULL COMMENT '微信openid',
    phone VARCHAR(20) COMMENT '手机号',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nickname (nickname),
    INDEX idx_openid (openid)
);
```

## 6. 测试验证

可以通过以下接口测试微信通知功能：

```bash
# 测试违规停车通知
curl -X POST http://www.xuerparking.cn:8080/api/wechat/send/violation-notification \
  -H "Content-Type: application/json" \
  -d '{
    "plateNumber": "粤B12345",
    "parkName": "测试停车场",
    "violationLocation": "A区域",
    "parkingDuration": "2小时30分钟",
    "managerNickname": "管家张三"
  }'

# 测试预约待审核通知
curl -X POST http://www.xuerparking.cn:8080/api/wechat/send/booking-pending-notification \
  -H "Content-Type: application/json" \
  -d '{
    "plateNumber": "粤B12345", 
    "parkName": "测试停车场",
    "bookerName": "李四",
    "contactPhone": "13800138000",
    "managerNickname": "管家张三"
  }'
``` 