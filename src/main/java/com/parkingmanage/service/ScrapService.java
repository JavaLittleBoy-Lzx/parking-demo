package com.parkingmanage.service;

import com.parkingmanage.entity.Scrap;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.vo.ScrapMaintenceDto;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 报废管理 服务类
 </p>

 @author yuli
 @since 2022-03-04
*/
public interface ScrapService extends IService<Scrap> {
    /**
     新增
     @param scrap
    */
    void saveScrap(Scrap scrap);

    /**
     list 解析
     @param deviceName
     @param scrapDate
     @return
    */
    List<Scrap> queryListScrap(String deviceName, String scrapDate,String deviceCode);

    /**
     修改
     @param scrap
    */
    void updateScrapAndDevice(Scrap scrap);

    /**
     导出
     @param deviceName
     @param deviceCode
     @param scrapDate
     @param response
    */
    void exportScrap(String deviceName, String deviceCode, String scrapDate, HttpServletResponse response);

    /**
     修改合格新增
     @param scrapMaintenceDto
    */
    void auditScrapAndMaintence(ScrapMaintenceDto scrapMaintenceDto);
}
