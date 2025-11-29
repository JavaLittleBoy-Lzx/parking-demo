package com.parkingmanage.controller;


import com.parkingmanage.service.ExportDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.ws.Response;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lzx
 * @since 2024-08-19
 */
@RestController
@RequestMapping("/export-data")
public class ExportDataController {
    @Autowired
    private ExportDataService exportDataService;

}

