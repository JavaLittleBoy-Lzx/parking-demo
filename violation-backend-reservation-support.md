# 违规添加页面支持后台预约数据说明

## 需求

在违规添加页面（`add-violation.vue`）中搜索车牌号码时，需要能够查询到后台录入的预约数据（`vehicle_reservation` 表）。

## 问题分析

### 原有实现

**前端**：`add-violation.vue` 的 `searchPlatesInModal` 方法调用两个接口：
1. `violationApi.searchLocalData()` - 查询月票车数据
2. `appointmentAPI.getAppointmentPlateNumber()` - 查询预约车数据

**后端**：`getAppointmentPlateNumber` 接口只查询 `appointment` 表，不包含 `vehicle_reservation` 表。

### 修改方案

修改后端 `getAppointmentPlateNumber` 接口，让它同时查询两个表的数据：
- `appointment` 表 - 小程序预约
- `vehicle_reservation` 表 - 后台录入预约

## 已完成的修改

### 后端修改

**文件**：`AppointmentController.java`

**方法**：`getAppointmentPlateNumber(@RequestParam String plateNumber)`

**位置**：第 462-559 行

#### 修改内容

1. **查询 appointment 表**（原有逻辑保持不变）
   ```java
   List<Appointment> appointmentAll = appointmentService.getAppointmentPlateNumber(plateNumber);
   ```

2. **添加 vehicle_reservation 表查询**
   ```java
   // 使用 MyBatis-Plus 的 QueryWrapper 按车牌号模糊查询
   QueryWrapper<VehicleReservation> queryWrapper = new QueryWrapper<>();
   if (plateNumber != null && !plateNumber.trim().isEmpty()) {
       queryWrapper.like("plate_number", plateNumber);
   }
   
   List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
   ```

3. **数据转换与合并**
   ```java
   for (VehicleReservation vr : vehicleReservations) {
       Map<String, Object> vrMap = convertVehicleReservationToMap(vr);
       appointmentList.add(vrMap);
   }
   ```

4. **添加数据来源标识**
   - `appointment` 表的数据：`dataSource = "miniprogram"`
   - `vehicle_reservation` 表的数据：`dataSource = "backend"`（在 `convertVehicleReservationToMap` 方法中设置）

#### 关键代码

```java
@GetMapping("/getAppointmentPlateNumber")
@ResponseBody
public R<Map<String, Object>> getAppointmentPlateNumber(@RequestParam(required = false) String plateNumber) {
    logger.info("🔍 [预约车搜索] 开始查询，车牌号: {} (包含后台录入)", plateNumber);
    
    ArrayList<Object> appointmentList = new ArrayList<>();
    
    // 1️⃣ 查询 appointment 表数据
    List<Appointment> appointmentAll = appointmentService.getAppointmentPlateNumber(plateNumber);
    logger.info("📋 [预约车搜索] appointment表查询结果: {} 条", appointmentAll.size());
    
    // ... 转换 appointment 数据，添加 dataSource = "miniprogram" ...
    
    // 2️⃣ 查询 vehicle_reservation 表数据（后台录入）
    try {
        QueryWrapper<VehicleReservation> queryWrapper = new QueryWrapper<>();
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            queryWrapper.like("plate_number", plateNumber);
        }
        
        List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
        logger.info("📋 [预约车搜索] vehicle_reservation表查询结果: {} 条", vehicleReservations.size());
        
        for (VehicleReservation vr : vehicleReservations) {
            Map<String, Object> vrMap = convertVehicleReservationToMap(vr);
            appointmentList.add(vrMap);
            logger.info("🔍 [预约车搜索] 找到后台录入记录 - 车牌: {}, 车场: {}", vr.getPlateNumber(), vr.getYardName());
        }
        
        logger.info("✅ [预约车搜索] 合并后总数据量: {} 条", appointmentList.size());
    } catch (Exception e) {
        logger.error("❌ [预约车搜索] 查询vehicle_reservation表失败: {}", e.getMessage(), e);
        // 即使查询失败，也返回appointment表的数据
    }
    
    HashMap<String, Object> dataMap = new HashMap<>();
    dataMap.put("data", appointmentList);
    return R.ok(dataMap);
}
```

### 前端自动支持

**文件**：`add-violation.vue`

**方法**：`searchPlatesInModal()` （第 2030-2179 行）

前端已经有数据来源识别逻辑：
```javascript
dataSource: '预约车', // 标记数据来源
```

后端返回的 `dataSource` 字段会自动被前端识别：
- `dataSource === "miniprogram"` → 显示为"预约车"
- `dataSource === "backend"` → 同样显示为"预约车"（因为都是预约数据）

## 数据流程

```
违规添加页面搜索车牌
         ↓
调用 appointmentAPI.getAppointmentPlateNumber(plateNumber)
         ↓
后端查询两个表
    ├─ appointment 表（小程序预约）
    └─ vehicle_reservation 表（后台录入）
         ↓
数据转换为统一格式
    ├─ 小程序预约：dataSource = "miniprogram"
    └─ 后台录入：dataSource = "backend"
         ↓
合并返回给前端
         ↓
前端显示搜索结果（支持选择）
```

## 字段映射

使用 `convertVehicleReservationToMap` 方法进行字段映射，与 `getList` 接口使用相同的转换逻辑。

详见：`vehicle-reservation-integration.md`

## 测试步骤

### 1. 后端测试
```bash
# 测试接口
GET http://localhost:8080/parking/appointment/getAppointmentPlateNumber?plateNumber=粤B

# 检查返回数据：
# 1. 是否包含 appointment 表的数据
# 2. 是否包含 vehicle_reservation 表的数据
# 3. dataSource 字段是否正确
# 4. vehicle_reservation 的 ID 是否有 "vr_" 前缀
```

### 2. 前端测试
1. 进入违规添加页面（`pagesE/violation/add-violation.vue`）
2. 点击车牌号搜索框
3. 输入后台录入的车牌号（部分或完整）
4. 检查搜索结果：
   - ✅ 能否看到后台录入的预约记录
   - ✅ 车主信息是否正确显示（通知人姓名）
   - ✅ 车场名称是否正确
   - ✅ 进场/离场时间是否正确
5. 选择后台录入的预约记录
6. 检查表单是否正确填充车牌号和车主信息

## 数据来源识别

### 后端标识
```java
// 小程序预约
appointmentMap.put("dataSource", "miniprogram");

// 后台录入
map.put("dataSource", "backend");  // 在 convertVehicleReservationToMap 中设置
```

### 前端处理
前端在 `add-violation.vue` 中将所有预约数据统一标记为"预约车"：
```javascript
dataSource: '预约车', // 标记数据来源
```

前端不区分小程序预约和后台录入，统一作为预约车处理。如果需要区分，可以通过后端返回的 `dataSource` 字段或 `appointtype` 字段判断。

## 注意事项

### 1. 查询性能
- 使用模糊查询（`LIKE`），在数据量大时可能影响性能
- 建议为 `plate_number` 字段添加索引

### 2. 异常处理
- 如果 vehicle_reservation 表查询失败，不影响 appointment 表数据的返回
- 记录错误日志，便于排查问题

### 3. ID 唯一性
- `appointment` 表：保持原有ID
- `vehicle_reservation` 表：添加前缀 `"vr_"`，避免ID冲突

### 4. 数据过滤
- 后台录入的数据可能跨车场
- 前端应根据当前管家所属车场进行过滤（如果需要）

## 相关接口

1. **getList** - 查询所有预约（已修改，支持后台数据）
   - 用于：预约查询列表页面
   
2. **getAppointmentPlateNumber** - 按车牌号搜索预约（本次修改）
   - 用于：违规添加页面的车牌搜索

## 修改文件清单

1. **后端**：
   - `d:\PakingDemo\parking-demo\src\main\java\com\parkingmanage\controller\AppointmentController.java`
     - 第 462-559 行：修改 `getAppointmentPlateNumber` 方法

2. **前端**（无需修改）：
   - `d:\PakingDemo\car-new-demo-2\car-new-demo\pagesE\violation\add-violation.vue`
     - 已有的 `searchPlatesInModal` 方法自动支持

## 测试清单

- [ ] 后端编译通过
- [ ] 接口返回合并后的数据
- [ ] 后台录入数据字段映射正确
- [ ] 违规添加页面能搜索到后台数据
- [ ] 选择后台数据后表单填充正确
- [ ] 数据来源标识正确
- [ ] 车牌号模糊搜索正常工作
- [ ] 异常情况下不影响小程序数据查询

## 完成时间

2024-12-12

## 开发者

Cascade AI Assistant
