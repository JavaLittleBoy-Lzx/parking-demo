# 巡逻员编码集成说明

## 概述

本次修改将巡逻员权限控制从硬编码方式改为从前端传递usercode参数的方式，使用正常的MyBatis-Plus查询替代了原生SQL查询。

## 主要修改内容

### 1. 新增Patrol实体类
- **文件**: `src/main/java/com/parkingmanage/entity/Patrol.java`
- **说明**: 对应数据库中的patrol表
- **字段**:
  - id: 主键
  - usercode: 巡逻员编码
  - name: 巡逻员姓名
  - province: 省份
  - city: 城市
  - district: 区县
  - community: 负责小区

### 2. 新增PatrolMapper
- **文件**: `src/main/java/com/parkingmanage/mapper/PatrolMapper.java`
- **说明**: 巡逻员表的Mapper接口

### 3. 修改ViolationsService接口
- **文件**: `src/main/java/com/parkingmanage/service/ViolationsService.java`
- **修改内容**:
  - `getPlateSuggestions(String keyword, String usercode)`: 添加usercode参数
  - `getAppointmentRecordsByOwnerInfo(String keyword, Integer page, Integer size, String usercode)`: 简化参数

### 4. 修改ViolationsServiceImpl实现类
- **文件**: `src/main/java/com/parkingmanage/service/impl/ViolationsServiceImpl.java`
- **主要修改**:
  - 添加PatrolMapper依赖注入
  - 使用MyBatis-Plus的LambdaQueryWrapper替代原生SQL查询
  - 删除getCurrentUserId()和getCurrentUserRole()方法
  - 修改权限控制逻辑，直接从参数获取usercode

### 5. 修改ViolationsController
- **文件**: `src/main/java/com/parkingmanage/controller/ViolationsController.java`
- **修改内容**:
  - `/owners/plate-suggestions`: 添加usercode参数
  - `/appointment-records-by-owner`: 简化参数，添加usercode参数

## 核心查询逻辑

### 原来的查询方式（已删除）
```java
// 使用原生SQL查询
QueryWrapper<Violations> patrolQuery = new QueryWrapper<>();
patrolQuery.apply("SELECT community FROM patrol WHERE usercode = {0} LIMIT 1", currentUserId);
List<Map<String, Object>> patrolResult = violationsMapper.selectMaps(patrolQuery);
```

### 新的查询方式
```java
// 使用MyBatis-Plus LambdaQueryWrapper
LambdaQueryWrapper<Patrol> patrolQuery = new LambdaQueryWrapper<>();
patrolQuery.eq(Patrol::getUsercode, usercode);
Patrol patrol = patrolMapper.selectOne(patrolQuery);
```

## 前端调用示例

### 车牌搜索建议
```javascript
// GET /parking/violations/owners/plate-suggestions?keyword=粤A&usercode=002
fetch('/parking/violations/owners/plate-suggestions', {
  method: 'GET',
  params: {
    keyword: '粤A',
    usercode: '002'  // 从小程序登录后获取的巡逻员编码
  }
})
```

### 预约记录查询
```javascript
// GET /parking/violations/appointment-records-by-owner?keyword=张三&page=1&size=20&usercode=002
fetch('/parking/violations/appointment-records-by-owner', {
  method: 'GET',
  params: {
    keyword: '张三',
    page: 1,
    size: 20,
    usercode: '002'  // 从小程序登录后获取的巡逻员编码
  }
})
```

## 小程序端集成步骤

### 1. 登录时保存usercode
```javascript
// 小程序登录成功后
wx.setStorageSync('usercode', '002');  // 保存巡逻员编码到本地存储
```

### 2. 接口调用时传递usercode
```javascript
// 从本地存储获取usercode
const usercode = wx.getStorageSync('usercode');

// 调用接口时传递usercode参数
wx.request({
  url: 'https://your-api.com/parking/violations/owners/plate-suggestions',
  data: {
    keyword: keyword,
    usercode: usercode
  },
  success: function(res) {
    // 处理返回结果
  }
});
```

## 权限控制逻辑

1. **有usercode参数**: 查询patrol表获取巡逻员负责的小区，只返回该小区的数据
2. **无usercode参数**: 返回所有数据（适用于管理员等其他角色）
3. **usercode无效**: 查询不到巡逻员信息时，返回空结果

## 数据库要求

确保patrol表中有正确的数据：
```sql
-- 示例数据
INSERT INTO patrol (id, usercode, name, community) 
VALUES (5, '002', '巡逻员002', '欧洲新城');
```

## 测试验证

### 1. 测试patrol表查询
```java
// 在Service中测试
LambdaQueryWrapper<Patrol> query = new LambdaQueryWrapper<>();
query.eq(Patrol::getUsercode, "002");
Patrol result = patrolMapper.selectOne(query);
System.out.println("查询结果: " + result);
```

### 2. 接口测试
- 测试URL: `GET /parking/violations/owners/plate-suggestions?keyword=粤&usercode=002`
- 预期结果: 只返回"欧洲新城"小区的车牌信息

## 注意事项

1. **向后兼容**: usercode参数设为可选，不传时不进行小区过滤
2. **数据安全**: 通过小区权限控制，确保巡逻员只能看到负责小区的数据
3. **错误处理**: 当usercode无效时，返回空结果而不是错误
4. **性能优化**: 使用MyBatis-Plus的Lambda查询，避免SQL注入风险 