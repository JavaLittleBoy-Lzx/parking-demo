# 房间数据提取报告

## 执行摘要

成功从两个月票订单SQL文件中提取房间信息数据，生成用于插入到rooms表的SQL语句。

## 数据来源

1. **文件1**: `月票订单_20251121214212571001_final.sql`
   - 提取记录数: 4,725条

2. **文件2**: `月票订单_20251121214212571001_manual_fixed.sql`
   - 提取记录数: 102条

## 数据处理结果

- **合并总记录数**: 4,827条
- **去重后记录数**: 3,357条唯一房间
- **输出文件**: `room_data_import.sql`

## 提取字段

从原始SQL文件中提取了以下字段：

| 字段名 | 说明 | 示例 |
|--------|------|------|
| province | 省份 | 黑龙江省 |
| city | 城市 | 哈尔滨市 |
| district | 区域 | 道里区 |
| community | 小区 | 欧洲新城 |
| building | 楼栋 | 香榭丽舍、白金汉、巴塞罗纳等 |
| units | 单元 | A、B、C、D等 |
| floor | 楼层 | 1-20, -1(地下一层)等 |
| roomnumber | 房间号 | 101、2004等 |

## 目标表结构

生成的SQL语句适用于以下表结构：

```sql
CREATE TABLE rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    province VARCHAR(255),
    city VARCHAR(255),
    district VARCHAR(255),
    community VARCHAR(255),
    building VARCHAR(255),
    units INT,
    floor INT,
    roomnumber INT,
    is_audit VARCHAR(2),        -- 设置为NULL
    audit_start_time VARCHAR(255), -- 设置为NULL
    audit_end_time VARCHAR(255)    -- 设置为NULL
);
```

## 数据特点

1. **审核状态**: 所有记录的`is_audit`、`audit_start_time`、`audit_end_time`均设置为NULL
2. **地理位置**: 所有记录均位于"黑龙江省 - 哈尔滨市 - 道里区 - 欧洲新城"
3. **楼栋分布**: 包含多个楼栋：香榭丽舍、白金汉、巴塞罗纳、凡尔赛、维也纳、佛罗伦萨等
4. **去重逻辑**: 基于(province, city, district, community, building, units, floor, roomnumber)的组合去重

## 数据示例

```sql
INSERT INTO rooms (province, city, district, community, building, units, floor, roomnumber, is_audit, audit_start_time, audit_end_time)
VALUES ('黑龙江省', '哈尔滨市', '道里区', '欧洲新城', '香榭丽舍', 'B', '1', '2004', NULL, NULL, NULL);

INSERT INTO rooms (province, city, district, community, building, units, floor, roomnumber, is_audit, audit_start_time, audit_end_time)
VALUES ('黑龙江省', '哈尔滨市', '道里区', '欧洲新城', '白金汉', 'A', '10', '1601', NULL, NULL, NULL);
```

## 使用说明

1. **导入数据库**:
   ```bash
   mysql -u your_username -p parking_management < room_data_import.sql
   ```

2. **验证导入**:
   ```sql
   SELECT COUNT(*) FROM rooms WHERE is_audit IS NULL;
   -- 应返回 3,357
   ```

3. **检查数据**:
   ```sql
   SELECT building, COUNT(*) as count 
   FROM rooms 
   GROUP BY building 
   ORDER BY count DESC;
   ```

## 注意事项

⚠️ **重要提示**:
- 目标表中的`units`、`floor`、`roomnumber`字段类型为`int`，但源数据中可能包含非数字值（如'未知'、'A'等）
- 建议先检查目标表结构，确保字段类型匹配
- 如果表结构中units、floor、roomnumber是varchar类型，则可以直接导入
- 如果是int类型，需要先清理数据或调整表结构

## 生成时间

生成时间: 2024-11-22
生成工具: `extract_room_data.py`
