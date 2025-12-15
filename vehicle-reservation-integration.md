# vehicle_reservation 表与 appointment 表字段映射

## 需求说明
在小程序预约查询页面，需要同时展示两个来源的数据：
1. `appointment` 表 - 小程序端预约的数据
2. `vehicle_reservation` 表 - 后台录入的预约数据

**关键要求**：
- 必须按车场过滤（同一车场的数据）
- 前端页面展示统一的数据格式

## 字段映射关系

| 前端显示字段（小写） | appointment表字段 | vehicle_reservation表字段 | 说明 |
|-------------------|-----------------|------------------------|------|
| `platenumber` | `platenumber` | `plate_number` | 车牌号 |
| `visitorname` | `visitorname` | `notifier_name` | 访客姓名/通知人姓名 |
| `visitorphone` | `visitorphone` | - | 访客手机（vehicle_reservation无此字段） |
| `recorddate` | `recorddate` | `create_time` | 预约时间/创建时间 |
| `visitdate` | `visitdate` | `appointment_time` | 预约访问时间 |
| `arrivedate` | `arrivedate` | `enter_time` | 进场时间 |
| `leavedate` | `leavedate` | `leave_time` | 离场时间 |
| `auditstatus` | `auditstatus` | - | 审核状态（后台录入默认"已通过"） |
| `venuestatus` | `venuestatus` | 根据时间计算 | 车辆状态 |
| `community` | `community` | `yard_name` | 小区名称/车场名称 |
| `building` | `building` | - | 栋号（vehicle_reservation无此字段） |
| `units` | `units` | - | 单元（vehicle_reservation无此字段） |
| `floor` | `floor` | - | 楼层（vehicle_reservation无此字段） |
| `room` | `room` | - | 房间号（vehicle_reservation无此字段） |
| `ownername` | `ownername` | `merchant_name` | 业主姓名/商户名称 |
| `ownerphone` | `ownerphone` | - | 业主手机（vehicle_reservation无此字段） |
| `visitreason` | `visitreason` | `release_reason` | 访问原因/放行原因 |
| `appointtype` | `appointtype` | "后台录入" | 预约类型 |
| `refusereason` | `refusereason` | `remark` | 拒绝原因/备注 |
| `cartype` | `cartype` | `vehicle_classification` | 车辆类型/车辆分类 |

## 车辆状态计算规则

对于 vehicle_reservation 表的数据，`venuestatus` 根据以下规则计算：

```java
if (enterTime != null && leaveTime != null) {
    venuestatus = "已离场";
} else if (enterTime != null && leaveTime == null) {
    venuestatus = "已进场";
} else {
    venuestatus = "待入场";
}
```

## 默认值设置

vehicle_reservation 表缺少的字段设置默认值：
- `auditstatus`: "已通过" （后台录入默认已审核通过）
- `appointtype`: "后台录入" （标识数据来源）
- `visitorphone`: "" （空字符串）
- `ownerphone`: "" （空字符串）
- `building`: "" （空字符串）
- `units`: "" （空字符串）
- `floor`: "" （空字符串）
- `room`: "" （空字符串）
- `province`: "黑龙江省"
- `city`: "哈尔滨市"
- `district`: "道里区"
- `owneropenid`: ""
- `openid`: ""
- `auditusername`: "系统"

## 车场过滤规则

查询时必须按车场过滤：
- `appointment` 表：`community = ?`
- `vehicle_reservation` 表：`yard_name = ?`

## 实现步骤

1. 修改 `AppointmentController` 的 `getList()` 方法
2. 修改 `AppointmentController` 的 `listByPhone()` 方法（访客查询暂不整合后台数据）
3. 注入 `VehicleReservationService` 服务
4. 查询时合并两表数据
5. 统一转换为前端需要的格式

## 注意事项

1. **数据源标识**：添加 `dataSource` 字段，区分数据来源
   - "miniprogram" - 小程序预约
   - "backend" - 后台录入

2. **按车场过滤**：
   - 管家模式：根据管家的车场查询
   - 访客模式：暂时只查询 appointment 表

3. **排序规则**：
   - 按创建时间倒序（最新的在前）

4. **ID唯一性**：
   - appointment表：保持原有ID
   - vehicle_reservation表：添加前缀 "vr_" + ID
