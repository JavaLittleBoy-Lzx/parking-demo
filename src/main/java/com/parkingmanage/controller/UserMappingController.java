package com.parkingmanage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.parkingmanage.mapper.UserMappingMapper;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.common.Result;

/**
 * 用户映射控制器
 * 处理用户昵称相关的查询操作
 */
@RestController
@RequestMapping("/parking/user")
@CrossOrigin(origins = "*")
public class UserMappingController {

    @Autowired
    private UserMappingMapper userMappingMapper;

    /**
     * 检查昵称是否存在于user_mapping表中
     * @param requestBody 包含昵称的请求体
     * @param request HTTP请求对象
     * @return 查询结果
     */
    @PostMapping("/checkNicknameExists")
    public Result<Object> checkNicknameExists(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        System.out.println("========================================");
        System.out.println(" [DEBUG] checkNicknameExists 方法被调用");
        System.out.println(" [DEBUG] 请求体内容: " + requestBody);
        System.out.println("========================================");
        try {
            String nickname = (String) requestBody.get("nickname");
            System.out.println(" [DEBUG] 解析出的昵称: " + nickname);
            // 参数验证
            if (nickname == null || nickname.trim().isEmpty()) {
                System.out.println(" [DEBUG] 昵称为空，返回错误");
                return Result.error("昵称不能为空");
            }
            // 去除首尾空格
            nickname = nickname.trim();
            System.out.println(" [DEBUG] 去除首尾空格后的昵称: " + nickname);
            // 查询数据库
            int count = userMappingMapper.countByNickname(nickname);
            System.out.println(" [DEBUG] 查询数据库结果: " + count);
            boolean exists = count > 0;
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("exists", exists);
            resultData.put("nickname", nickname);
            resultData.put("count", count);
            // 记录日志
            System.out.println("📱 [昵称查询] 查询昵称: " + nickname + ", 结果: " + (exists ? "存在" : "不存在") + ", 记录数: " + count);
            return Result.success(resultData);
        } catch (Exception e) {
            System.err.println("❌ [昵称查询] 查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据昵称获取用户详细信息
     * @param requestBody 包含昵称的请求体
     * @param request HTTP请求对象
     * @return 用户详细信息
     */
    @PostMapping("/getUserByNickname")
    public Result<Object> getUserByNickname(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String nickname = (String) requestBody.get("nickname");
            // 参数验证
            if (nickname == null || nickname.trim().isEmpty()) {
                return Result.error("昵称不能为空");
            }
            // 去除首尾空格
            nickname = nickname.trim();
            // 查询数据库
            List<UserMapping> userMappings = userMappingMapper.findByNickname(nickname);
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("nickname", nickname);
            resultData.put("found", !userMappings.isEmpty());
            resultData.put("count", userMappings.size());
            resultData.put("users", userMappings);
            // 记录日志
            System.out.println("📱 [用户查询] 根据昵称查询用户: " + nickname + ", 找到记录数: " + userMappings.size());
            return Result.success(resultData);
        } catch (Exception e) {
            System.err.println("❌ [用户查询] 查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 保存或更新用户昵称信息
     * @param requestBody 包含用户信息的请求体
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/saveUserNickname")
    public Result<Object> saveUserNickname(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String nickname = (String) requestBody.get("nickname");
            String openid = (String) requestBody.get("openid");
            String phone = (String) requestBody.get("phone");
            
            // 参数验证
            if (nickname == null || nickname.trim().isEmpty()) {
                return Result.error("昵称不能为空");
            }
            
            if (openid == null || openid.trim().isEmpty()) {
                return Result.error("OpenID不能为空");
            }
            // 去除首尾空格
            nickname = nickname.trim();
            openid = openid.trim();
            if (phone != null) phone = phone.trim();
            // 检查是否已存在记录
            UserMapping existingMapping = userMappingMapper.findByOpenid(openid);
            UserMapping userMapping;
            if (existingMapping != null) {
                // 更新现有记录
                existingMapping.setNickname(nickname);
                if (phone != null && !phone.isEmpty()) {
                    existingMapping.setPhone(phone);
                }
                existingMapping.setUpdateTime(new java.util.Date());
                userMappingMapper.updateUserMapping(existingMapping);
                userMapping = existingMapping;
                System.out.println("📱 [用户保存] 更新用户昵称信息: " + nickname + " (OpenID: " + openid + ")");
            } else {
                // 创建新记录
                userMapping = new UserMapping();
                userMapping.setNickname(nickname);
                userMapping.setOpenid(openid);
                userMapping.setPhone(phone);
                userMapping.setCreateTime(new java.util.Date());
                userMapping.setUpdateTime(new java.util.Date());
                userMappingMapper.insertUserMapping(userMapping);
                System.out.println("📱 [用户保存] 创建新用户昵称记录: " + nickname + " (OpenID: " + openid + ")");
            }
            
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("nickname", nickname);
            resultData.put("openid", openid);
            resultData.put("isNew", existingMapping == null);
            return Result.success(resultData);
            
        } catch (Exception e) {
            System.err.println("❌ [用户保存] 保存过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("保存过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据手机号查询用户昵称信息
     * @param requestBody 包含手机号的请求体
     * @param request HTTP请求对象
     * @return 用户昵称信息
     */
    @PostMapping("/getUserByPhone")
    public Result<Object> getUserByPhone(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String phone = (String) requestBody.get("phone");
            
            // 参数验证
            if (phone == null || phone.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            // 去除首尾空格
            phone = phone.trim();
            
            // 查询数据库
            List<UserMapping> userMappings = userMappingMapper.findByPhone(phone);
            
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("phone", phone);
            resultData.put("found", !userMappings.isEmpty());
            resultData.put("count", userMappings.size());
            resultData.put("users", userMappings);
            
            // 如果找到记录，返回第一个用户的昵称
            if (!userMappings.isEmpty()) {
                UserMapping firstUser = userMappings.get(0);
                resultData.put("nickname", firstUser.getNickname());
                resultData.put("openid", firstUser.getOpenid());
            }
            
            // 记录日志
            System.out.println("📱 [手机号查询] 根据手机号查询用户: " + phone + ", 找到记录数: " + userMappings.size());
            return Result.success(resultData);
            
        } catch (Exception e) {
            System.err.println("❌ [手机号查询] 查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 删除用户映射记录
     * @param requestBody 包含删除条件的请求体
     * @param request HTTP请求对象
     * @return 删除结果
     */
    @PostMapping("/deleteUserMapping")
    public Result<Object> deleteUserMapping(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String openid = (String) requestBody.get("openid");
            String nickname = (String) requestBody.get("nickname");
            
            // 参数验证 - 至少需要一个删除条件
            if ((openid == null || openid.trim().isEmpty()) && (nickname == null || nickname.trim().isEmpty())) {
                return Result.error("删除条件不能为空，需要提供OpenID或昵称");
            }
            
            int deletedCount = 0;
            
            // 根据条件删除
            if (openid != null && !openid.trim().isEmpty()) {
                deletedCount = userMappingMapper.deleteByOpenid(openid.trim());
                System.out.println("📱 [用户删除] 根据OpenID删除用户映射: " + openid + ", 删除记录数: " + deletedCount);
            } else if (nickname != null && !nickname.trim().isEmpty()) {
                deletedCount = userMappingMapper.deleteByNickname(nickname.trim());
                System.out.println("📱 [用户删除] 根据昵称删除用户映射: " + nickname + ", 删除记录数: " + deletedCount);
            }
            
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("deleted", deletedCount > 0);
            resultData.put("deletedCount", deletedCount);
            
            return Result.success(resultData);
            
        } catch (Exception e) {
            System.err.println("❌ [用户删除] 删除过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("删除过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 检查用户公众号关注状态
     * @param requestBody 包含用户标识信息的请求体
     * @param request HTTP请求对象
     * @return 用户关注状态
     */
    @PostMapping("/checkFollowStatus")
    public Result<Object> checkFollowStatus(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String phone = (String) requestBody.get("phone");
            String nickname = (String) requestBody.get("nickname");
            String openid = (String) requestBody.get("openid");
            
            // 参数验证 - 至少需要一个查询条件
            if ((phone == null || phone.trim().isEmpty()) && 
                (nickname == null || nickname.trim().isEmpty()) && 
                (openid == null || openid.trim().isEmpty())) {
                return Result.error("查询条件不能为空，需要提供手机号、昵称或OpenID中的至少一个");
            }
            
            // 去除首尾空格
            if (phone != null) phone = phone.trim();
            if (nickname != null) nickname = nickname.trim();
            if (openid != null) openid = openid.trim();
            
            // 查询用户信息（按优先级：openid > phone > nickname）
            UserMapping userMapping = userMappingMapper.findByMultipleConditions(openid, phone, nickname);
            
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            
            if (userMapping != null) {
                // 用户存在，返回关注状态
                int isFollowed = userMapping.getIsFollowed() != null ? userMapping.getIsFollowed() : 0;
                resultData.put("is_followed", isFollowed);
                resultData.put("user_found", true);
                resultData.put("nickname", userMapping.getNickname());
                resultData.put("phone", userMapping.getPhone());
                resultData.put("openid", userMapping.getOpenid());
                resultData.put("follow_time", userMapping.getFollowTime());
                
                // 记录日志
                System.out.println("📱 [关注状态查询] 查询用户关注状态 - 昵称: " + userMapping.getNickname() + 
                                 ", 手机号: " + userMapping.getPhone() + 
                                 ", OpenID: " + userMapping.getOpenid() + 
                                 ", 关注状态: " + (isFollowed == 1 ? "已关注" : "未关注"));
            } else {
                // 用户不存在，默认未关注
                resultData.put("is_followed", 0);
                resultData.put("user_found", false);
                resultData.put("message", "用户不存在，默认为未关注状态");
                
                // 记录日志
                System.out.println("📱 [关注状态查询] 用户不存在 - 查询条件: phone=" + phone + 
                                 ", nickname=" + nickname + 
                                 ", openid=" + openid + 
                                 ", 默认返回未关注状态");
            }
            
            return Result.success(resultData);
            
        } catch (Exception e) {
            System.err.println("❌ [关注状态查询] 查询过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询过程中发生错误: " + e.getMessage());
        }
    }
    @PostMapping("/updatePhone")
    public Result<Object> updatePhone(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String openid = (String) requestBody.get("openid");
            String nickname = (String) requestBody.get("nickname");
            String phone = (String) requestBody.get("phone");
            if (openid == null || openid.trim().isEmpty()) {
                return Result.error("OpenID不能为空");
            }
            openid = openid.trim();
            if (nickname != null) nickname = nickname.trim();
            if (phone != null) phone = phone.trim();

            UserMapping existingMapping = userMappingMapper.findByOpenid(openid);

            if (existingMapping != null) {
                System.out.println("📱 [用户创建] 现有记录 OpenID=" + openid + ", 当前手机号=" + existingMapping.getPhone());

                if (nickname != null && !nickname.isEmpty()) {
                    existingMapping.setNickname(nickname);
                }

                // 仅当现有手机号为空时才更新
                if (phone != null && !phone.isEmpty()) {
                    String existingPhone = existingMapping.getPhone();
                    if (existingPhone == null || existingPhone.trim().isEmpty()) {
                        existingMapping.setPhone(phone);
                        System.out.println("✅ [用户创建] 手机号为空，更新为: " + phone);
                    } else {
                        System.out.println("ℹ️ [用户创建] 手机号已存在(" + existingPhone + ")，不覆盖");
                    }
                }

                existingMapping.setUpdateTime(new java.util.Date());
                userMappingMapper.updateUserMapping(existingMapping);

                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("phone", existingMapping.getPhone());
                return Result.success(resultData);
            } else {
                UserMapping userMapping = new UserMapping();
                userMapping.setOpenid(openid);
                userMapping.setPhone(phone);
                userMappingMapper.insertUserMapping(userMapping);

                System.out.println("✅ [用户创建] 新建: OpenID=" + openid + ", nickname=" + nickname + ", phone=" + phone);

                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("phone", phone);
                return Result.success(resultData);
            }
        } catch (Exception e) {
            System.err.println("❌ [用户创建] 错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error("创建错误");
        }
    }
} 