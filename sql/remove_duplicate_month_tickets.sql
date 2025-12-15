根据判定大同时考虑外键约束，不删除被 violations 表引用的记录

-- 
-- 步骤1: 查看重复数据统计
-- ============AXWHERE car_no IS NOT NULL 
  AND park_name IS NOT NULL
============================================
-- 详情-- ============================================
WHEEXSTS存在其他相同 car_no + park_name 但 id 更大1FROMmonth_tickmt2
  WHERE mt2. = mt.car_noANDmt2.park_name= mt.ANDm2.d > t.id
)
ORDER BY carno, ak_name, ;

-- ============================================
-- 步骤3: 查看哪些记录被 vioaton 表引用（这些可能无法删除）
-- ============================================
SELECT 
    m.idmt.car_no,
mt.park_name,
    COUTv.volation_count mtINNERJOINviolationsvNv.mh_tckt = m.idWHEREEXISTS(
 SELE  FRMonh_tick mt2
    WHERE mt2mt  2mt  2m.
)GOUP BY,
ORDER BYviolaon_count DESC
-- ============================================4 安全删除方案（推荐）
--删除重复记录，但不删除 ============================================
DELETEFROMmnth_ick
WHERE d IN(     id FROM(     SELECTmt
       FROMmnth_ck mt
        WHERE EXISTS (
            -- 存在其他相同 car_n + park_ame 但  更大的记录
            SELECT 1         FROM m2           WHEREmt2.car_no=             ANDmt2.park_name=           AND2id > m.d
        )
        AND NOT EXISTS (
            -- 不被 vaton 表引用
            SELECT 1                    WHERE) )asep
);
--============================================--步骤5:处理被引用的重复记录（手动处理）
--如果有重复记录被violations引用，需要特殊处理
--============================================

--5.1查找被引用的重复记录
mt.idasold_id,
mt.mt.mt_keep.idasnew_id,
COUNT(v.id)asviolaon_count
FROM onthtick mt
INNER JOIN vaton v ON v.month_icket_id = mt.idINNERJOIN(
--找到每组应该保留的记录（id最大）
SELECTcar_no, park_name, AXmaxmt_max_maxmt_maxmtINNERJOIonh_ck tkee ON mt_kep.m_ax.max<mt_max.max_i  -- 不是保留的那条记录
GROUP BY mtid, mt.car_no, mt.par_nam, mt_ke.ORDER BY violation_count DESC5.2（将旧 id 指向新 id）注意：执行前请确认 5.1 查询结果，确映射关系正确AXmaxmt_max_maxmt    _maxmtmt_maxmax<mt_maxmax.3再次步骤4的操作更新完引用后，再次WHEid id FROM (SELECT mt.idFROM moth_tick tWHERE EXISTS (  SELECT 1         mt2WERE2mt          2mt          2m.d
        )
        AND NOT EXISTS (
            SELECT 1
            FROM vaton v             v.month_ticket_id =
        )
    )astm
)
-- ============================================
-- ============================================
 6.1确认ck
WHERE ar_no IS NOT NULL 
  AND par_name IS NOT NULL;

-- 6.2 查看删除统计
SELECT 
    '删除前总记录数' as description,
    COUNT(*) as count
FROM month_tick
-- ============================================（可选，再次）
-- ============================================执行前确保已经没有重复数据============
-- 执行顺序说明
-- 
/*推荐
先-3情况如果3没有结果（没有被引用重复），直接4即可3（有被引用的重复记录）则按顺序：
   - 步骤5.1：查看需要更新的引用关系
   - 5.2：     .3：剩余的记录45可选：建议先在测试环境执行务必每个步骤后检查结果再继续 car_no 或 park_name 为NULL的记录不会被处理
*/