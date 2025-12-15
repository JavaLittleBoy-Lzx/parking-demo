# 微信用户映射功能实现说明

## 功能概述
实现了微信公众号网页授权和事件推送的用户映射功能，自动管理用户的关注状态。

## 实现的功能

### 1. 网页授权用户信息存储
- **触发时机**：用户通过微信公众号网页授权获取到nickname和openid后
- **存储位置**：user_mapping表
- **默认状态**：is_followed = 0（未关注）
- **实现位置**：`WeChatAuthController.handleWebAuth()`方法

### 2. 关注事件处理
- **触发时机**：用户关注公众号时
- **更新操作**：
  - 如果用户已存在：更新is_followed = 1，设置follow_time
  - 如果用户不存在：创建新记录，从微信API获取完整用户信息
- **实现位置**：`WeChatEventServiceImpl.handleSubscribeEvent()`方法

### 3. 取消关注事件处理
- **触发时机**：用户取消关注公众号时
- **更新操作**：
  - 设置is_followed = 0
  - 设置unfollow_time
  - 清空敏感信息（昵称、头像、地理位置等）
- **实现位置**：`WeChatEventServiceImpl.handleUnsubscribeEvent()`方法

## 修改的文件

### 新增文件
1. `UserMappingService.java` - 用户映射服务接口
2. `UserMappingServiceImpl.java` - 用户映射服务实现类
3. `sql/user_mapping.sql` - 数据库表创建脚本

### 修改文件
1. `WeChatAuthController.java`
   - 注入UserMappingService
   - 在handleWebAuth方法中添加用户信息存储逻辑

2. `WeChatEventServiceImpl.java`
   - 注入UserMappingService
   - 修改关注/取消关注事件处理逻辑，使用Service层而不是直接使用Mapper

## 工作流程

1. **网页授权流程**：
   ```
   用户访问网页授权页面 → 获取nickname和openid → 存储到user_mapping表(is_followed=0)
   ```

2. **关注事件流程**：
   ```
   用户关注公众号 → 微信推送关注事件 → 更新/创建user_mapping记录(is_followed=1)
   ```

3. **取消关注事件流程**：
   ```
   用户取消关注 → 微信推送取消关注事件 → 更新user_mapping记录(is_followed=0，清空敏感信息)
   ```

## 使用方法

1. 先执行sql/user_mapping.sql创建数据库表
2. 重启应用服务
3. 配置微信公众号的事件推送URL指向：`/wechat/event`
4. 配置微信网页授权回调URL指向：`/parking/wechat/auth`

功能将自动运行，无需额外配置。 