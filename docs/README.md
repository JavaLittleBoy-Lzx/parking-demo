# 违规管理系统改造项目

本项目基于原有的违规管理系统，将前端的模拟数据改造为与真实数据库连接的完整系统。

## 项目结构

```
├── backend/                    # 后端API服务
│   ├── api/                   # API路由
│   │   ├── owners.js         # 车主相关接口
│   │   ├── violations.js     # 违规记录接口
│   │   └── violation-types.js # 违规类型接口
│   ├── database/             # 数据库脚本
│   │   ├── create_tables.sql # 建表脚本
│   │   └── init_data.sql     # 初始化数据
│   ├── scripts/              # 工具脚本
│   │   └── init-database.js  # 数据库初始化脚本
│   ├── app.js               # 主应用文件
│   ├── package.json         # 依赖配置
│   └── .env.example         # 环境变量示例
├── frontend/                 # 前端相关
│   └── api/                 # API封装
│       └── violation-api.js # 违规管理API封装
└── 原有Vue文件修改/          # 修改后的Vue文件
    ├── violation.vue        # 管家端违规页面
    └── owner-new-violation.vue # 业主端违规页面
```

## 数据库设计

### 主要数据表

1. **owners** - 车主信息表
   - 存储车主基本信息、联系方式、住址、信用分等

2. **vehicles** - 车辆信息表
   - 存储车辆信息，关联车主

3. **violations** - 违规记录表
   - 存储违规记录详情，包括类型、位置、处理状态等

4. **violation_types** - 违规类型配置表
   - 可配置的违规类型，支持使用统计

5. **parking_records** - 停车记录表
   - 停车预约和实际停车记录

6. **users** - 用户表
   - 系统用户账号，支持多角色

## 安装和运行

### 后端服务

1. 安装依赖：
```bash
cd backend
npm install
```

2. 配置环境变量：
```bash
cp .env.example .env
# 编辑 .env 文件，配置数据库连接信息
```

3. 初始化数据库：
```bash
npm run init-db
```

4. 启动服务：
```bash
npm run dev  # 开发模式
# 或
npm start    # 生产模式
```

### 前端集成

1. 将 `frontend/api/violation-api.js` 复制到你的前端项目的 `api` 目录

2. 将修改后的Vue文件替换原有文件：
   - `violation.vue` → `pagesE/violation/violation.vue`
   - `owner-new-violation.vue` → `pages/violation/owner-new-violation.vue`

3. 在前端项目中配置API基础URL：
```javascript
// 在环境变量或配置文件中设置
VUE_APP_API_BASE_URL=http://www.xuerparking.cn:3000/api
```

## API接口文档

### 车主相关接口

- `GET /api/owners/by-plate/:plateNumber` - 根据车牌号查询车主
- `GET /api/owners/plate-suggestions?keyword=xxx` - 车牌号搜索建议
- `GET /api/owners/:ownerId/vehicles` - 获取车主的车辆列表
- `PUT /api/owners/:ownerId/credit-score` - 更新车主信用分

### 违规记录接口

- `POST /api/violations` - 创建违规记录
- `GET /api/violations` - 获取违规记录列表（支持分页和筛选）
- `PUT /api/violations/:id/status` - 更新违规记录状态
- `GET /api/violations/statistics` - 获取违规统计数据
- `GET /api/violations/high-risk-vehicles` - 获取高风险车辆列表

### 违规类型接口

- `GET /api/violation-types` - 获取违规类型列表
- `POST /api/violation-types` - 创建违规类型
- `PUT /api/violation-types/:id` - 更新违规类型
- `DELETE /api/violation-types/:id` - 删除违规类型

## 主要改造内容

### 1. 数据库连接
- 将前端的模拟数据替换为真实的数据库查询
- 实现完整的CRUD操作
- 支持复杂的筛选和统计查询

### 2. API接口
- 设计RESTful API接口
- 实现数据验证和错误处理
- 支持分页、排序、筛选等功能

### 3. 前端改造
- 修改数据获取方式，从模拟数据改为API调用
- 保持原有的UI和交互逻辑
- 增加错误处理和加载状态

### 4. 数据一致性
- 确保前后端数据格式一致
- 实现数据的实时更新
- 支持信用分的动态计算

## 测试数据

系统初始化后包含以下测试数据：

- 8个车主信息（包含不同信用分等级）
- 8个车辆信息（包含新能源车和普通车）
- 13种违规类型（常见违规和其他违规）
- 9条违规记录（不同状态和严重程度）
- 5个用户账号（业主、管家、管理员）

## 注意事项

1. 确保数据库服务正常运行
2. 检查网络连接和端口配置
3. 前端需要处理API调用的异步操作
4. 建议在生产环境中使用HTTPS
5. 定期备份数据库数据

## 技术栈

- **后端**: Node.js + Express + MySQL
- **前端**: Vue.js + uni-app
- **数据库**: MySQL 5.7+
- **其他**: JWT认证、文件上传、日志记录
