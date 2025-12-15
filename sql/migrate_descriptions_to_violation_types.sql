-- ============================================
-- 数据迁移：将 violation_descriptions 表中的描述迁移到 violation_types 表的 description 字段
-- 作者：System
-- 日期：2025-01-31
-- ============================================

USE project_lzx;

-- 1. 备份当前数据（可选）
-- CREATE TABLE violation_descriptions_backup AS SELECT * FROM violation_descriptions;
-- CREATE TABLE violation_types_backup AS SELECT * FROM violation_types;

-- 2. 将 violation_descriptions 表中的描述合并到 violation_types 表
-- 按照 violation_type_code 和 park_name 分组，将多条描述用分号连接

UPDATE violation_types vt
SET vt.description = (
    SELECT GROUP_CONCAT(vd.description_text SEPARATOR '; ')
    FROM violation_descriptions vd
    WHERE vd.violation_type_code COLLATE utf8mb4_unicode_ci = vt.type_code COLLATE utf8mb4_unicode_ci
      AND (vd.park_name COLLATE utf8mb4_unicode_ci = vt.park_name COLLATE utf8mb4_unicode_ci 
           OR (vd.park_name IS NULL AND vt.park_name IS NULL))
    GROUP BY vd.violation_type_code, vd.park_name
)
WHERE EXISTS (
    SELECT 1
    FROM violation_descriptions vd
    WHERE vd.violation_type_code COLLATE utf8mb4_unicode_ci = vt.type_code COLLATE utf8mb4_unicode_ci
      AND (vd.park_name COLLATE utf8mb4_unicode_ci = vt.park_name COLLATE utf8mb4_unicode_ci 
           OR (vd.park_name IS NULL AND vt.park_name IS NULL))
);

-- 3. 验证迁移结果
SELECT 
    vt.id,
    vt.type_name,
    vt.type_code,
    vt.park_name,
    vt.description,
    (SELECT COUNT(*) FROM violation_descriptions vd 
     WHERE vd.violation_type_code = vt.type_code 
       AND (vd.park_name = vt.park_name OR (vd.park_name IS NULL AND vt.park_name IS NULL))
    ) as original_desc_count
FROM violation_types vt
ORDER BY vt.id;

-- 4. 清空 violation_descriptions 表（可选，如果确认迁移成功）
-- TRUNCATE TABLE violation_descriptions;

-- 5. 或者删除已迁移的描述（保留未关联的描述）
-- DELETE FROM violation_descriptions 
-- WHERE violation_type_code IN (SELECT type_code FROM violation_types);

COMMIT;
