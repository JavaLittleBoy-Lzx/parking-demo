# 🔧 字段查询为空问题修复

## ❌ 问题描述

查询违规类型接口时，以下字段返回为空：
- `severityLevel` (严重程度)
- `icon` (图标)
- `description` (描述)

## 🔍 问题原因

**根本原因**：MyBatis Mapper XML 文件中的 SQL 查询语句没有包含这些字段。

### 问题代码位置
文件：`parking-demo/src/main/java/com/parkingmanage/mapper/xml/ViolationTypeMapper.xml`

**修改前的 SQL**：
```xml
<select id="selectTypePage" resultMap="BaseResultMap">
    SELECT
        id, type_name, type_code, park_name,
        sort_order, is_enabled,                    ⬅️ 缺少 severity_level, icon, description
        created_at, updated_at
    FROM violation_types
    ...
</select>
```

虽然 `BaseResultMap` 中定义了字段映射关系，但如果 SQL 查询语句本身不查询这些字段，MyBatis 就无法将数据映射到实体类属性中。

## ✅ 解决方案

### 修改内容

在所有查询语句中添加缺失的字段：`severity_level`, `icon`, `description`

### 1. 修改 resultMap（已包含 icon 映射）

```xml
<resultMap id="BaseResultMap" type="com.parkingmanage.entity.ViolationType">
    <id column="id" property="id"/>
    <result column="type_name" property="typeName"/>
    <result column="type_code" property="typeCode"/>
    <result column="park_name" property="parkName"/>
    <result column="severity_level" property="severityLevel"/>
    <result column="icon" property="icon"/>                      ⬅️ 添加 icon 映射
    <result column="description" property="description"/>
    <result column="sort_order" property="sortOrder"/>
    <result column="is_enabled" property="isEnabled"/>
    <result column="created_by" property="createdBy"/>
    <result column="created_at" property="createdAt"/>
    <result column="updated_at" property="updatedAt"/>
</resultMap>
```

### 2. 修改所有 SELECT 语句

#### selectTypePage (分页查询)
```xml
<select id="selectTypePage" resultMap="BaseResultMap">
    SELECT
        id, type_name, type_code, park_name,
        severity_level, icon, description,         ⬅️ 添加这三个字段
        sort_order, is_enabled,
        created_at, updated_at
    FROM violation_types
    ...
</select>
```

#### selectEnabledTypes (查询启用的类型)
```xml
<select id="selectEnabledTypes" resultMap="BaseResultMap">
    SELECT
        id, type_name, type_code, park_name,
        severity_level, icon, description,         ⬅️ 添加这三个字段
        sort_order, is_enabled,
        created_at, updated_at
    FROM violation_types
    ...
</select>
```

#### selectByCodeAndPark (根据代码查询)
```xml
<select id="selectByCodeAndPark" resultMap="BaseResultMap">
    SELECT
        id, type_name, type_code, park_name,
        severity_level, icon, description,         ⬅️ 添加这三个字段
        sort_order, is_enabled,
        created_at, updated_at
    FROM violation_types
    ...
</select>
```

## 🚀 应用修复

### 步骤 1: 确认数据库有数据

```sql
-- 检查数据库中是否有这些字段的数据
SELECT id, type_name, severity_level, icon, description 
FROM violation_types 
LIMIT 5;
```

如果字段为空，需要先更新数据：
```sql
-- 示例：更新现有数据
UPDATE violation_types 
SET 
    severity_level = 'moderate',
    icon = 'WarningFilled',
    description = '在禁止停车区域停放车辆'
WHERE type_code = 'illegal_parking';
```

### 步骤 2: 重新编译项目

```bash
cd parking-demo
mvn clean compile
```

或者在 IDE 中重新构建项目。

### 步骤 3: 重启后端服务

重启 Spring Boot 应用以加载更新后的 Mapper 配置。

### 步骤 4: 验证修复

#### 方法 1: 使用 curl 测试
```bash
curl -X GET "http://www.xuerparking.cn:8081/parking/violation-config/types?page=1&size=10"
```

#### 方法 2: 查看浏览器 Network
1. 打开前端页面
2. F12 打开开发者工具
3. 查看 Network 标签中的 API 响应

**预期响应**：
```json
{
  "code": "0",
  "msg": "查询成功",
  "data": {
    "records": [
      {
        "id": 1,
        "typeName": "违规停车",
        "typeCode": "illegal_parking",
        "severityLevel": "moderate",        ✅ 不再为空
        "icon": "WarningFilled",            ✅ 不再为空
        "description": "在禁止停车区域停放车辆",  ✅ 不再为空
        ...
      }
    ]
  }
}
```

## 📋 修改文件清单

1. ✅ `parking-demo/src/main/java/com/parkingmanage/entity/ViolationType.java`
   - 添加 `icon` 字段

2. ✅ `parking-demo/src/main/java/com/parkingmanage/mapper/xml/ViolationTypeMapper.xml`
   - 在 `BaseResultMap` 中添加 `icon` 映射
   - 在 `selectTypePage` 中添加三个字段
   - 在 `selectEnabledTypes` 中添加三个字段
   - 在 `selectByCodeAndPark` 中添加三个字段

3. ✅ `parking-demo/sql/add_icon_to_violation_types.sql`
   - 数据库添加 `icon` 字段的脚本

## 💡 经验总结

### MyBatis 字段映射的三个要素

要让 MyBatis 正确返回字段数据，需要满足以下**三个条件**：

1. **数据库表有该字段**
   ```sql
   ALTER TABLE violation_types ADD COLUMN icon VARCHAR(50);
   ```

2. **Java 实体类有对应属性**
   ```java
   private String icon;
   ```

3. **Mapper XML 的 SQL 查询包含该字段**
   ```xml
   SELECT id, type_name, icon FROM violation_types
   ```

**缺少任何一个条件，字段都会返回为空！**

### 常见错误

❌ **错误 1**: 只修改了实体类，没修改 Mapper XML
```java
// 实体类添加了字段
private String icon;

// 但 SQL 没查询该字段
SELECT id, type_name FROM violation_types  ⬅️ 缺少 icon
```

❌ **错误 2**: 只修改了 resultMap，没修改 SELECT 语句
```xml
<!-- resultMap 有映射 -->
<result column="icon" property="icon"/>

<!-- 但 SELECT 没查询 -->
<select id="selectTypePage">
    SELECT id, type_name FROM violation_types  ⬅️ 缺少 icon
</select>
```

✅ **正确做法**: 三者保持一致
```sql
-- 1. 数据库有字段
ALTER TABLE violation_types ADD COLUMN icon VARCHAR(50);
```

```java
// 2. 实体类有属性
private String icon;
```

```xml
<!-- 3. SQL 查询该字段 -->
<select id="selectTypePage">
    SELECT id, type_name, icon FROM violation_types
</select>
```

## 🔍 调试技巧

### 1. 开启 MyBatis SQL 日志

在 `application.yml` 或 `application.properties` 中添加：

```yaml
# application.yml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

或

```properties
# application.properties
mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
```

这样可以在控制台看到实际执行的 SQL 语句，检查是否包含所需字段。

### 2. 使用数据库客户端直接查询

```sql
-- 执行 Mapper 中的 SQL，查看返回结果
SELECT
    id, type_name, type_code, park_name,
    severity_level, icon, description,
    sort_order, is_enabled,
    created_at, updated_at
FROM violation_types
ORDER BY sort_order ASC
LIMIT 10;
```

### 3. 检查字段命名规范

MyBatis 会自动将下划线命名转换为驼峰命名（需要开启配置）：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true  # 开启自动转换
```

- 数据库字段：`severity_level`
- Java 属性：`severityLevel`
- 自动映射：✅

如果没有开启自动转换，需要在 resultMap 中显式指定映射关系。

## ✅ 验证清单

修复完成后，确认以下所有项：

- [ ] 数据库表有 `severity_level`, `icon`, `description` 字段
- [ ] Java 实体类有对应的属性
- [ ] Mapper XML 的 resultMap 包含字段映射
- [ ] 所有 SELECT 语句都查询了这些字段
- [ ] 项目重新编译成功
- [ ] 后端服务重启成功
- [ ] API 响应包含这些字段且有值
- [ ] 前端页面正确显示这些字段

---

**修复日期**: 2025-10-08  
**问题严重程度**: 中等  
**影响范围**: 违规类型查询接口  
**修复状态**: ✅ 已完成
