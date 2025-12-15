# 违规管理系统后端接口文档

## 概述

本文档详细描述了违规管理系统中新增违规记录功能所需的后端接口。这些接口支持管家录入违规记录的完整流程，包括车牌识别、车主信息查询、违规记录提交等功能。

## 接口列表

### 1. 车牌识别接口

#### 1.1 OCR车牌识别

**接口地址：** `POST /api/ocr/plate-recognition`

**功能描述：** 通过图像识别技术识别车牌号码

**请求参数：**
```json
{
  "image": "base64编码的图片数据",
  "imageType": "jpg|png|jpeg",
  "confidence": 0.8
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "plateNumber": "黑A12345",
    "confidence": 0.95,
    "plateType": "普通车辆",
    "region": {
      "x": 100,
      "y": 50,
      "width": 200,
      "height": 80
    }
  }
}
```

**错误响应：**
```json
{
  "code": 400,
  "message": "图片格式不支持",
  "data": null
}
```

### 2. 车主信息查询接口

#### 2.1 根据车牌号查询车主信息

**接口地址：** `GET /api/owners/by-plate/{plateNumber}`

**功能描述：** 根据车牌号查询车主基本信息

**路径参数：**
- `plateNumber`: 车牌号（必填）

**查询参数：**
- `communityId`: 社区ID（可选，用于限制查询范围）

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "ownerId": "123456",
    "name": "张三",
    "phone": "138****5678",
    "plateNumber": "黑A12345",
    "vehicleType": "小型汽车",
    "address": {
      "building": "3栋",
      "unit": "2单元",
      "floor": "15楼",
      "room": "1502"
    },
    "registrationDate": "2023-01-15",
    "status": "active"
  }
}
```

#### 2.2 车主历史违规查询

**接口地址：** `GET /api/owners/{ownerId}/violations`

**功能描述：** 查询车主的历史违规记录

**路径参数：**
- `ownerId`: 车主ID

**查询参数：**
- `page`: 页码（默认1）
- `size`: 每页数量（默认10）
- `startDate`: 开始日期（可选）
- `endDate`: 结束日期（可选）

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 12,
    "currentMonth": 3,
    "records": [
      {
        "id": "v001",
        "plateNumber": "黑A12345",
        "violationType": "超时停车",
        "location": "A区-15号车位",
        "violationTime": "2025-01-20 14:30:00",
        "status": "已处理",
        "reporterId": "manager001"
      }
    ]
  }
}
```

### 3. 违规记录管理接口

#### 3.1 新增违规记录

**接口地址：** `POST /api/violations`

**功能描述：** 管家录入新的违规记录

**请求参数：**
```json
{
  "plateNumber": "黑A12345",
  "ownerId": "123456",
  "violationType": "超时停车",
  "customType": "",
  "location": "A区-15号车位",
  "coordinates": {
    "latitude": 39.9042,
    "longitude": 116.4074
  },
  "description": "车辆超时停车2小时，影响其他车辆使用",
  "severity": "moderate",
  "evidence": {
    "photos": [
      "https://example.com/evidence/photo1.jpg",
      "https://example.com/evidence/photo2.jpg"
    ],
    "videos": [],
    "voiceMemo": "https://example.com/evidence/voice1.mp3"
  },
  "reporterId": "manager001",
  "communityId": "community001"
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "违规记录创建成功",
  "data": {
    "violationId": "v20250122001",
    "plateNumber": "黑A12345",
    "status": "pending",
    "createdAt": "2025-01-22 14:30:00",
    "estimatedProcessTime": "2025-01-22 16:30:00"
  }
}
```

#### 3.2 上传违规证据文件

**接口地址：** `POST /api/violations/evidence/upload`

**功能描述：** 上传违规现场的照片、视频或语音文件

**请求参数：** `multipart/form-data`
- `file`: 文件（必填）
- `type`: 文件类型（photo|video|voice）
- `violationId`: 违规记录ID（可选，用于关联）

**响应数据：**
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "fileId": "file001",
    "fileName": "evidence_20250122_143000.jpg",
    "fileUrl": "https://example.com/evidence/file001.jpg",
    "fileSize": 1024000,
    "uploadTime": "2025-01-22 14:30:00"
  }
}
```

#### 3.3 查询违规记录列表

**接口地址：** `GET /api/violations`

**功能描述：** 查询违规记录列表（支持多种筛选条件）

**查询参数：**
- `page`: 页码（默认1）
- `size`: 每页数量（默认20）
- `status`: 状态筛选（pending|processing|completed|cancelled）
- `plateNumber`: 车牌号筛选
- `violationType`: 违规类型筛选
- `reporterId`: 报告人ID筛选
- `communityId`: 社区ID筛选
- `startDate`: 开始日期
- `endDate`: 结束日期
- `severity`: 严重程度（mild|moderate|severe）

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": "v20250122001",
        "plateNumber": "黑A12345",
        "ownerName": "张三",
        "ownerPhone": "138****5678",
        "violationType": "超时停车",
        "location": "A区-15号车位",
        "violationTime": "2025-01-22 14:30:00",
        "status": "pending",
        "severity": "moderate",
        "reporterName": "李管家",
        "evidenceCount": {
          "photos": 3,
          "videos": 0,
          "voiceMemos": 1
        },
        "createdAt": "2025-01-22 14:30:00"
      }
    ]
  }
}
```

#### 3.4 获取违规记录详情

**接口地址：** `GET /api/violations/{violationId}`

**功能描述：** 获取指定违规记录的详细信息

**路径参数：**
- `violationId`: 违规记录ID

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": "v20250122001",
    "plateNumber": "黑A12345",
    "owner": {
      "id": "123456",
      "name": "张三",
      "phone": "13812345678",
      "address": "3栋2单元1502"
    },
    "violationType": "超时停车",
    "customType": "",
    "location": "A区-15号车位",
    "coordinates": {
      "latitude": 39.9042,
      "longitude": 116.4074
    },
    "description": "车辆超时停车2小时，影响其他车辆使用",
    "severity": "moderate",
    "evidence": {
      "photos": [
        {
          "id": "photo001",
          "url": "https://example.com/evidence/photo1.jpg",
          "thumbnail": "https://example.com/evidence/thumb_photo1.jpg",
          "uploadTime": "2025-01-22 14:30:00"
        }
      ],
      "videos": [],
      "voiceMemos": [
        {
          "id": "voice001",
          "url": "https://example.com/evidence/voice1.mp3",
          "duration": 45,
          "uploadTime": "2025-01-22 14:31:00"
        }
      ]
    },
    "reporter": {
      "id": "manager001",
      "name": "李管家",
      "phone": "13987654321"
    },
    "status": "pending",
    "statusHistory": [
      {
        "status": "pending",
        "time": "2025-01-22 14:30:00",
        "operator": "李管家",
        "remark": "违规记录已创建"
      }
    ],
    "createdAt": "2025-01-22 14:30:00",
    "updatedAt": "2025-01-22 14:30:00"
  }
}
```

### 4. 地理位置相关接口

#### 4.1 坐标转地址

**接口地址：** `POST /api/location/geocode`

**功能描述：** 将GPS坐标转换为具体地址信息

**请求参数：**
```json
{
  "latitude": 39.9042,
  "longitude": 116.4074,
  "communityId": "community001"
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "转换成功",
  "data": {
    "address": "A区-15号车位",
    "building": "A区",
    "parkingSpace": "15号车位",
    "zone": "地下停车场",
    "accuracy": "high"
  }
}
```

#### 4.2 获取社区停车位列表

**接口地址：** `GET /api/communities/{communityId}/parking-spaces`

**功能描述：** 获取指定社区的停车位列表

**路径参数：**
- `communityId`: 社区ID

**查询参数：**
- `zone`: 区域筛选（可选）
- `status`: 状态筛选（available|occupied|maintenance）

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "ps001",
      "number": "A-001",
      "zone": "A区",
      "type": "standard",
      "status": "occupied",
      "coordinates": {
        "latitude": 39.9042,
        "longitude": 116.4074
      }
    }
  ]
}
```

### 5. 通知相关接口

#### 5.1 发送违规通知

**接口地址：** `POST /api/notifications/violation`

**功能描述：** 向车主发送违规通知

**请求参数：**
```json
{
  "violationId": "v20250122001",
  "ownerId": "123456",
  "notificationType": "sms|wechat|app",
  "message": "您的车辆黑A12345在A区-15号车位存在违规停车行为，请及时处理。",
  "urgency": "normal"
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "通知发送成功",
  "data": {
    "notificationId": "n20250122001",
    "sentAt": "2025-01-22 14:35:00",
    "estimatedDelivery": "2025-01-22 14:36:00"
  }
}
```

### 6. 统计分析接口

#### 6.1 违规统计数据

**接口地址：** `GET /api/violations/statistics`

**功能描述：** 获取违规记录的统计数据

**查询参数：**
- `timeRange`: 时间范围（week|month|quarter|year）
- `communityId`: 社区ID
- `groupBy`: 分组方式（type|time|location|reporter）

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "summary": {
      "total": 156,
      "pending": 23,
      "processing": 45,
      "completed": 88
    },
    "byType": [
      {
        "type": "超时停车",
        "count": 67,
        "percentage": 43.0
      },
      {
        "type": "未按位停车",
        "count": 34,
        "percentage": 21.8
      }
    ],
    "byTime": [
      {
        "date": "2025-01-22",
        "count": 12
      }
    ],
    "trends": {
      "thisWeek": 23,
      "lastWeek": 18,
      "growth": 27.8
    }
  }
}
```

## 数据库设计建议

### 违规记录表 (violations)

```sql
CREATE TABLE violations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  violation_id VARCHAR(50) UNIQUE NOT NULL COMMENT '违规记录编号',
  plate_number VARCHAR(20) NOT NULL COMMENT '车牌号',
  owner_id BIGINT COMMENT '车主ID',
  violation_type VARCHAR(50) NOT NULL COMMENT '违规类型',
  custom_type VARCHAR(100) COMMENT '自定义违规类型',
  location VARCHAR(200) COMMENT '违规位置',
  latitude DECIMAL(10, 8) COMMENT '纬度',
  longitude DECIMAL(11, 8) COMMENT '经度',
  description TEXT COMMENT '违规描述',
  severity ENUM('mild', 'moderate', 'severe') DEFAULT 'moderate' COMMENT '严重程度',
  status ENUM('pending', 'processing', 'completed', 'cancelled') DEFAULT 'pending' COMMENT '处理状态',
  reporter_id BIGINT NOT NULL COMMENT '报告人ID',
  community_id BIGINT NOT NULL COMMENT '社区ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_plate_number (plate_number),
  INDEX idx_owner_id (owner_id),
  INDEX idx_reporter_id (reporter_id),
  INDEX idx_community_id (community_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

### 违规证据表 (violation_evidence)

```sql
CREATE TABLE violation_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  violation_id BIGINT NOT NULL COMMENT '违规记录ID',
  file_type ENUM('photo', 'video', 'voice') NOT NULL COMMENT '文件类型',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_url VARCHAR(500) NOT NULL COMMENT '文件URL',
  file_size BIGINT COMMENT '文件大小(字节)',
  duration INT COMMENT '时长(秒，仅音视频)',
  thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
  upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (violation_id) REFERENCES violations(id) ON DELETE CASCADE,
  INDEX idx_violation_id (violation_id),
  INDEX idx_file_type (file_type)
);
```

## 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权访问 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 数据冲突（如重复提交） |
| 422 | 数据验证失败 |
| 500 | 服务器内部错误 |
| 1001 | 车牌号格式错误 |
| 1002 | 车主信息不存在 |
| 1003 | 违规类型不支持 |
| 1004 | 文件上传失败 |
| 1005 | 图片识别失败 |
| 1006 | 地理位置解析失败 |

## 安全考虑

1. **权限验证**：所有接口需要验证用户身份和权限
2. **数据脱敏**：车主手机号等敏感信息需要脱敏处理
3. **文件安全**：上传的文件需要进行安全检查
4. **接口限流**：防止恶意调用和刷量
5. **数据加密**：敏感数据传输需要加密
6. **审计日志**：记录所有操作日志用于审计

## 性能优化建议

1. **数据库索引**：为常用查询字段建立索引
2. **缓存策略**：对热点数据进行缓存
3. **文件存储**：使用CDN加速文件访问
4. **分页查询**：大数据量查询必须分页
5. **异步处理**：文件上传和通知发送采用异步处理
