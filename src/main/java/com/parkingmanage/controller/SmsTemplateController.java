package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.SmsTemplate;
import com.parkingmanage.service.SmsTemplateService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 短信模板前端控制器
 * </p>
 *
 * @author 系统管理员
 */
@RestController
@RequestMapping("/parking/smsTemplate")
@CrossOrigin(origins = "*")
@Api(tags = "短信模板管理")
public class SmsTemplateController {

    @Resource
    private SmsTemplateService smsTemplateService;

    @ApiOperation("添加短信模板")
    @PostMapping
    public ResponseEntity<Result> insertSmsTemplate(@RequestBody SmsTemplate smsTemplate) {
        Result result = new Result();
        
        // 检查模板CODE是否重复
        if (smsTemplateService.checkDuplicateTemplateCode(smsTemplate.getTemplateCode(), null)) {
            result.setCode("1");
            result.setMsg("模板CODE已存在，添加失败！");
            return ResponseEntity.ok(result);
        }
        
        smsTemplate.setDeleted(0);
        smsTemplateService.save(smsTemplate);
        result.setMsg("添加成功！");
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改短信模板")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody SmsTemplate smsTemplate) {
        Result result = new Result();
        
        // 检查模板CODE是否重复（排除自己）
        if (smsTemplateService.checkDuplicateTemplateCode(smsTemplate.getTemplateCode(), smsTemplate.getId())) {
            result.setCode("1");
            result.setMsg("模板CODE已存在，修改失败！");
            return ResponseEntity.ok(result);
        }
        
        smsTemplateService.updateById(smsTemplate);
        result.setMsg("修改成功！");
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除短信模板")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Integer id) {
        Result result = new Result();
        boolean success = smsTemplateService.removeById(id);
        if (success) {
            result.setMsg("删除成功！");
        } else {
            result.setCode("1");
            result.setMsg("删除失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("查询所有短信模板")
    @GetMapping("/list")
    public ResponseEntity<Result> getAllSmsTemplates() {
        Result result = new Result();
        List<SmsTemplate> list = smsTemplateService.list();
        result.setData(list);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据ID查询短信模板")
    @GetMapping("/{id}")
    public ResponseEntity<Result> getById(@PathVariable Integer id) {
        Result result = new Result();
        SmsTemplate smsTemplate = smsTemplateService.getById(id);
        result.setData(smsTemplate);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据模板类型查询")
    @GetMapping("/type/{templateType}")
    public ResponseEntity<Result> getByTemplateType(@PathVariable Integer templateType) {
        Result result = new Result();
        List<SmsTemplate> list = smsTemplateService.getByTemplateType(templateType);
        result.setData(list);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("分页查询短信模板")
    @GetMapping("/page")
    public IPage<SmsTemplate> findPage(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer templateType,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        List<SmsTemplate> smsTemplateList = smsTemplateService.queryListSmsTemplate(templateName, templateType);
        
        // 按照创建时间排序
        List<SmsTemplate> sortedList = smsTemplateList.stream()
                .sorted(Comparator.comparing(SmsTemplate::getGmtCreate).reversed())
                .collect(Collectors.toList());
        
        return PageUtils.getPage(sortedList, pageNum, pageSize);
    }
}

