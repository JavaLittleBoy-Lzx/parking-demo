#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复月票订单SQL文件中的数据拆分问题
1. 修正房间号拆分逻辑
2. 去除车牌号中的"绿"字
3. 将building简写改为全称
"""

import re
import sys
import io

# 设置标准输出编码为UTF-8
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Building名称映射表
BUILDING_MAP = {
    '香': '香榭丽舍',
    '凡': '凡尔赛',
    '维': '维也纳',
    '巴': '巴塞罗纳',
    '卢': '卢森堡',
    '白': '白金汉',
    '佛': '佛罗伦萨'
}

def parse_room_number(ownername):
    """
    解析房间号，返回(building, units, floor, roomnumber)
    例如：
    - 巴C11-2101 -> ('巴', 'C', '11', '2101')
    - 白C4-1402 -> ('白', 'C', '4', '1402')
    - 香A-1019 -> ('香', 'A', '', '1019')  # 没有楼层，不需要修改
    """
    # 匹配模式：建筑简称 + 字母 + 可选数字 + '-' + 房间号
    # 例如：巴C11-2101, 白C4-1402, 香A-1019
    
    # 第一种模式：文字 + 大写字母 + 数字 + '-' + 房间号
    # 如：巴C11-2101, 白C4-1402
    pattern1 = r'^([香凡维巴卢白佛])([A-Z])(\d+)-(\d+)$'
    match = re.match(pattern1, ownername)
    if match:
        building = match.group(1)
        units = match.group(2)
        floor = match.group(3)
        roomnumber = match.group(4)
        return (building, units, floor, roomnumber)
    
    # 第二种模式：文字 + 大写字母 + '-' + 房间号（字母后没有数字）
    # 如：香A-1019
    pattern2 = r'^([香凡维巴卢白佛])([A-Z])-(\d+)$'
    match = re.match(pattern2, ownername)
    if match:
        return None  # 这种格式不需要修改
    
    # 第三种模式：其他格式
    return None

def clean_plate_number(plates):
    """去除车牌号中的"绿"字"""
    if not plates:
        return plates
    return plates.replace('绿', '')

def get_building_fullname(building_short):
    """将building简写转换为全称"""
    return BUILDING_MAP.get(building_short, building_short)

def process_insert_statement(line):
    """处理一条INSERT语句"""
    # 提取VALUES部分
    values_match = re.search(r"VALUES\s*\((.*?)\);", line)
    if not values_match:
        return line
    
    values_str = values_match.group(1)
    
    # 分割字段值（处理引号中的逗号）
    values = []
    current = []
    in_quotes = False
    for char in values_str:
        if char == "'" and (not current or current[-1] != '\\'):
            in_quotes = not in_quotes
        current.append(char)
        if char == ',' and not in_quotes:
            values.append(''.join(current[:-1]).strip())
            current = []
    if current:
        values.append(''.join(current).strip())
    
    # 字段索引
    # 0: province, 1: city, 2: district, 3: community, 4: building, 
    # 5: units, 6: floor, 7: roomnumber, 8: ownername, 9: ownerphone, 
    # 10: plates, 11: parkingspaces, 12: isaudit, 13: permitverify
    
    if len(values) < 14:
        return line
    
    modified = False
    
    # 获取ownername（去除引号）
    ownername = values[8].strip("'")
    
    # 尝试解析房间号
    parsed = parse_room_number(ownername)
    if parsed:
        building_short, units, floor, roomnumber = parsed
        
        # 更新building为全称
        building_full = get_building_fullname(building_short)
        if values[4] != f"'{building_full}'":
            values[4] = f"'{building_full}'"
            modified = True
        
        # 更新units, floor, roomnumber
        if values[5] != f"'{units}'":
            values[5] = f"'{units}'"
            modified = True
        if values[6] != f"'{floor}'":
            values[6] = f"'{floor}'"
            modified = True
        if values[7] != f"'{roomnumber}'":
            values[7] = f"'{roomnumber}'"
            modified = True
    else:
        # 即使不需要重新拆分，也要更新building全称
        building_short = values[4].strip("'")
        building_full = get_building_fullname(building_short)
        if values[4] != f"'{building_full}'":
            values[4] = f"'{building_full}'"
            modified = True
    
    # 处理车牌号，去除"绿"字
    plates = values[10].strip("'")
    cleaned_plates = clean_plate_number(plates)
    if plates != cleaned_plates:
        values[10] = f"'{cleaned_plates}'"
        modified = True
    
    # 重新组装INSERT语句
    if modified:
        new_values = ', '.join(values)
        new_line = re.sub(r"VALUES\s*\(.*?\);", f"VALUES ({new_values});", line)
        return new_line
    
    return line

def main():
    input_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_split.sql'
    output_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_split_fixed.sql'
    
    print("开始处理SQL文件...")
    
    processed_count = 0
    total_lines = 0
    insert_count = 0
    
    with open(input_file, 'r', encoding='utf-8') as f_in:
        lines = f_in.readlines()
    
    total_lines = len(lines)
    
    with open(output_file, 'w', encoding='utf-8') as f_out:
        i = 0
        while i < len(lines):
            line = lines[i]
            
            # 检查是否是INSERT语句的开始
            if line.strip().startswith('INSERT INTO'):
                # 读取下一行（VALUES部分）
                if i + 1 < len(lines):
                    insert_count += 1
                    insert_line = line
                    values_line = lines[i + 1]
                    
                    # 合并两行
                    full_statement = insert_line.rstrip() + '\n' + values_line
                    
                    # 处理完整的INSERT语句
                    old_statement = full_statement
                    new_statement = process_insert_statement(full_statement)
                    
                    # 写入处理后的语句
                    f_out.write(new_statement)
                    
                    if new_statement != old_statement:
                        processed_count += 1
                        # 调试：打印前3条修改的记录
                        if processed_count <= 3:
                            print(f"\n修改 #{processed_count}:")
                            print(f"原VALUES: {values_line[:80]}...")
                            new_values = new_statement.split('\n')[1] if '\n' in new_statement else new_statement
                            print(f"新VALUES: {new_values[:80]}...")
                    
                    i += 2  # 跳过VALUES行
                else:
                    f_out.write(line)
                    i += 1
            else:
                f_out.write(line)
                i += 1
    
    print(f"\n处理完成!")
    print(f"总行数: {total_lines}")
    print(f"INSERT语句数: {insert_count}")
    print(f"修改的INSERT语句数: {processed_count}")
    print(f"输出文件: {output_file}")

if __name__ == '__main__':
    main()
