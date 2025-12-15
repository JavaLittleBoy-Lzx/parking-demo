package com.parkingmanage.service.impl;

import com.parkingmanage.common.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 @PROJECT_NAME: orientation
 @PACKAGE_NAME: com.orientation.service.impl
 @NAME: FilesService
 @author:yuli
 @DATE: 2022/1/9 2:44
 @description: 文件上传服务
*/
@Slf4j
@Service
public class FilesServiceImp {

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     图片上传
     @param file
     @return
    */
    public List<String> getStringUrls(MultipartFile[] file) {
        List<String> fileUrlList = new ArrayList<>();
        if (file != null && file.length > 0) {
            File savePathFile = new File(uploadPath);
            if (!savePathFile.exists()) {
                //若不存在该目录，则创建目录
                savePathFile.mkdir();
            }
            for (MultipartFile multipartFile : file) {
                String end = Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(multipartFile.getOriginalFilename().lastIndexOf("."));
                //转换为小写
                String suffix = end.toLowerCase();
                System.out.println(end);
                if (!suffix.equals(".jpg") && !suffix.equals(".jpeg") && !suffix.equals(".png")/* || suffix.equals(".gif")*/) {
                    throw new CustomException("18", "文件格式不对，请重新选择");
                }
                //文件名先产生一个uuid 在和真是文件名拼接起来，防止文件重复上传失败
                String fileName = UUID.randomUUID().toString() + multipartFile.getOriginalFilename();
                // 得到文件存取位置
                //保存
                try {
                    //将文件保存指定目录
                    multipartFile.transferTo(new File(uploadPath + fileName));
                } catch (Exception e) {
                    log.error("保存文件异常{}", e);
                    e.printStackTrace();
                    throw new CustomException("18", "保存文件异常");
                }
                //项目url，这里可以使用常量或者去数据字典获取相应的url前缀；
                String fileUrl = "http://www.xuerparking.cn:9999";
                //文件获取路径
                fileUrl = fileUrl + "/uploadfile/" + fileName;
                fileUrlList.add(fileUrl);
            }
        }
        return fileUrlList;
    }


}
