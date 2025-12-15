# 用户权限控制功能实现说明

## 功能概述

本功能实现了违规记录的用户权限控制，确保用户只能查看和操作自己添加的违规记录，提高数据安全性和隐私保护。

## 主要特性

### 1. 数据隔离
- ✅ 用户只能查看自己创建的违规记录
- ✅ 基于用户角色的权限控制
- ✅ 支持不同角色的权限等级

### 2. 角色权限
- **管理员 (manager)**: 可以查看和操作所有违规记录
- **管家 (housekeeper)**: 可以查看和操作所有违规记录  
- **巡逻员 (patrol)**: 只能查看和操作自己创建的记录
- **住户 (resident)**: 只能查看和操作自己创建的记录

### 3. 操作权限
- **查看**: 根据角色过滤数据
- **编辑**: 只能编辑自己创建的待处理记录
- **删除**: 管理员和管家可删除所有记录，普通用户只能删除自己的记录
- **统计**: 统计数据也基于权限过滤

## 技术实现

### 后端实现

#### 1. 数据库变更
```sql
-- 添加创建者字段
ALTER TABLE violations 
ADD COLUMN created_by VARCHAR(50) COMMENT '创建者ID';

-- 添加索引
CREATE INDEX idx_violations_created_by ON violations(created_by);
```

#### 2. 实体类修改
```java
// Violations.java
@ApiModelProperty(value = "创建者ID")
private String createdBy;
```

#### 3. 控制器权限控制
```java
// ViolationsController.java
private String getCurrentUserId(HttpServletRequest request) {
    // 从请求头或Token中获取用户ID
    String userId = request.getHeader("User-Id");
    // 或从JWT Token中解析
    return userId;
}

private String getUserRole(HttpServletRequest request) {
    // 从请求头或Token中获取用户角色
    String role = request.getHeader("User-Role");
    return role;
}
```

#### 4. 服务层过滤
```java
// ViolationsServiceImpl.java
public IPage<Map<String, Object>> getViolationsWithOwnerInfo(..., String createdByFilter) {
    // 根据createdByFilter参数过滤数据
    if (StringUtils.hasText(createdByFilter)) {
        // 只返回指定创建者的记录
    }
}

public boolean canUpdateViolation(Long violationId, String currentUserId, String userRole) {
    // 检查用户是否有权限更新指定记录
    if ("manager".equals(userRole) || "housekeeper".equals(userRole)) {
        return true; // 管理员和管家可以更新所有记录
    }
    // 普通用户只能更新自己创建的记录
    Violations violation = this.getById(violationId);
    return violation != null && currentUserId.equals(violation.getCreatedBy());
}
```

### 前端实现

#### 1. 用户身份管理
```javascript
// 获取当前用户信息
const userInfo = uni.getStorageSync('userInfo');
const currentUserId = userInfo?.userId || userInfo?.id;
const userRole = userInfo?.role;
```

#### 2. 提交时设置创建者
```javascript
// add-violation.vue
async submitToServer() {
    const userInfo = uni.getStorageSync('userInfo');
    const currentUserId = userInfo?.userId || userInfo?.id;
    
    const submitData = {
        // ... 其他数据
        createdBy: currentUserId, // 设置创建者ID
        reporterId: currentUserId
    };
}
```

#### 3. 权限控制页面
```javascript
// my-violations.vue
loadRecords() {
    const allRecords = uni.getStorageSync('user_violation_records') || [];
    // 筛选出当前用户的记录
    this.myRecords = allRecords.filter(record => {
        return record.createdBy === this.currentUserId;
    });
}
```

## 使用说明

### 1. 测试步骤

1. **添加违规记录**
   - 使用不同用户身份登录
   - 在 `pagesE/violation/add-violation.vue` 页面添加违规记录
   - 记录会自动关联到当前用户

2. **查看权限控制效果**
   - 访问 `pages/violation/my-violations.vue` 页面
   - 查看 "权限控制演示" 区域的统计信息
   - 验证只能看到自己创建的记录

3. **角色权限测试**
   - 修改用户角色（在 `userInfo` 中）
   - 测试不同角色的查看和操作权限

### 2. 配置说明

#### 用户信息存储格式
```javascript
const userInfo = {
    userId: "user_123",        // 用户ID
    id: "user_123",           // 备用ID字段
    name: "张三",             // 用户姓名
    role: "patrol",           // 用户角色：manager/housekeeper/patrol/resident
    phone: "13800138000"      // 联系电话
};

// 存储到本地
uni.setStorageSync('userInfo', userInfo);
```

#### 请求头配置（生产环境）
```javascript
// API请求时添加用户信息
uni.request({
    url: '/parking/violations',
    header: {
        'User-Id': currentUserId,
        'User-Role': userRole,
        'Authorization': 'Bearer ' + token
    }
});
```

## 安全注意事项

### 1. 前端安全
- ❌ 前端权限控制仅用于UI展示，不能作为安全边界
- ✅ 所有权限验证必须在后端进行
- ✅ 敏感操作需要二次验证

### 2. 后端安全
- ✅ 从可信来源（JWT Token）获取用户身份
- ✅ 所有API都必须进行权限检查
- ✅ SQL查询中添加权限过滤条件
- ✅ 避免通过前端传递的用户ID进行权限判断

### 3. 数据库安全
- ✅ 使用索引提高查询性能
- ✅ 定期审计权限配置
- ✅ 记录重要操作的日志

## 扩展功能

### 1. 审计日志
```java
// 记录用户操作日志
@Component
public class ViolationAuditLogger {
    public void logCreate(String userId, String violationId) {
        // 记录创建日志
    }
    
    public void logUpdate(String userId, String violationId, String operation) {
        // 记录更新日志
    }
}
```

### 2. 数据脱敏
```java
// 敏感数据脱敏
private String maskPhone(String phone) {
    if (phone != null && phone.length() >= 7) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    return phone;
}
```

### 3. 权限缓存
```java
// 使用Redis缓存用户权限
@Cacheable(value = "user_permissions", key = "#userId")
public UserPermission getUserPermissions(String userId) {
    // 获取用户权限配置
}
```

## 故障排除

### 1. 常见问题

**Q: 用户看不到任何记录？**
A: 检查用户身份信息是否正确存储，确认 `currentUserId` 与记录中的 `createdBy` 字段匹配。

**Q: 权限控制不生效？**
A: 确认后端API已正确实现权限过滤逻辑，检查请求头中的用户信息传递。

**Q: 数据库查询性能问题？**
A: 确认已在 `created_by` 字段上创建索引，考虑添加复合索引。

### 2. 调试方法
```javascript
// 前端调试
console.log('当前用户信息:', uni.getStorageSync('userInfo'));
console.log('用户记录数量:', this.myRecords.length);
console.log('全部记录数量:', this.allRecords.length);
```

```java
// 后端调试
log.info("当前用户: {}, 角色: {}", currentUserId, userRole);
log.info("查询条件: createdByFilter = {}", createdByFilter);
log.info("返回记录数量: {}", result.getTotal());
```

## 版本历史

- **v1.0** (2025-01-31): 初始版本，实现基本的用户权限控制
- 添加 `created_by` 字段支持
- 实现角色权限控制
- 前端权限验证和数据过滤

## 相关文件

### 后端文件
- `ViolationsController.java` - 控制器权限检查
- `ViolationsService.java` - 服务接口定义
- `ViolationsServiceImpl.java` - 权限控制逻辑实现
- `Violations.java` - 实体类（添加createdBy字段）

### 前端文件
- `add-violation.vue` - 违规添加页面（设置创建者）
- `my-violations.vue` - 个人违规记录查看页面
- `violation.vue` - 违规记录列表页面

### 数据库文件
- `add_created_by_column.sql` - 数据库迁移脚本

---

**注意**: 这是一个演示版本的权限控制实现。在生产环境中，建议使用更完善的权限管理框架（如Spring Security、Shiro等）和JWT认证机制。 