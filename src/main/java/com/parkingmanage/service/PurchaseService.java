package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Purchase;
import com.parkingmanage.vo.PurchaseVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 采申请管理 服务类
 </p>

 @author yuli
 @since 2022-02-28
*/
public interface PurchaseService extends IService<Purchase> {
    // Page<PurchaseVo> querypage();

    /**
     申请采购设备
     @param departmentId
     @param deviceName
     @param applicationTime
     @return
    */
    List<PurchaseVo> queryPurchas(Integer departmentId,String deviceName,String applicationTime,String audiusTime,Integer audiusUserId);

    /**
     导出
     @param response
    */
    void exportPurchaseManagement(HttpServletResponse response);

    /**

     @param departmentId
     @param deviceName
     @param applicationTime
     @param audiusTime
     @param response
    */
    void queryExportPurchaseManagement(Integer departmentId,String deviceName,String audiusTime,Integer auditStatus,Integer audiusUserId,HttpServletResponse response);

    /**

     @param files
     @param purchaseId
    */
    void updatePurchaseVoucher(MultipartFile[] files, Integer purchaseId);
}
