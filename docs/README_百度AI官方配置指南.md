# 百度智能云车牌识别API官方配置指南

> 基于[百度智能云官方视频教程](https://cloud.baidu.com/video-center/video/741)整理

## 🎬 **官方视频要点**

百度智能云官方视频详细介绍了车牌识别API的使用方法，包含以下核心内容：
- Access Token获取机制
- 车牌识别API调用方法
- 错误码处理和最佳实践
- 参数配置和优化建议

## 📋 **完整配置流程**

### 步骤1: 开通百度智能云服务

1. **访问百度智能云控制台**
   - 登录：https://console.bce.baidu.com/
   - 进入"人工智能" → "文字识别OCR"

2. **开通车牌识别服务**
   ```
   服务名称：车牌识别
   免费额度：1000次/月
   计费方式：按调用次数计费
   ```

3. **创建应用获取密钥**
   - 应用名称：`停车管理系统车牌识别`
   - 应用类型：Web应用
   - 记录：`API Key` 和 `Secret Key`

### 步骤2: 项目配置

#### 1. 配置文件设置

在 `application.yml` 中配置：

```yaml
# 百度智能云车牌识别配置
baidu:
  ai:
    # 从百度智能云控制台获取
    api-key: "你的API_Key"          # 必填
    secret-key: "你的Secret_Key"    # 必填
    
    # API配置（一般不需要修改）
    base-url: "https://aip.baidubce.com"
    token-url: "/oauth/2.0/token"
    plate-url: "/rest/2.0/ocr/v1/license_plate"
    
    # 性能配置
    token-cache-minutes: 25         # Token缓存时间
    request-timeout: 30000          # 请求超时时间(毫秒)
    max-retries: 3                  # 最大重试次数
```

#### 2. 验证配置

启动应用后访问测试接口：

```bash
# 检查配置
curl http://www.xuerparking.cn:8080/api/plate/test/config

# 测试连接
curl -X POST http://www.xuerparking.cn:8080/api/plate/test/connection
```

## 🔑 **Access Token机制详解**

根据官方视频说明，百度AI使用OAuth 2.0认证：

### Token获取流程
```
1. 使用API Key + Secret Key → 获取Access Token
2. 使用Access Token → 调用车牌识别API
3. Token有效期30天，自动缓存管理
```

### 在我们的实现中
```java
// 自动获取和缓存Access Token
private String getAccessToken() {
    // 检查缓存的token是否有效
    if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
        return accessToken;  // 使用缓存
    }
    
    // 向百度服务器请求新token
    // POST https://aip.baidubce.com/oauth/2.0/token
    // grant_type=client_credentials&client_id=API_KEY&client_secret=SECRET_KEY
}
```

## 📊 **API调用参数说明**

### 请求参数
| 参数名 | 类型 | 必选 | 描述 |
|--------|------|------|------|
| image | string | 是 | 图像数据，base64编码，大小不超过4M |
| multi_detect | string | 否 | 是否检测多个车牌，默认false |

### 响应参数
```json
{
    "words_result": [
        {
            "color": "蓝色",              // 车牌颜色
            "number": "京A12345",         // 车牌号码
            "probability": {
                "average": 0.9534,        // 平均置信度
                "variance": 0.0042        // 置信度方差
            },
            "type": "普通汽车号牌",        // 车牌类型
            "location": {                 // 位置信息
                "left": 10,
                "top": 20, 
                "width": 100,
                "height": 30
            }
        }
    ],
    "words_result_num": 1
}
```

## ⚠️ **错误码处理**

根据官方文档，常见错误码及处理：

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 18 | QPS超限 | 降低请求频率 |
| 19 | 请求总量超限 | 升级服务配额 |
| 216200 | 未检测到车牌 | 提示用户重新拍摄 |
| 216103 | 图片过大 | 压缩图片后重试 |
| 110 | Access token无效 | 重新获取token |

## 🚀 **最佳实践建议**

### 1. 图片优化
```javascript
// 前端图片处理
const optimizeImage = (file) => {
    // 压缩到适当大小（建议1-2MB）
    const maxSize = 2 * 1024 * 1024; // 2MB
    const quality = file.size > maxSize ? 0.8 : 0.9;
    
    return compressImage(file, quality);
}
```

### 2. 错误重试机制
```java
// 自动重试逻辑
@Retryable(value = {Exception.class}, maxAttempts = 3)
public PlateRecognitionResult recognizePlateWithRetry(String base64Image) {
    return recognizePlateFromBase64(base64Image);
}
```

### 3. 缓存机制
```java
// Redis缓存识别结果（相同图片不重复识别）
@Cacheable(value = "plateRecognition", key = "#imageHash")
public PlateRecognitionResult recognizeWithCache(String imageHash, String base64Image) {
    return callBaiduAPI(base64Image);
}
```

## 📱 **前端集成代码**

### 完整的前端调用示例：

```vue
<template>
  <view class="plate-recognition">
    <!-- 摄像头组件 -->
    <camera 
      device-position="back" 
      flash="off"
      @error="onCameraError"
      style="width: 100%; height: 400rpx;"
    >
      <view class="camera-overlay">
        <view class="scan-frame"></view>
        <text class="scan-tip">请将车牌对准扫描框</text>
      </view>
    </camera>
    
    <!-- 拍照按钮 -->
    <button @click="takePhoto" :disabled="isRecognizing">
      {{ isRecognizing ? '识别中...' : '拍照识别' }}
    </button>
    
    <!-- 识别结果 -->
    <view v-if="result" class="result">
      <text>车牌号：{{ result.plateNumber }}</text>
      <text>颜色：{{ result.color }}</text>
      <text>置信度：{{ result.confidence }}%</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      isRecognizing: false,
      result: null
    }
  },
  methods: {
    async takePhoto() {
      this.isRecognizing = true;
      
      try {
        // 拍照
        const ctx = uni.createCameraContext();
        const photo = await this.capturePhoto(ctx);
        
        // 转换为base64
        const base64 = await this.fileToBase64(photo.tempImagePath);
        
        // 调用识别API
        const response = await uni.request({
          url: 'http://www.xuerparking.cn:8080/api/plate/recognize',
          method: 'POST',
          data: { image: base64 },
          header: { 'Content-Type': 'application/json' }
        });
        
        if (response.data.success) {
          this.result = response.data.data;
          this.$emit('plate-recognized', this.result);
        } else {
          uni.showToast({
            title: response.data.message,
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('识别失败:', error);
        uni.showToast({
          title: '识别失败，请重试',
          icon: 'none'
        });
      } finally {
        this.isRecognizing = false;
      }
    },
    
    capturePhoto(ctx) {
      return new Promise((resolve, reject) => {
        ctx.takePhoto({
          quality: 'high',
          success: resolve,
          fail: reject
        });
      });
    },
    
    fileToBase64(filePath) {
      return new Promise((resolve, reject) => {
        uni.getFileSystemManager().readFile({
          filePath,
          encoding: 'base64',
          success: (res) => resolve(res.data),
          fail: reject
        });
      });
    }
  }
}
</script>
```

## 🔍 **调试和监控**

### 1. 日志监控
```bash
# 查看识别日志
tail -f logs/parking.log | grep "车牌识别"

# 查看错误日志
tail -f logs/error.log | grep "PlateRecognition"
```

### 2. 性能监控
```java
// 添加性能监控
@Timed(name = "plate.recognition", description = "车牌识别性能监控")
public PlateRecognitionResult recognizePlate(String base64Image) {
    long startTime = System.currentTimeMillis();
    try {
        return doRecognition(base64Image);
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        log.info("车牌识别耗时: {}ms", duration);
    }
}
```

## 📞 **技术支持**

- **官方文档**: https://ai.baidu.com/ai-doc/OCR/dk3h7y5vr
- **官方视频**: https://cloud.baidu.com/video-center/video/741
- **技术支持**: 4008-777-818
- **开发者社区**: 百度AI开发者论坛

---

按照以上官方指南配置，你的车牌识别功能将更加稳定和高效！ 