package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.*;
import com.parkingmanage.mapper.PurchaseMapper;
import com.parkingmanage.mapper.SupplierMapper;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.PurchaseService;
import com.parkingmanage.service.UserService;
import com.parkingmanage.utils.BeanConvertUtils;
import com.parkingmanage.vo.PurchaseVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 采申请管理 服务实现类
 </p>

 @author yuli
 @since 2022-02-28
*/
@Service
public class PurchaseServiceImpl extends ServiceImpl<PurchaseMapper, Purchase> implements PurchaseService {

    @Resource
    private DepartmentService departmentService;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private UserService userService;
    @Resource
    private FilesServiceImp filesService;
    @Resource
    private DeviceService deviceService;

    @Override
    public List<PurchaseVo> queryPurchas(Integer departmentId, String deviceName, String applicationTime, String audiusTime, Integer audiusUserId) {
        List<Purchase> purchaseList = this.list(Wrappers.<Purchase>lambdaQuery().eq(departmentId != null, Purchase::getDepartmentId, departmentId)
                .like(StringUtils.hasLength(deviceName), Purchase::getDeviceName, deviceName)
                .like(StringUtils.hasLength(applicationTime), Purchase::getApplicationTime, applicationTime)
                .like(StringUtils.hasLength(audiusTime), Purchase::getAudiusTime, audiusTime)
                .eq(audiusUserId != null,
                        Purchase::getAudiusUserId,
                        audiusUserId));
        List<PurchaseVo> purchaseVoList = BeanConvertUtils.copyListProperties(purchaseList, PurchaseVo::new);
        for (PurchaseVo purchaseVo : purchaseVoList) {
            Department department = departmentService.getById(purchaseVo.getDepartmentId());
            if (!ObjectUtils.isEmpty(department)) {
                purchaseVo.setDepartmentName(department.getDepartmentName());
            }
            User appUser = userService.getById(purchaseVo.getApplicantUserId());
            if (!ObjectUtils.isEmpty(appUser)) {
                purchaseVo.setApplicantName(appUser.getUserName());
            }
            User User = userService.getById(purchaseVo.getAudiusUserId());
            if (!ObjectUtils.isEmpty(User)) {
                purchaseVo.setAudiustName(User.getUserName());
            }
            Supplier supplier = supplierMapper.selectById(purchaseVo.getSupplierId());
            if (!ObjectUtils.isEmpty(supplier)) {
                purchaseVo.setSupplierName(supplier.getSupplierName());
            }
        }
        if (!CollectionUtils.isEmpty(purchaseVoList)) {
            purchaseVoList = purchaseVoList.stream().sorted(Comparator.comparingInt(Purchase::getAuditStatus)).collect(Collectors.toList());
        }
        return purchaseVoList;
    }

    @Override
    public void exportPurchaseManagement(HttpServletResponse response) {

        List<PurchaseVo> purchaseVos = queryPurchas(null, null, null, null, null);
        this.WExcelData(response, purchaseVos);
    }

    private void WExcelData(HttpServletResponse response, List<PurchaseVo> purchaseVos) {
        if (!CollectionUtils.isEmpty(purchaseVos)) {
            for (PurchaseVo purchaseVo : purchaseVos) {
                if (purchaseVo.getAuditStatus().equals(1)) {
                    purchaseVo.setAuditStatusType("待采购");
                }
                if (purchaseVo.getAuditStatus().equals(0)) {
                    purchaseVo.setAuditStatusType("审核中");
                }
                if (purchaseVo.getAuditStatus().equals(2)) {
                    purchaseVo.setAuditStatusType("审核未通过");
                }
                if (purchaseVo.getAuditStatus().equals(3)) {
                    purchaseVo.setAuditStatusType("已采购");
                }
            }
        }
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("deviceName", "设备名称");
        writer.addHeaderAlias("devicePrice", "设备价格");
        writer.addHeaderAlias("orderQuantity", "预购数量");
        writer.addHeaderAlias("departmentName", "申请部门");
        writer.addHeaderAlias("applicantName", "申请人");
        writer.addHeaderAlias("applicationTime", "申请时间");
        writer.addHeaderAlias("applicationReason", "申请原因");
        writer.addHeaderAlias("supplierName", "厂商");
        writer.addHeaderAlias("audiustName", "审批人");
        writer.addHeaderAlias("auditStatusType", "审批状态");
        writer.addHeaderAlias("fileReason", "审批意见");
        writer.setOnlyAlias(true);
        writer.write(purchaseVos, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=purchasexcel.xlsx");
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.flush(out, true);
        writer.close();
        IoUtil.close(out);
    }

    @Override
    public void queryExportPurchaseManagement(Integer departmentId, String deviceName, String audiusTime, Integer auditStatus, Integer audiusUserId, HttpServletResponse response) {
        List<PurchaseVo> purchaseVos = queryPurchas(departmentId, deviceName, null, audiusTime, audiusUserId);
        if (auditStatus != null) {
            purchaseVos = purchaseVos.stream().filter((PurchaseVo purchaseVo) -> auditStatus != null && auditStatus.equals(purchaseVo.getAuditStatus())).collect(Collectors.toList());
        }
        this.WExcelData(response, purchaseVos);
    }

    @Override
    @Transactional
    public void updatePurchaseVoucher(MultipartFile[] files, Integer purchaseId) {
        if (purchaseId == null) {
            throw new CustomException("99", "purchaseId必须填写");
        }
        Purchase purchase = this.getById(purchaseId);
        //批量添加设备
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < purchase.getOrderQuantity(); i++) {
            Device device = new Device();
            //生成设备编码
            StringBuffer str = new StringBuffer(String.valueOf(System.currentTimeMillis()));
            str.append(i);
            device.setDeviceCode(str.toString());
            device.setDeviceName(purchase.getDeviceName());
            device.setDevicePrice(purchase.getDevicePrice());
            device.setDepartmentId(purchase.getDepartmentId());
            device.setModel(purchase.getModel());
            device.setDeviceType(purchase.getDeviceType());
            device.setPurchaseTime(LocalDateTime.now());
            devices.add(device);
        }
        deviceService.saveBatch(devices);
        if (files != null && files.length > 0) {
            List<String> PurchaseVoucher = filesService.getStringUrls(files);
            if (!CollectionUtils.isEmpty(PurchaseVoucher)) {
                purchase.setPurchaseVoucher(org.apache.commons.lang3.StringUtils.join(PurchaseVoucher, ","));
            }
        }
        purchase.setPurchaseTime(LocalDateTime.now());
        purchase.setAuditStatus(3);
        this.updateById(purchase);
    }
}
