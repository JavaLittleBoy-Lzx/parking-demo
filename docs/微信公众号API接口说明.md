# 微信公众号API接口说明

本项目新增了微信公众号API接口，用于获取access_token和批量查询关注公众号的用户信息。

## 接口列表

### 1. 获取access_token

**接口地址：** `GET /parking/wechat-public/getAccessToken`

**接口描述：** 获取微信公众号的access_token，用于后续API调用。

**参考文档：** [微信官方文档](https://developers.weixin.qq.com/doc/service/api/base/api_getaccesstoken.html)

**请求参数：**
- `appid` (可选): 公众号的唯一凭证，如果不传则使用配置文件中的默认值
- `secret` (可选): 公众号的唯一凭证密钥，如果不传则使用配置文件中的默认值

**请求示例：**
```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken?appid=YOUR_APPID&secret=YOUR_SECRET"
```

**响应示例：**
```json
{
  "code": "0",
  "msg": "获取access_token成功",
  "data": {
    "access_token": "ACCESS_TOKEN",
    "expires_in": 7200,
    "expires_time": 1640995200000
  }
}
```

### 2. 批量获取用户基本信息

**接口地址：** `POST /parking/wechat-public/batchGetUserInfo`

**接口描述：** 批量获取关注公众号的用户基本信息。

**参考文档：** [微信官方文档](https://developers.weixin.qq.com/doc/service/api/usermanage/userinfo/api_batchuserinfo.html)

**请求参数：**
- `accessToken` (必需): 接口调用凭证
- `requestBody` (必需): JSON格式的请求体

**请求体格式：**
```json
{
  "openids": ["openid1", "openid2", "openid3"],
  "lang": "zh_CN"
}
```

**请求示例：**
```bash
curl -X POST "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openids": ["openid1", "openid2"],
    "lang": "zh_CN"
  }'
```

**响应示例：**
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
        "unionid": "unionid",
        "remark": "",
        "groupid": 0,
        "tagid_list": [],
        "subscribe_scene": "ADD_SCENE_OTHERS",
        "qr_scene": 0,
        "qr_scene_str": ""
      }
    ],
    "total_count": 1,
    "request_count": 2
  }
}
```

### 3. 获取单个用户基本信息

**接口地址：** `GET /parking/wechat-public/getUserInfo`

**接口描述：** 获取单个用户的基本信息。

**请求参数：**
- `accessToken` (必需): 接口调用凭证
- `openid` (必需): 用户的openid
- `lang` (可选): 返回国家地区语言版本，默认为zh_CN

**请求示例：**
```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserInfo?accessToken=ACCESS_TOKEN&openid=USER_OPENID&lang=zh_CN"
```

### 4. 获取关注用户列表

**接口地址：** `GET /parking/wechat-public/getUserList`

**接口描述：** 获取关注公众号的用户列表。

**请求参数：**
- `accessToken` (必需): 接口调用凭证
- `nextOpenid` (可选): 拉取列表的第一个用户的OPENID，不填默认从头开始拉取

**请求示例：**
```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserList?accessToken=ACCESS_TOKEN"
```

### 5. 检查access_token有效性

**接口地址：** `GET /parking/wechat-public/checkAccessToken`

**接口描述：** 检查access_token是否有效。

**请求参数：**
- `accessToken` (必需): 接口调用凭证

**请求示例：**
```bash
curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/checkAccessToken?accessToken=ACCESS_TOKEN"
```

## 配置说明

在 `application.yml` 文件中，可以配置默认的微信公众号参数：

```yaml
# 微信配置
wechat:
  # 微信公众号配置
  public:
    appid: wx7fcbbc5d885b630b        # 替换为您的公众号AppID
    secret: YOUR_SECRET_HERE         # 替换为您的公众号AppSecret
  # 其他配置...
```

## 使用流程

1. **获取access_token**
   ```bash
   # 使用配置文件中的默认值
   curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken"
   
   # 或传入自定义的appid和secret
   curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken?appid=YOUR_APPID&secret=YOUR_SECRET"
   ```

2. **获取用户列表**
   ```bash
   curl -X GET "http://www.xuerparking.cn:8543/parking/wechat-public/getUserList?accessToken=ACCESS_TOKEN"
   ```

3. **批量获取用户信息**
   ```bash
   curl -X POST "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "openids": ["openid1", "openid2", "openid3"],
       "lang": "zh_CN"
     }'
   ```

## 错误码说明

### 微信API常见错误码：

- `40001`: 获取access_token时AppSecret错误，或者access_token无效
- `40002`: 不合法的凭证类型
- `40013`: 不合法的AppID
- `40125`: 不合法的secret
- `40164`: 调用接口的IP地址不在白名单中
- `41004`: 缺少secret参数
- `42001`: access_token超时

### 本系统错误码：

- `0`: 成功
- `1`: 失败（具体错误信息见msg字段）

## 注意事项

1. **access_token有效期为7200秒（2小时）**，建议缓存并在过期前重新获取
2. **批量获取用户信息一次最多支持100个openid**
3. **所有接口都需要有效的access_token**（除了获取access_token接口本身）
4. **请妥善保管AppSecret**，避免泄露
5. **调用频率有限制**，请参考微信官方文档的限制说明

## 集成示例

### Java代码示例：

```java
// 获取access_token
@Autowired
private RestTemplate restTemplate;

public String getAccessToken() {
    String url = "http://www.xuerparking.cn:8543/parking/wechat-public/getAccessToken";
    ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("access_token");
}

// 批量获取用户信息
public List<Map<String, Object>> batchGetUserInfo(String accessToken, List<String> openids) {
    String url = "http://www.xuerparking.cn:8543/parking/wechat-public/batchGetUserInfo?accessToken=" + accessToken;
    
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("openids", openids);
    requestBody.put("lang", "zh_CN");
    
    ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (List<Map<String, Object>>) data.get("user_info_list");
}
```

### 前端JavaScript示例：

```javascript
// 获取access_token
async function getAccessToken() {
    const response = await fetch('/parking/wechat-public/getAccessToken');
    const result = await response.json();
    if (result.code === '0') {
        return result.data.access_token;
    }
    throw new Error(result.msg);
}

// 批量获取用户信息
async function batchGetUserInfo(accessToken, openids) {
    const response = await fetch(`/parking/wechat-public/batchGetUserInfo?accessToken=${accessToken}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            openids: openids,
            lang: 'zh_CN'
        })
    });
    
    const result = await response.json();
    if (result.code === '0') {
        return result.data.user_info_list;
    }
    throw new Error(result.msg);
}
```

## 相关链接

- [微信公众平台开发者文档](https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Overview.html)
- [微信公众号基础接口](https://developers.weixin.qq.com/doc/service/api/base/api_getaccesstoken.html)
- [用户管理接口](https://developers.weixin.qq.com/doc/offiaccount/User_Management/Getting_a_User_List.html)

---

*最后更新：2024年1月* 