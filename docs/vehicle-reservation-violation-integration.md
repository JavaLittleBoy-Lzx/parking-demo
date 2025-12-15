# 违规管理集成vehicle_reservation表和appointment表 - 实现说明

## 概述

本次开发实现了违规管理系统与`vehicle_reservation`表和`appointment`表的集成，为非东北林业大学车场（如万象上东等后台预约车场）提供了专用的违规查询和创建接口。

**关联逻辑**：
- `appointment`表：小程序预约记录（优先）
- `vehicle_reservation`表：后台预约记录（次优先）

## 核心变更

### 1. 后端变更

#### 1.1 VehicleReservationMapper
- **文件**: `src/main/java/com/parkingmanage/mapper/VehicleReservationMapper.java`
- **新增方法**: `selectReservationByPlateAndYard(String plateNumber, String yardName)`
- **功能**: 根据车牌号和车场名称查询预约信息

#### 1.2 VehicleReservationMapper.xml
- **文件**: `src/main/java/com/parkingmanage/mapper/xml/VehicleReservationMapper.xml`
- **新增SQL**: `selectReservationByPlateAndYard`
- **关联条件**: `plate_number` + `yard_name` + `deleted=0`

#### 1.3 ViolationsMapper.xml
- **文件**: `src/main/java/com/parkingmanage/mapper/xml/ViolationsMapper.xml`
- **已有方法**: `selectViolationsWithReservation`
- **功能**: 关联`violations`表和`vehicle_reservation`表查询违规记录

#### 1.4 ViolationsService
- **文件**: `src/main/java/com/parkingmanage/service/ViolationsService.java`
- **新增接口**:
  - `getViolationsWithReservation()` - 分页查询违规记录（关联vehicle_reservation）
  - `createViolationForNonNefu()` - 非东林车场创建违规记录
  - `getVehicleReservationInfo()` - 查询vehicle_reservation预约信息

#### 1.5 ViolationsServiceImpl
- **文件**: `src/main/java/com/parkingmanage/service/impl/ViolationsServiceImpl.java`
- **实现了上述三个服务方法**
- **新增依赖**: `VehicleReservationMapper`

#### 1.6 ViolationsController
- **文件**: `src/main/java/com/parkingmanage/controller/ViolationsController.java`
- **新增接口**:
  - `GET /parking/violations/non-nefu/list` - 非东林车场分页查询违规记录
  - `POST /parking/violations/non-nefu/create` - 非东林车场创建违规记录
  - `GET /parking/violations/non-nefu/reservation-info` - 查询预约信息

### 2. 前端变更

#### 2.1 violation-api.js
- **文件**: `car-new-demo/api/violation-api.js`
- **新增方法**:
  - `getViolationsWithReservation(params)` - 调用非东林车场查询接口
  - `createViolationForNonNefu(violationData, yardName)` - 调用非东林车场创建接口
  - `getVehicleReservationInfo(plateNumber, yardName)` - 查询预约信息
  - `isNefuPark(parkName)` - 判断是否为东北林业大学车场

#### 2.2 violation.vue
- **文件**: `car-new-demo/pagesE/violation/violation.vue`
- **已有逻辑**: 根据车场名称自动选择查询接口（第5857-5863行）

#### 2.3 add-violation.vue
- **文件**: `car-new-demo/pagesE/violation/add-violation.vue`
- **新增逻辑**: 提交违规时根据车场名称判断使用哪个API

## 接口说明

### 非东林车场违规查询
```
GET /parking/violations/non-nefu/list
参数:
  - page: 页码（默认1）
  - size: 页大小（默认20）
  - plateNumber: 车牌号（可选）
  - status: 状态（可选）
  - violationType: 违规类型（可选）
  - startDate: 开始时间（可选）
  - endDate: 结束时间（可选）
  - created_by: 创建者（可选）
  - community: 车场名称（可选）
```

### 非东林车场创建违规
```
POST /parking/violations/non-nefu/create?yardName=万象上东
Body: Violations实体JSON
```

### 查询预约信息
```
GET /parking/violations/non-nefu/reservation-info
参数:
  - plateNumber: 车牌号（必填）
  - yardName: 车场名称（必填）
```

## 数据关联逻辑

### violations表与vehicle_reservation表关联
- **关联字段**: `plate_number`（车牌号）+ `yard_name/park_name`（车场名称）
- **关联方式**: LEFT JOIN
- **过滤条件**: `deleted = 0`

### 关联返回字段

#### 来自appointment表（小程序预约，优先）
- `aptOwnerName` - 业主姓名
- `aptOwnerPhone` - 业主电话
- `appointmentReason` - 预约原因
- `aptAppointType` - 预约类型
- `auditorName` - 审核人
- `appointmentStatus` - 预约状态
- `appointmentDate` - 预约日期
- `auditDate` - 审核日期
- `aptArriveDate` - 到达时间
- `aptLeaveDate` - 离开时间
- `aptCommunity/aptBuilding/aptUnits/aptRoom` - 地址信息

#### 来自vehicle_reservation表（后台预约）
- `vrNotifierName` - 通知人姓名
- `merchantName` - 商户名称
- `releaseReason` - 放行原因
- `vrEnterTime` - 进场时间
- `reserveTime` - 预约时间
- `enterVipType` - 进场VIP类型
- `vrRemark` - 备注
- `vehicleClassification` - 车辆分类

#### 合并后的字段（COALESCE优先级：appointment > vehicle_reservation）
- `ownerName` - 业主姓名
- `ownerPhone` - 业主电话
- `ownerAddress` - 业主地址
- `ownerType` - 业主类型（appointment/visitor/monthly/unknown）
- `appointmentType` - 预约类型（小程序/后台）

## 使用说明

### 车场判断逻辑
系统通过车场名称自动判断使用哪套接口：
- **包含"东北林业大学"**: 使用原有接口（`/parking/violations`）
- **其他车场**: 使用新接口（`/parking/violations/non-nefu/*`）

### 前端调用示例
```javascript
import { violationApi } from '@/api/violation-api.js';

// 判断车场类型
const isNefu = violationApi.isNefuPark('万象上东'); // false
const isNefu2 = violationApi.isNefuPark('东北林业大学'); // true

// 查询违规记录
if (violationApi.isNefuPark(parkName)) {
    const data = await violationApi.getViolations(params);
} else {
    const data = await violationApi.getViolationsWithReservation(params);
}

// 创建违规记录
if (violationApi.isNefuPark(parkName)) {
    const result = await violationApi.createViolation(violationData);
} else {
    const result = await violationApi.createViolationForNonNefu(violationData, parkName);
}
```

## 注意事项

1. **东北林业大学项目已上线**，保持原有接口不变
2. 非东林车场的违规查询会自动排除东北林业大学的数据
3. 创建违规时会自动尝试从`vehicle_reservation`表获取预约信息填充业主数据
4. 前端会根据车场名称自动选择正确的API接口

## 文件清单

### 后端文件
1. `VehicleReservationMapper.java` - 新增查询方法
2. `VehicleReservationMapper.xml` - 新增SQL
3. `ViolationsMapper.xml` - 已有关联查询
4. `ViolationsService.java` - 新增服务接口
5. `ViolationsServiceImpl.java` - 新增服务实现
6. `ViolationsController.java` - 新增API接口

### 前端文件
1. `violation-api.js` - 新增API调用方法
2. `violation.vue` - 已有车场判断逻辑
3. `add-violation.vue` - 新增创建时车场判断逻辑
