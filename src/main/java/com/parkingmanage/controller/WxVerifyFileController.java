package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Allocation;
import com.parkingmanage.entity.Device;
import com.parkingmanage.service.AllocationService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 调拨管理 前端控制器
 </p>
 @author yuli
 @since 2022-03-02
*/
@RestController
@RequestMapping("/verify")
public class WxVerifyFileController {

    @RequestMapping(value="hI08PoN6uk.txt")
    public void S4mH7ZmUAn(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        ClassPathResource classPathResource = new ClassPathResource("hI08PoN6uk.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()));
            PrintWriter writer = response.getWriter();
            writer.write(br.readLine());
            writer.flush();
            writer.close();
        }
    @RequestMapping(value="/butler/hI08PoN6uk.txt")
    public void butler(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        ClassPathResource classPathResource = new ClassPathResource("/butler/hI08PoN6uk.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()));
        PrintWriter writer = response.getWriter();
        writer.write(br.readLine());
        writer.flush();
        writer.close();
    }
    @RequestMapping(value="MP_verify_aZPqsZ3MkkiTCId3.txt")
    public void MP_service(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        ClassPathResource classPathResource = new ClassPathResource("MP_verify_aZPqsZ3MkkiTCId3.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()));
        PrintWriter writer = response.getWriter();
        writer.write(br.readLine());
        writer.flush();
        writer.close();
    }
    @RequestMapping(value="/butler/MP_verify_aZPqsZ3MkkiTCId3.txt")
    public void service(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        ClassPathResource classPathResource = new ClassPathResource("/butler/MP_verify_aZPqsZ3MkkiTCId3.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()));
        PrintWriter writer = response.getWriter();
        writer.write(br.readLine());
        writer.flush();
        writer.close();
    }
}