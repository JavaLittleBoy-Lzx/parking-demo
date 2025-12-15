# 🚗 停车管理系统 - 小程序API接口说明

## 📋 预约违规记录相关接口

### **1. 查询违规记录列表**
**接口地址：** `GET /api/violations`

**功能说明：** 查询违规记录列表，包含完整的预约信息

**响应示例：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "plateNumber": "川A12345",
      "appointmentId": 456,
      "violationType": "违规停车",
      "location": "A区域",
      "status": "pending",
      "statusText": "待处理",
      "createdAt": "2025-01-15 14:30:00",
      
      // 🔥 业主基础信息
      "ownerName": "张三",
      "ownerPhone": "13800138000", 
      "ownerAddress": "阳光小区1栋2单元301室",
      "ownerType": "appointment",
      
      // 🆕 预约详细信息（仅预约车有）
      "appointmentReason": "探望老人",
      "appointmentType": "visitor",
      "appointmentTypeText": "访客预约",
      "auditorName": "王管理员",
      "appointmentStatus": "approved",
      "appointmentStatusText": "已审核通过",
      "appointmentDate": "2025-01-15",
      "auditDate": "2025-01-14 10:30:00"
    }
  ]
}
```

### **2. 🆕 查询预约详细信息**
**接口地址：** `GET /api/violations/appointment-detail/{appointmentId}`

**功能说明：** 根据预约ID查询预约的完整详细信息（小程序端专用）

**请求示例：**
```
GET /api/violations/appointment-detail/456
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 基础信息
    "appointmentId": 456,
    "plateNumber": "川A12345",
    "ownerName": "张三",
    "ownerPhone": "13800138000",
    "ownerAddress": "阳光小区1栋2单元301室",
    "ownerType": "appointment",
    
    // 🔥 预约核心信息
    "appointmentReason": "探望老人",
    "appointmentType": "visitor",
    "appointmentTypeText": "访客预约",
    "appointmentDate": "2025-01-15",
    
    // 🔥 审核信息
    "auditorName": "王管理员",
    "appointmentStatus": "approved",
    "appointmentStatusText": "已审核通过",
    "auditDate": "2025-01-14 10:30:00",
    
    // 停车状态信息
    "arrivedate": "2025-01-15 09:30:00",
    "leavedate": null,
    "recorddate": "2025-01-14 08:45:00",
    
    // 其他详细信息
    "refuseReason": null,
    "venueStatus": "available",
    "parkingInfo": "A区域临时停车位"
  }
}
```

### **3. 根据车牌号查询预约记录**
**接口地址：** `GET /api/violations/appointment-records/{plateNumber}`

**功能说明：** 根据车牌号查询相关的预约记录（用于违规录入时参考）

**请求示例：**
```
GET /api/violations/appointment-records/川A12345
```

**响应示例：**
```json
{
  "code": 200,
  "data": [
    {
      "appointmentId": 456,
      "plateNumber": "川A12345",
      "ownerName": "张三",
      "ownerPhone": "13800138000",
      "ownerAddress": "阳光小区1栋2单元301室",
      "visitdate": "2025-01-15",
      "auditstatus": "approved",
      "auditStatusText": "已审核通过",
      "parkingStatus": "在场中"
    }
  ]
}
```

### **4. 创建违规记录**
**接口地址：** `POST /api/violations`

**功能说明：** 创建违规记录，支持关联预约记录

**请求示例：**
```json
{
  "plateNumber": "川A12345",
  "appointmentId": 456,  // 🆕 预约记录ID，可选
  "ownerId": 789,        // 业主ID，可选（有appointmentId时可不传）
  "violationType": "违规停车",
  "location": "A区域",
  "description": "超时停车2小时",
  "createdBy": "安保员001"
}
```

## 🎯 小程序端使用场景

### **场景1：违规记录列表展示**
小程序端显示违规记录时，可以直接展示：
- ✅ 基础违规信息（车牌、类型、位置、时间）
- ✅ 业主联系信息（姓名、电话、地址）
- ✅ **预约原因**（如：探望老人、朋友聚会）
- ✅ **预约类型**（访客预约、业主预约、送货预约等）
- ✅ **审核人姓名**（便于问题反馈）

### **场景2：违规详情页面**
点击某条预约车违规记录后，调用详情接口展示：
```
🚗 车辆信息
车牌号：川A12345
业主：张三 (13800138000)
地址：阳光小区1栋2单元301室

📅 预约信息  
预约类型：访客预约
预约原因：探望老人
预约日期：2025-01-15
审核人：王管理员
审核状态：已审核通过

⚠️ 违规信息
违规类型：违规停车
发生位置：A区域
违规描述：超时停车2小时
记录时间：2025-01-15 14:30:00
```

### **场景3：数据字段说明**

| 字段名 | 类型 | 说明 | 小程序显示建议 |
|--------|------|------|----------------|
| `appointmentReason` | String | 预约原因 | 🏷️ 原因标签 |
| `appointmentType` | String | 预约类型代码 | 🔄 转换为中文显示 |
| `appointmentTypeText` | String | 预约类型中文 | 📝 直接显示 |
| `auditorName` | String | 审核人姓名 | 👤 联系人信息 |
| `appointmentStatus` | String | 审核状态代码 | 🎯 状态图标 |
| `appointmentStatusText` | String | 审核状态中文 | ✅ 状态文字 |
| `appointmentDate` | String | 预约日期 | 📅 日期显示 |
| `auditDate` | DateTime | 审核时间 | ⏰ 时间戳 |

## 🔧 开发建议

1. **优先级显示**：预约信息 > 本地车主信息 > 月票车主信息
2. **空值处理**：非预约车的预约相关字段为null，需要前端做空值判断
3. **类型转换**：后端已提供中文显示字段，建议优先使用
4. **错误处理**：接口可能返回404（预约不存在）或500（系统错误）

## 📱 小程序界面设计建议

```
┌─────────────────────────┐
│ 🚗 违规记录详情         │
├─────────────────────────┤
│ 📋 基础信息             │
│   车牌：川A12345        │
│   时间：01-15 14:30     │
│   位置：A区域           │
├─────────────────────────┤
│ 👤 业主信息             │
│   姓名：张三            │
│   电话：138****8000     │
│   地址：阳光小区1栋2单元 │
├─────────────────────────┤
│ 📅 预约信息 [预约车]    │
│   类型：🏠 访客预约     │
│   原因：👵 探望老人     │
│   审核：✅ 王管理员通过 │
└─────────────────────────┘
```

---

📞 **技术支持**：如有接口问题，请联系后端开发团队
⚡ **更新日志**：2025-01-15 新增预约详细信息字段支持 