package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.TempCarPreEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 临时车预进场数据Mapper接口
 * 
 * @author lzx
 */
@Mapper
public interface TempCarPreEntryMapper extends BaseMapper<TempCarPreEntry> {
    
    /**
     * 根据车牌号和车场编码查询预进场记录（不限制是否使用）
     */
    @Select("SELECT * FROM temp_car_pre_entry WHERE plate_number = #{plateNumber} AND park_code = #{parkCode} ORDER BY create_time DESC LIMIT 1")
    TempCarPreEntry findByPlateNumberAndParkCode(@Param("plateNumber") String plateNumber, 
                                               @Param("parkCode") String parkCode);
    
    /**
     * 根据车牌号和车场编码查询未使用的预进场记录
     */
    @Select("SELECT * FROM temp_car_pre_entry WHERE plate_number = #{plateNumber} AND park_code = #{parkCode} AND used = 0 ORDER BY create_time DESC LIMIT 1")
    TempCarPreEntry findUnusedByPlateNumberAndParkCode(@Param("plateNumber") String plateNumber, 
                                                      @Param("parkCode") String parkCode);
    
    /**
     * 更新预进场时间
     */
    @Update("UPDATE temp_car_pre_entry SET pre_enter_time = #{preEnterTime}, used = 0 WHERE id = #{id}")
    int updatePreEnterTime(@Param("id") Integer id, 
                          @Param("preEnterTime") String preEnterTime);
    
    /**
     * 标记预进场记录为已使用
     */
    @Update("UPDATE temp_car_pre_entry SET used = 1 WHERE id = #{id}")
    int markAsUsed(@Param("id") Integer id);
} 