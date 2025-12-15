# 违规通知功能修改说明

## 修改概述

根据需求，对违规记录添加时的通知功能进行了重要修改：

### 前端修改 (add-violation.vue)

1. **删除了前端直接调用发送通知的方法**
   - 移除了 `sendViolationNotification()` 方法
   - 删除了在提交成功后调用 `this.sendViolationNotification()` 的代码
   - 改为由后端在创建违规记录时自动发送通知

### 后端修改 (ViolationsController.java)

1. **注入了UserMappingMapper依赖**
   ```java
   @Resource
   private com.parkingmanage.mapper.UserMappingMapper userMappingMapper;
   ```

2. **重写了sendViolationNotification方法**
   - **原来**: 发送通知给管家
   - **现在**: 发送通知给预约记录中的访客 (visitorname)

3. **新的通知流程**:
   - 检查违规记录是否有`appointmentId`
   - 根据`appointmentId`查询预约记录详情
   - 从预约记录中获取`visitorname`
   - 通过`visitorname`查询`user_mapper`表获取`openid`
   - 使用`sendTemplateMessage`方法发送通知给访客

4. **添加了getOpenidByNickname方法**
   - 复用了ParkingTimeoutController中的逻辑
   - 根据昵称查询user_mapper表获取openid

## 功能变化

### 修改前
- 前端在提交违规记录后直接调用微信通知API
- 通知发送给管家

### 修改后
- 后端在创建违规记录时自动发送通知
- 通知发送给违规车辆对应预约记录中的访客
- 只有存在预约记录的违规才会发送通知

## 注意事项

1. **只对预约车辆发送通知**: 如果违规记录没有`appointmentId`，则不会发送通知
2. **需要user_mapper数据**: 访客的昵称必须在user_mapper表中有对应的openid记录
3. **模板ID**: 当前使用的模板ID为"parking_violation_template"，可根据实际情况调整

## 日志信息

修改后的代码包含详细的日志信息，便于调试：
- 查询预约记录详情的日志
- 查找访客姓名的日志
- 查询openid的日志
- 发送通知成功/失败的日志

## 兼容性

- 对于非预约车辆的违规记录，系统会跳过通知发送，不影响正常业务流程
- 如果访客昵称在user_mapper中不存在，会记录警告日志但不影响违规记录的创建 