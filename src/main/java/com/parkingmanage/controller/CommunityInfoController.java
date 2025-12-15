package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.CommunityInfo;
import com.parkingmanage.service.CommunityInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 小区基本信息控制器
 * 管理小区的基本信息和图片，避免数据冗余
 * 
 * @author system
 * @since 2024-12-01
 */
@RestController
@RequestMapping("/parking/community-info")
@CrossOrigin(origins = "*")
@Api(tags = "小区基本信息管理")
public class CommunityInfoController {
    
    @Resource
    private CommunityInfoService communityInfoService;

    @ApiOperation("添加小区基本信息")
    @PostMapping
    public ResponseEntity<Result<CommunityInfo>> add(@RequestBody CommunityInfo communityInfo) {
        Result<CommunityInfo> result = new Result<>();
        try {
            // 检查是否已存在
            CommunityInfo existing = communityInfoService.getByLocation(
                communityInfo.getProvince(),
                communityInfo.getCity(),
                communityInfo.getDistrict(),
                communityInfo.getCommunity()
            );
            
            if (existing != null) {
                result.setCode("-1");
                result.setMsg("该小区已存在");
                result.setData(existing);
            } else {
                communityInfoService.save(communityInfo);
                result.setCode("0");
                result.setMsg("添加成功");
                result.setData(communityInfo);
                System.out.println("✅ [小区信息] 添加成功: " + communityInfo.getCommunity());
            }
        } catch (Exception e) {
            System.err.println("❌ [小区信息] 添加失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("添加失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改小区基本信息")
    @PutMapping
    public ResponseEntity<Result<CommunityInfo>> update(@RequestBody CommunityInfo communityInfo) {
        Result<CommunityInfo> result = new Result<>();
        try {
            communityInfoService.updateById(communityInfo);
            result.setCode("0");
            result.setMsg("修改成功");
            result.setData(communityInfo);
            System.out.println("✅ [小区信息] 修改成功: " + communityInfo.getCommunity());
        } catch (Exception e) {
            System.err.println("❌ [小区信息] 修改失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("修改失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("更新小区图片")
    @PutMapping("/updateImages")
    public ResponseEntity<Result<CommunityInfo>> updateImages(
            @RequestParam Integer id,
            @RequestParam(required = false) String images,
            @RequestParam(required = false) String mainImage) {
        
        System.out.println("🖼️ [小区图片] 更新请求: id=" + id + ", mainImage=" + mainImage);
        
        Result<CommunityInfo> result = new Result<>();
        try {
            CommunityInfo communityInfo = communityInfoService.getById(id);
            if (communityInfo == null) {
                result.setCode("-1");
                result.setMsg("小区不存在");
                return ResponseEntity.ok(result);
            }
            
            if (images != null) {
                communityInfo.setImages(images);
            }
            if (mainImage != null) {
                communityInfo.setMainImage(mainImage);
            }
            
            communityInfoService.updateById(communityInfo);
            
            result.setCode("0");
            result.setMsg("图片更新成功");
            result.setData(communityInfo);
            System.out.println("✅ [小区图片] 更新成功");
        } catch (Exception e) {
            System.err.println("❌ [小区图片] 更新失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("图片更新失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据小区名称获取小区图片")
    @GetMapping("/getCommunityImages")
    public ResponseEntity<Result<CommunityInfo>> getCommunityImages(@RequestParam String communityName) {
        System.out.println("🖼️ [小区图片] 查询小区图片: " + communityName);
        
        Result<CommunityInfo> result = new Result<>();
        try {
            CommunityInfo communityInfo = communityInfoService.getByCommunityName(communityName);
            
            if (communityInfo != null && communityInfo.getMainImage() != null) {
                result.setCode("0");
                result.setMsg("查询成功");
                result.setData(communityInfo);
            } else {
                result.setCode("0");
                result.setMsg("该小区暂无图片");
                result.setData(null);
                System.out.println("⚠️ [小区图片] 该小区暂无图片");
            }
        } catch (Exception e) {
            System.err.println("❌ [小区图片] 查询失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("根据ID删除小区信息")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Boolean>> delete(@PathVariable Integer id) {
        Result<Boolean> result = new Result<>();
        try {
            boolean success = communityInfoService.removeById(id);
            result.setCode("0");
            result.setMsg(success ? "删除成功" : "删除失败");
            result.setData(success);
        } catch (Exception e) {
            System.err.println("❌ [小区信息] 删除失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("查询所有小区基本信息")
    @GetMapping("/list")
    public ResponseEntity<Result<List<CommunityInfo>>> list(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district) {
        
        Result<List<CommunityInfo>> result = new Result<>();
        try {
            QueryWrapper<CommunityInfo> wrapper = new QueryWrapper<>();
            
            if (province != null && !province.isEmpty()) {
                wrapper.eq("province", province);
            }
            if (city != null && !city.isEmpty()) {
                wrapper.eq("city", city);
            }
            if (district != null && !district.isEmpty()) {
                wrapper.eq("district", district);
            }
            if (community != null && !community.isEmpty()) {
                wrapper.like("community", community);
            }
            
            List<CommunityInfo> list = communityInfoService.list(wrapper);
            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(list);
        } catch (Exception e) {
            System.err.println("❌ [小区信息] 查询失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @ApiOperation("分页查询小区基本信息")
    @GetMapping("/page")
    public ResponseEntity<Result<IPage<CommunityInfo>>> page(
            @RequestParam(required = false) String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        Result<IPage<CommunityInfo>> result = new Result<>();
        try {
            Page<CommunityInfo> page = new Page<>(pageNum, pageSize);
            QueryWrapper<CommunityInfo> wrapper = new QueryWrapper<>();
            
            if (community != null && !community.isEmpty()) {
                wrapper.like("community", community);
            }
            
            wrapper.orderByDesc("updated_at");
            
            IPage<CommunityInfo> pageResult = communityInfoService.page(page, wrapper);
            result.setCode("0");
            result.setMsg("查询成功");
            result.setData(pageResult);
        } catch (Exception e) {
            System.err.println("❌ [小区信息] 分页查询失败: " + e.getMessage());
            e.printStackTrace();
            result.setCode("-1");
            result.setMsg("查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
