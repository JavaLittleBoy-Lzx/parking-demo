# 微信小程序后端配置说明

## 🔧 开发环境配置

### 1. 当前状态
- 开发模式已启用 (`IS_DEV_MODE = true`)
- 使用模拟的session_key和测试手机号
- 无需真实的微信API配置

### 2. 测试手机号配置
在 `WeChatUtils.java` 中修改测试手机号：
```java
String testPhoneNumber = "13800138000";  // 替换为你的测试手机号
```

## 🚀 生产环境配置

### 1. 启用生产模式
在 `WeChatUtils.java` 中：
```java
private static final boolean IS_DEV_MODE = false;  // 改为false
```

### 2. 配置微信小程序参数
```java
private static final String APP_ID = "你的小程序AppID";
private static final String APP_SECRET = "你的小程序AppSecret";
```

### 3. 添加HTTP客户端依赖
在 `pom.xml` 中添加：
```xml
<!-- Spring Web 已包含RestTemplate -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 或者使用OkHttp -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### 4. 解除生产代码注释
在 `WeChatUtils.java` 的 `getSessionKey` 方法中，解除注释并使用以下代码：
```java
// 使用RestTemplate调用微信API
RestTemplate restTemplate = new RestTemplate();
String responseBody = restTemplate.getForObject(url, String.class);

logger.info("微信登录响应: {}", responseBody);

JSONObject jsonObject = JSONObject.parseObject(responseBody);
if (jsonObject.containsKey("session_key")) {
    return jsonObject.getString("session_key");
} else if (jsonObject.containsKey("errcode")) {
    String errMsg = jsonObject.getString("errmsg");
    logger.error("微信API返回错误: {}", errMsg);
    throw new RuntimeException("微信API错误: " + errMsg);
} else {
    logger.error("获取session_key失败，未知响应格式: {}", responseBody);
    throw new RuntimeException("获取session_key失败");
}
```

## 🔐 安全注意事项

### 1. AppSecret保护
- 永远不要将AppSecret提交到代码仓库
- 使用环境变量或配置文件管理敏感信息
- 建议使用Spring的`@Value`注解读取配置

### 2. 使用配置文件
创建 `application-prod.yml`：
```yaml
wechat:
  miniprogram:
    app-id: ${WECHAT_APP_ID}
    app-secret: ${WECHAT_APP_SECRET}
```

在代码中使用：
```java
@Value("${wechat.miniprogram.app-id}")
private String appId;

@Value("${wechat.miniprogram.app-secret}")
private String appSecret;
```

## 🧪 测试建议

### 1. 开发阶段测试
- 使用真实的测试手机号（你自己的手机号）
- 确保该手机号在业主或管家表中有对应记录

### 2. 数据库准备
在开发数据库中添加测试数据：
```sql
-- 添加测试管家
INSERT INTO butler (usercode, username, phone, province, city, district, community, status) 
VALUES ('B001', '测试管家', '13800138000', '广东省', '深圳市', '南山区', '测试小区', '已确认');

-- 添加测试业主
INSERT INTO ownerinfo (province, city, district, community, building, units, floor, roomnumber, ownername, ownerphone) 
VALUES ('广东省', '深圳市', '南山区', '测试小区', 'A栋', 1, 10, 1001, '测试业主', '13800138001');
```

## 📱 前端配置

### 1. API地址配置
在小程序的 `config/api.js` 中：
```javascript
// 生产环境
production: {
  baseURL: 'https://your-domain.com', // 你的HTTPS域名
  timeout: 15000
}
```

### 2. 微信开发者工具配置
- 开发阶段：勾选"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"
- 生产阶段：在微信公众平台配置服务器域名

## 🔄 部署流程

1. 修改 `IS_DEV_MODE = false`
2. 配置真实的APP_ID和APP_SECRET
3. 添加HTTP客户端依赖
4. 解除生产代码注释
5. 配置HTTPS域名
6. 在微信公众平台配置服务器域名
7. 部署并测试

## ⚠️ 常见问题

### 1. 解密失败
- 检查AppID和AppSecret是否正确
- 确认微信返回的session_key格式
- 验证加密数据的完整性

### 2. 权限问题
- 确保用户手机号在数据库中存在
- 检查角色判断逻辑
- 验证权限配置

### 3. 网络问题
- 确保服务器可以访问微信API
- 检查防火墙设置
- 验证HTTPS证书配置 