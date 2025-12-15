# 微信公众号API测试案例

本文档提供微信公众号API接口的具体测试案例和步骤。

## 前置条件

1. 确保项目已启动，端口为 8543
2. 确保 `application.yml` 中配置了正确的微信公众号 `appid` 和 `secret`
3. 确保有有效的微信公众号开发者权限

## 测试步骤

### 步骤1：获取access_token

**方法1：使用配置文件中的默认值**

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken"
```

**方法2：传入自定义参数**

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken?appid=wx7fcbbc5d885b630b&secret=19b9f00b48f266875b1b7e55eda6dd17"
```

**预期响应（成功）：**
```json
{
  "code": "0",
  "msg": "获取access_token成功",
  "data": {
    "access_token": "65_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "expires_in": 7200,
    "expires_time": 1704067200000
  }
}
```

**预期响应（失败）：**
```json
{
  "code": "1",
  "msg": "获取access_token失败: [40013] 不合法的 AppID"
}
```

### 步骤2：检查access_token有效性

使用步骤1获取到的access_token：

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/checkAccessToken?accessToken=YOUR_ACCESS_TOKEN"
```

**预期响应（有效）：**
```json
{
  "code": "0",
  "msg": "access_token有效",
  "data": {
    "valid": true,
    "response": {
      "ip_list": [
        "101.226.103.0/25",
        "101.226.233.0/24"
      ]
    }
  }
}
```

### 步骤3：获取关注用户列表

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserList?accessToken=YOUR_ACCESS_TOKEN"
```

**预期响应：**
```json
{
  "code": "0",
  "msg": "获取关注用户列表成功",
  "data": {
    "total": 2,
    "count": 2,
    "data": {
      "openid": [
        "openid1",
        "openid2"
      ]
    },
    "next_openid": "openid2"
  }
}
```

### 步骤4：获取单个用户信息

使用步骤3获取到的openid：

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserInfo?accessToken=YOUR_ACCESS_TOKEN&openid=USER_OPENID&lang=zh_CN"
```

**预期响应：**
```json
{
  "code": "0",
  "msg": "获取用户信息成功",
  "data": {
    "subscribe": 1,
    "openid": "openid1",
    "language": "zh_CN",
    "subscribe_time": 1640995200,
    "unionid": "unionid",
    "remark": "",
    "groupid": 0,
    "tagid_list": [],
    "subscribe_scene": "ADD_SCENE_OTHERS",
    "qr_scene": 0,
    "qr_scene_str": ""
  }
}
```

### 步骤5：批量获取用户信息

使用步骤3获取到的openid列表：

```bash
curl -X POST "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openids": ["openid1", "openid2"],
    "lang": "zh_CN"
  }'
```

**预期响应：**
```json
{
  "code": "0",
  "msg": "批量获取用户信息成功",
  "data": {
    "user_info_list": [
      {
        "subscribe": 1,
        "openid": "openid1",
        "language": "zh_CN",
        "subscribe_time": 1640995200,
        "unionid": "unionid1",
        "remark": "",
        "groupid": 0,
        "tagid_list": [],
        "subscribe_scene": "ADD_SCENE_OTHERS",
        "qr_scene": 0,
        "qr_scene_str": ""
      },
      {
        "subscribe": 1,
        "openid": "openid2",
        "language": "zh_CN",
        "subscribe_time": 1640995300,
        "unionid": "unionid2",
        "remark": "",
        "groupid": 0,
        "tagid_list": [],
        "subscribe_scene": "ADD_SCENE_OTHERS",
        "qr_scene": 0,
        "qr_scene_str": ""
      }
    ],
    "total_count": 2,
    "request_count": 2
  }
}
```

## 使用Postman测试

### 1. 获取access_token

- **方法**: GET
- **URL**: `http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken`
- **参数**: 
  - `appid`: wx7fcbbc5d885b630b (可选)
  - `secret`: 19b9f00b48f266875b1b7e55eda6dd17 (可选)

### 2. 批量获取用户信息

- **方法**: POST
- **URL**: `http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=YOUR_ACCESS_TOKEN`
- **Headers**: 
  - `Content-Type`: application/json
- **Body** (raw JSON):
```json
{
  "openids": ["openid1", "openid2"],
  "lang": "zh_CN"
}
```

## 错误场景测试

### 1. 无效的AppID

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken?appid=invalid_appid&secret=invalid_secret"
```

**预期响应：**
```json
{
  "code": "1",
  "msg": "获取access_token失败: [40013] 不合法的 AppID"
}
```

### 2. 无效的access_token

```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserList?accessToken=invalid_token"
```

**预期响应：**
```json
{
  "code": "1",
  "msg": "获取关注用户列表失败: [40001] invalid credential access_token isinvalid or not latest"
}
```

### 3. 空的openid列表

```bash
curl -X POST "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openids": [],
    "lang": "zh_CN"
  }'
```

**预期响应：**
```json
{
  "code": "1",
  "msg": "缺少必需参数：openids列表不能为空"
}
```

### 4. 超过100个openid

```bash
curl -X POST "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openids": ["openid1", "openid2", ... "openid101"],
    "lang": "zh_CN"
  }'
```

**预期响应：**
```json
{
  "code": "1",
  "msg": "openids列表长度不能超过100"
}
```

## 性能测试建议

### 1. 并发测试

使用 JMeter 或其他工具测试并发请求：

- 获取access_token: 建议QPS < 10（微信限制）
- 批量获取用户信息: 建议QPS < 100（微信限制）

### 2. 缓存测试

- 验证access_token在有效期内的缓存机制
- 测试access_token过期后的自动重新获取

## 集成测试

### Java单元测试示例

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class WeChatPublicApiTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String accessToken;
    
    @Test
    @Order(1)
    public void testGetAccessToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/parking/wechat-public/getAccessToken", Map.class);
        
        assertEquals("0", response.getBody().get("code"));
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        this.accessToken = (String) data.get("access_token");
        assertNotNull(this.accessToken);
    }
    
    @Test
    @Order(2)
    public void testBatchGetUserInfo() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("openids", Arrays.asList("test_openid1", "test_openid2"));
        requestBody.put("lang", "zh_CN");
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/parking/wechat-public/batchGetUserInfo?accessToken=" + accessToken, 
            requestBody, Map.class);
        
        assertEquals("0", response.getBody().get("code"));
    }
}
```

## 日志监控

测试时注意观察日志输出：

```
2024-01-01 10:00:00 INFO  WeChatPublicApiController - 🔑 开始获取access_token - appid: [wx7fcbbc5d885b630b]
2024-01-01 10:00:01 INFO  WeChatPublicApiController - ✅ 成功获取access_token，有效期: 7200 秒
2024-01-01 10:00:05 INFO  WeChatPublicApiController - 📊 开始批量获取用户信息 - 用户数量: 2, 语言: zh_CN
2024-01-01 10:00:06 INFO  WeChatPublicApiController - ✅ 成功获取用户信息 - 返回数量: 2
```

---

*测试完成后，请确保删除或保护敏感的access_token信息* 