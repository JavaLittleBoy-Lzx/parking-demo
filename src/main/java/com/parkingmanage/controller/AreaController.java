package com.parkingmanage.controller;


import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Area;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Department;
import com.parkingmanage.query.TransmitAreaQuery;
import com.parkingmanage.service.AreaService;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.vo.AreaResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2022-09-08
 */
@RestController
@RequestMapping("/parking/area")
public class AreaController {
    @Resource
    private ButlerService butlerService;
    @Resource
    private AreaService areaService;

    @ApiOperation("添加")
    @PostMapping("/insertArea")
    public ResponseEntity<Result> insertArea(@RequestBody InsertAreaRequest request) {
        System.out.println("=== insertArea 接口被调用 ===");
        System.out.println("province: " + request.getProvince());
        System.out.println("city: " + request.getCity());
        System.out.println("district: " + request.getDistrict());
        System.out.println("community: " + request.getCommunity());
        System.out.println("usercode: " + request.getUsercode());
        System.out.println("username: " + request.getUsername());
        System.out.println("arrayId数量: " + (request.getArrayId() != null ? request.getArrayId().size() : 0));
        
        Butler butler = butlerService.getByUsercode(
            request.getProvince(), 
            request.getCity(), 
            request.getDistrict(), 
            request.getCommunity(), 
            request.getUsercode()
        );
        String openid = butler.getOpenid();
        areaService.deleteArea(
            request.getProvince(), 
            request.getCity(), 
            request.getDistrict(), 
            request.getCommunity(), 
            request.getUsercode()
        );
        areaService.insertArea(
            request.getUsercode(), 
            request.getUsername(), 
            openid, 
            request.getArrayId()
        );
        
        Result result = new Result();
        result.setCode("0");
        result.setMsg("权限设置成功");
        System.out.println("权限保存完成");
        return ResponseEntity.ok(result);
    }
    
    // 请求体对象
    public static class InsertAreaRequest {
        private String province;
        private String city;
        private String district;
        private String community;
        private String usercode;
        private String username;
        private List<Integer> arrayId;
        
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getCommunity() { return community; }
        public void setCommunity(String community) { this.community = community; }
        public String getUsercode() { return usercode; }
        public void setUsercode(String usercode) { this.usercode = usercode; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<Integer> getArrayId() { return arrayId; }
        public void setArrayId(List<Integer> arrayId) { this.arrayId = arrayId; }
    }
    @ApiOperation("添加")
    @PostMapping("/batsave")
    public ResponseEntity<Result> batSave(@RequestBody List<Community> communitys) {
        System.out.println(communitys);
        Area area = new Area();
        if (!communitys.isEmpty()) {
            String openid = communitys.get(0).getOpenid();
            areaService.deleteByOpenid(openid);
            Iterator<Community> iter = communitys.iterator();
            while (iter.hasNext()) {
                Community community = iter.next();
                area.setProvince(community.getProvince());
                area.setCity(community.getCity());
                area.setDistrict(community.getDistrict());
                area.setCommunity(community.getCommunity());
                area.setBuilding(community.getBuilding());
                area.setUnits(community.getUnits());
                area.setFloor(community.getFloor());
                area.setOpenid(community.getOpenid());
                area.setUsername(community.getUsername());
                areaService.save(area);
            }
        }
        return ResponseEntity.ok(new Result());
    }
    @ApiOperation("查询单条")
    @GetMapping("/{openid}")
    public Area getByOpenId(@PathVariable String openid) {
        Area area = areaService.getByOpenId(openid);
        return area;
    }

    @ApiOperation("查询单条")
    @GetMapping("/getAreaByOpenId/{openid}")
    public List<AreaResult> getAreaByOpenId(@PathVariable String openid) {
        List<AreaResult> areaResult = areaService.getAreaByOpenId(openid);
        return areaResult;
    }

    @ApiOperation("查询单条")
    @GetMapping("/transmit")
    @ResponseBody
    public List<Area> getTransmitByOpenId(TransmitAreaQuery transmitAreaQuery) {
//        System.out.println("000000000000000000000000000");
//        System.out.println(transmitAreaQuery);
        Area area = areaService.getByOpenId(transmitAreaQuery.getOpenid());
        transmitAreaQuery.setProvince(area.getProvince());
        transmitAreaQuery.setCity(area.getCity());
        transmitAreaQuery.setDistrict(area.getDistrict());
        transmitAreaQuery.setCommunity(area.getCommunity());
        return areaService.getTransmitByOpenId(transmitAreaQuery);
    }

    @ApiOperation("查询单条")
    @GetMapping("/getparking")
    public Area getParkingInfo(String province, String city, String district, String community, String building) {
//        System.out.println("99999999999900000000000000000");
//        System.out.println(community);
        return areaService.getParkingInfo(province, city, district, community, building);
    }
//    public R<Map<String,Object>> getMemberInfoByOpenId(@PathVariable String openid) {
//        Area area=areaService.getMemberInfoByOpenId(openid);
//        Map<String,Object>  wxMap=new HashMap<>();
//        if (area == null) {
//            wxMap.put("count",0);
//            wxMap.put("data",area);
//        }else {
//            wxMap.put("count",1);
//            wxMap.put("data",area);
//        }
//
//        HashMap<String, Object> dataMap = new HashMap<>();
//        dataMap.put("data",wxMap);
//        return R.ok(dataMap);
//
//    }
}

