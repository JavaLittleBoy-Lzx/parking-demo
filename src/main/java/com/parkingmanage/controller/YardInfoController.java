package com.parkingmanage.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.SmsTemplate;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.service.YardSmsTemplateRelationService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 <p>
 前端控制器
 * </p>
 *
 * @author 李子雄
 *
 */
@RestController
@RequestMapping("/parking/yardInfo")
@CrossOrigin(origins = "*")
public class YardInfoController {
    @Resource
    private YardInfoService yardInfoService;

    @Resource
    private YardSmsTemplateRelationService yardSmsTemplateRelationService;

    @Autowired
    public AIKEConfig aikeConfig;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertYardInfo(@RequestBody Map<String, Object> params) {
        Result result = new Result();
        try {
            // 提取车场信息
            YardInfo yardInfo = new YardInfo();
            yardInfo.setYardCode((String) params.get("yardCode"));
            yardInfo.setYardName((String) params.get("yardName"));
            
            // 安全地转换 yardNo
            Object yardNoObj = params.get("yardNo");
            if (yardNoObj != null) {
                if (yardNoObj instanceof Integer) {
                    yardInfo.setYardNo((Integer) yardNoObj);
                } else if (yardNoObj instanceof String) {
                    yardInfo.setYardNo(Integer.parseInt((String) yardNoObj));
                }
            }
            
            int num = yardInfoService.duplicate(yardInfo);
            if (num == 0) {
                yardInfoService.save(yardInfo);
                
                // 处理短信模板关联（去重后添加）
                Object templateIdsObj = params.get("smsTemplateIds");
                if (templateIdsObj != null) {
                    List<Integer> templateIds = new ArrayList<>();
                    if (templateIdsObj instanceof List) {
                        for (Object obj : (List<?>) templateIdsObj) {
                            Integer templateId = null;
                            if (obj instanceof Integer) {
                                templateId = (Integer) obj;
                            } else if (obj instanceof String) {
                                templateId = Integer.parseInt((String) obj);
                            }
                            // 去重：检查是否已存在
                            if (templateId != null && !templateIds.contains(templateId)) {
                                templateIds.add(templateId);
                            }
                        }
                    }
                    
                    if (!templateIds.isEmpty()) {
                        yardSmsTemplateRelationService.updateYardTemplates(yardInfo.getId(), templateIds);
                    }
                }
                
                // 添加成功，设置响应
                result.setCode(null);
                result.setMsg("添加成功");
            } else {
                result.setCode("1");
                result.setMsg("数据重复，增加失败！");
            }
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("添加失败：" + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Map<String, Object> params) {
        Result result = new Result();
        try {
            // 提取车场信息
            YardInfo yardInfo = new YardInfo();
            
            // 安全地转换 id
            Object idObj = params.get("id");
            if (idObj != null) {
                if (idObj instanceof Integer) {
                    yardInfo.setId((Integer) idObj);
                } else if (idObj instanceof String) {
                    yardInfo.setId(Integer.parseInt((String) idObj));
                }
            }
            
            yardInfo.setYardCode((String) params.get("yardCode"));
            yardInfo.setYardName((String) params.get("yardName"));
            
            // 安全地转换 yardNo
            Object yardNoObj = params.get("yardNo");
            if (yardNoObj != null) {
                if (yardNoObj instanceof Integer) {
                    yardInfo.setYardNo((Integer) yardNoObj);
                } else if (yardNoObj instanceof String) {
                    yardInfo.setYardNo(Integer.parseInt((String) yardNoObj));
                }
            }
            
            int num = yardInfoService.duplicate(yardInfo);
            if (num == 0) {
                yardInfoService.updateById(yardInfo);
                
                // 处理短信模板关联（去重后更新）
                Object templateIdsObj = params.get("smsTemplateIds");
                List<Integer> templateIds = new ArrayList<>();
                if (templateIdsObj != null && templateIdsObj instanceof List) {
                    for (Object obj : (List<?>) templateIdsObj) {
                        Integer templateId = null;
                        if (obj instanceof Integer) {
                            templateId = (Integer) obj;
                        } else if (obj instanceof String) {
                            templateId = Integer.parseInt((String) obj);
                        }
                        // 去重：检查是否已存在
                        if (templateId != null && !templateIds.contains(templateId)) {
                            templateIds.add(templateId);
                        }
                    }
                }
                
                yardSmsTemplateRelationService.updateYardTemplates(yardInfo.getId(), templateIds);
                
                // 修改成功，设置响应
                result.setCode(null);
                result.setMsg("修改成功");
            } else {
                result.setCode("1");
                result.setMsg("数据重复，修改失败！");
            }
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("修改失败：" + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return yardInfoService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/getAllYardInfo")
    public List<YardInfo> getAllYardInfo() {
        List<YardInfo> myquery = yardInfoService.list();
        return myquery;
    }

    @ApiOperation("查询所有车场名称")
    @GetMapping("/expYardName")
    public List<YardInfo> expYardNameList() {
        return yardInfoService.expYardNameList();
    }

    @ApiOperation("查询所有车场编码")
    @GetMapping("/yardCode")
    public List<String> yardCode(@RequestParam(required = false) String yardName) {
        return yardInfoService.yardCode(yardName);
    }
    @ApiOperation("获取授权停车场列表")
    @GetMapping("/getAuthParkCodes")
    public ResponseEntity getAuthParkCodes() {
        HashMap<String, Object> params = new HashMap<>();
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET,"getAuthParkCodes", params);
        return ResponseEntity.ok(data);
    }
    @ApiOperation("查询车场名称")
    @GetMapping("/yardName")
    public List<YardInfo> yardNameList() {
        ArrayList<YardInfo> yardInfos = new ArrayList<>();
        HashMap<String, String> params = new HashMap<>();
        String get = HttpClientUtil.doGet("https://www.xuerparking.cn:8543/parking/yardInfo/getAuthParkCodes",params);
//        String get = HttpClientUtil.doGet("http://www.xuerparking.cn:8543/parking/yardInfo/getAuthParkCodes",params);
//        System.out.println("查询车场名称接口：" + get);
        JSONObject jsonObject = JSONObject.parseObject(get);
        JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
        JSONObject jsonObjectDataData = (JSONObject) jsonObjectData.get("data");
        JSONArray jsonObjectDataDataparkInfoList = (JSONArray) jsonObjectDataData.get("parkInfoList");
        String parkName = "";
        for (int i = 0; i < jsonObjectDataDataparkInfoList.size(); i++) {
            JSONObject jsonObjectparkInfoList = (JSONObject)jsonObjectDataDataparkInfoList.get(i);
            parkName = (String)jsonObjectparkInfoList.get("parkName");
            String parkCode = (String)jsonObjectparkInfoList.get("parkCode");
            YardInfo yardInfo = new YardInfo();
            yardInfo.setYardName(parkName);
            yardInfo.setYardCode(parkCode);
            yardInfo.setDeleted(0);
            yardInfos.add(yardInfo);
        }
        //TODO 将查询到的结果存储到数据库中
        //先查询一下，若有就直接查询，没有添加
        YardInfo yardInfo = yardInfoService.yardByName(parkName);
        if (yardInfo == null) {
//            System.out.println("车场名称插入数据库" + yardInfo);
            yardInfoService.saveBatch(yardInfos);
        }else {
            yardInfoService.updateBatchById(yardInfos);
        }
//        System.out.println("车场名称列表：" + yardInfos);
        return yardInfos;
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<YardInfo> findPage(@RequestParam(required = false) String yardName,
                                       @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<YardInfo> yardInfoList = yardInfoService.queryListYardInfo(yardName);
        //按照设备名和申请日期排序
        List<YardInfo> asServices = yardInfoList.stream().sorted(Comparator.comparing(YardInfo::getYardName)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("查询车场关联的短信模板ID列表")
    @GetMapping("/{yardId}/templates")
    public ResponseEntity<Result> getYardTemplates(@PathVariable Integer yardId) {
        Result result = new Result();
        try {
            List<Integer> templateIds = yardSmsTemplateRelationService.getTemplateIdsByYardId(yardId);
            result.setData(templateIds);
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    @ApiOperation("查询车场关联的短信模板完整信息（包含code和signName）")
    @GetMapping("/{yardId}/sms-templates")
    public ResponseEntity<Result> getYardSmsTemplates(@PathVariable Integer yardId) {
        Result result = new Result();
        try {
            List<SmsTemplate> smsTemplates = yardSmsTemplateRelationService.getSmsTemplatesByYardId(yardId);
            result.setData(smsTemplates);
            result.setMsg("查询成功");
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败：" + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
    
    @ApiOperation("根据车场名称查询短信模板完整信息（包含code和signName）")
    @GetMapping("/sms-templates/by-name")
    public ResponseEntity<Result> getYardSmsTemplatesByName(@RequestParam String parkName) {
        Result result = new Result();
        try {
            // 1. 根据车场名称查询车场信息
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<YardInfo> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(YardInfo::getYardName, parkName)
                   .eq(YardInfo::getDeleted, 0);
            YardInfo yardInfo = yardInfoService.getOne(wrapper);
            
            if (yardInfo == null || yardInfo.getId() == null) {
                result.setCode("404");
                result.setMsg("未找到车场：" + parkName);
                return ResponseEntity.ok(result);
            }
            
            // 2. 查询该车场关联的短信模板
            List<SmsTemplate> smsTemplates = yardSmsTemplateRelationService.getSmsTemplatesByYardId(yardInfo.getId());
            result.setData(smsTemplates);
            result.setMsg("查询成功");
            
        } catch (Exception e) {
            result.setCode("1");
            result.setMsg("查询失败：" + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
}