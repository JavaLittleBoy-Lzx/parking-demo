-- ============================================
-- 简化版数据迁移：将 violation_descriptions 表中的描述迁移到 violation_types 表
-- ============================================

USE project_lzx;

-- 手动迁移每条记录
-- 1. 违规停车 (illegal_parking, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'illegal_parking' AND park_name = '东北林业大学'
)
WHERE type_code = 'illegal_parking' AND park_name = '东北林业大学';

-- 2. 占用消防通道 (fire_lane, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'fire_lane' AND park_name = '东北林业大学'
)
WHERE type_code = 'fire_lane' AND park_name = '东北林业大学';

-- 3. 占用绿化带 (green_belt, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'green_belt' AND park_name IS NULL
)
WHERE type_code = 'green_belt' AND park_name = '东北林业大学';

-- 4. 占用盲道 (blind_road, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'blind_road' AND park_name IS NULL
)
WHERE type_code = 'blind_road' AND park_name = '东北林业大学';

-- 5. 超时停车 (overtime_parking, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'overtime_parking' AND park_name IS NULL
)
WHERE type_code = 'overtime_parking' AND park_name = '东北林业大学';

-- 6. 未按位停车 (out_of_space, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'out_of_space' AND park_name IS NULL
)
WHERE type_code = 'out_of_space' AND park_name = '东北林业大学';

-- 7. 占用他人车位 (occupy_others_space, 东北林业大学)
UPDATE violation_types 
SET description = (
    SELECT GROUP_CONCAT(description_text SEPARATOR '; ')
    FROM violation_descriptions
    WHERE violation_type_code = 'occupy_others_space' AND park_name IS NULL
)
WHERE type_code = 'occupy_others_space' AND park_name = '东北林业大学';

-- 验证结果
SELECT id, type_name, type_code, park_name, description 
FROM violation_types 
ORDER BY id;

COMMIT;
