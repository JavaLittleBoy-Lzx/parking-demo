package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Illegalregiste;
import com.parkingmanage.query.DeleteFileQuery;
import com.parkingmanage.service.IllegalregisteService;
import com.parkingmanage.utils.PageUtils;
import com.parkingmanage.utils.ResourceUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 <p>
  前端控制器
 </p>

 @author MLH
 @since 2022-09-18
*/
@RestController
@RequestMapping("/parking/illegalregiste")
public class IllegalregisteController {

    @Resource
    private IllegalregisteService illegalregisteService;

    @ApiOperation("添加")
    @PostMapping("/insertIllegalregiste")
    public R<Object> insertRegiste(@RequestBody Illegalregiste illegalregisteS) {
        return ResourceUtil.buildR(illegalregisteService.save(illegalregisteS));
    }
    @PostMapping("/delete")
    @ResponseBody
    public  boolean deleteFile(@RequestBody DeleteFileQuery fileQuery) throws IOException {
        String filePath="";
        System.out.println("****************************************");
        System.out.println(fileQuery);
        String date=fileQuery.getDate();
        String fileName=fileQuery.getFile();
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        //在jar包所在目录下生成一个upload文件夹用来存储上传的图片
        String dirPath = jarF.getParentFile().toString()+"/classes/static/images/";
        try {
            OutputStream os = new FileOutputStream("test.txt");
            os.write(dirPath.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (IOException e) {

        }

    filePath=dirPath+date+'/'+fileName;
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    @PostMapping("/upload")
    @ResponseBody
    public R<Map<String,Object>>  upload(@RequestParam("file") MultipartFile file)throws IOException {
        //如果文件不存在上传失败
        Map<String,Object>  fileMap=new HashMap<>();
        Map<String,Object>  dataMap=new HashMap<>();
        ArrayList<Object> uploadList = new ArrayList<>();

        Logger logger = (Logger) LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
        if (file.isEmpty()) {
            logger.info("上传失败");
            System.out.println("文件不存在");
            fileMap.put("flag","1");
            fileMap.put("file","");
            uploadList.add(fileMap);
            dataMap.put("data",uploadList);
            return R.ok(dataMap);
        }
        //获取文件名字
        String fileName = file.getOriginalFilename();
        logger.info("文件名为: " + fileName);
        String suffixName = fileName.substring(fileName.lastIndexOf("."));
        //设置编译后文件存在路径
        //  String path = ClassUtils.getDefaultClassLoader().getResource("").getPath()+"static/images/";
        String datePath= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        //获取项目路径
        String paths ="";
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        //在jar包所在目录下生成一个upload文件夹用来存储上传的图片
        String dirPath = jarF.getParentFile().toString()+"/static/images/";
        System.out.println(dirPath);
        System.out.println(paths);
        paths=dirPath + datePath;
        String newImg = UUID.randomUUID() + suffixName;
        String path = paths +"/" + newImg;
        logger.info("构造路径" + path);
        File dest =new File(path);
        // 检测是否存在目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();//新建文件夹
        }
        try {
            //文件上传
            file.transferTo(dest);
            logger.info("上传成功");
            fileMap.put("flag","0");
            fileMap.put("date", datePath);
            fileMap.put("file", newImg);
            // fileMap.put("file","/" + datePath+"/" + newImg);
            dataMap.put("data",fileMap);
            uploadList.add(fileMap);
            dataMap.put("data",uploadList);
            System.out.println("上传成功");
            System.out.println(dataMap);
            return R.ok(dataMap);
        }catch (IOException e) {
            logger.error(e.toString(), e);
       }
        fileMap.put("flag","1");
        fileMap.put("date", "");
        fileMap.put("file","");
        uploadList.add(fileMap);
        dataMap.put("data",uploadList);
        System.out.println(dataMap);
        return R.ok(dataMap);
    }

    @ApiOperation("分页查询")
    @GetMapping("/allpage")
    public IPage<Illegalregiste> allPage(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String operatordate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Illegalregiste> IllegalregisteList = illegalregisteService.allManage(community,plateNumber,operatordate);
        //按照设备名和申请日期排序
        List<Illegalregiste> asServices = IllegalregisteList.stream().sorted(Comparator.comparing(Illegalregiste::getProvince).
                thenComparing(Illegalregiste::getCity).thenComparing(Illegalregiste::getDistrict).thenComparing(Illegalregiste::getCommunity).
                thenComparing(Illegalregiste::getBuilding).thenComparing(Illegalregiste::getUnits)
                .thenComparing(Illegalregiste::getOperatordate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

