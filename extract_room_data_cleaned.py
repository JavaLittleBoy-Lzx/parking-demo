#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
从月票订单SQL文件中提取building、units、floor、roomnumber等字段
生成用于插入到新表的SQL语句（处理int类型字段）
"""

import re
import os

def extract_insert_values(line):
    """从INSERT语句中提取VALUES部分的值"""
    match = re.search(r"VALUES\s*\((.*?)\);", line, re.IGNORECASE)
    if not match:
        return None
    
    values_str = match.group(1)
    
    # 解析字段值
    values = []
    current_value = ""
    in_quotes = False
    
    for char in values_str:
        if char == "'" and (not current_value or current_value[-1] != '\\'):
            in_quotes = not in_quotes
            current_value += char
        elif char == ',' and not in_quotes:
            values.append(current_value.strip())
            current_value = ""
        else:
            current_value += char
    
    if current_value:
        values.append(current_value.strip())
    
    return values

def clean_value(value):
    """清理字段值（去除引号）"""
    value = value.strip()
    if value.startswith("'") and value.endswith("'"):
        return value[1:-1]
    return value

def convert_to_int(value, default=None):
    """尝试将值转换为整数，失败则返回默认值"""
    try:
        # 处理负数
        if value.startswith('-'):
            return int(value)
        return int(value)
    except (ValueError, TypeError):
        return default

def process_sql_file(file_path):
    """处理SQL文件，提取需要的字段"""
    records = []
    skipped = []
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        lines = content.split('\n')
        
        current_statement = ""
        for line in lines:
            line = line.strip()
            if not line or line.startswith('--') or line.startswith('USE'):
                continue
            
            current_statement += " " + line
            
            if line.endswith(';'):
                if 'INSERT INTO' in current_statement:
                    values = extract_insert_values(current_statement)
                    if values and len(values) >= 8:
                        province = clean_value(values[0])
                        city = clean_value(values[1])
                        district = clean_value(values[2])
                        community = clean_value(values[3])
                        building = clean_value(values[4])
                        units_str = clean_value(values[5])
                        floor_str = clean_value(values[6])
                        roomnumber_str = clean_value(values[7])
                        
                        # 转换为int类型，无法转换的跳过
                        units = convert_to_int(units_str)
                        floor = convert_to_int(floor_str)
                        roomnumber = convert_to_int(roomnumber_str)
                        
                        # 只保留所有字段都能转换为int的记录
                        if units is not None and floor is not None and roomnumber is not None:
                            record = {
                                'province': province,
                                'city': city,
                                'district': district,
                                'community': community,
                                'building': building,
                                'units': units,
                                'floor': floor,
                                'roomnumber': roomnumber
                            }
                            records.append(record)
                        else:
                            # 记录跳过的数据
                            skipped.append({
                                'building': building,
                                'units': units_str,
                                'floor': floor_str,
                                'roomnumber': roomnumber_str,
                                'reason': 'Cannot convert to int'
                            })
                
                current_statement = ""
    
    return records, skipped

def remove_duplicates(records):
    """去除重复记录"""
    seen = set()
    unique_records = []
    
    for record in records:
        key = (
            record['province'],
            record['city'],
            record['district'],
            record['community'],
            record['building'],
            record['units'],
            record['floor'],
            record['roomnumber']
        )
        
        if key not in seen:
            seen.add(key)
            unique_records.append(record)
    
    return unique_records

def generate_insert_sql(records):
    """生成INSERT语句"""
    sql_lines = []
    sql_lines.append("-- Room information data import (cleaned for int fields)")
    sql_lines.append("-- Generated time: Auto-generated")
    sql_lines.append(f"-- Total records: {len(records)}")
    sql_lines.append("")
    sql_lines.append("USE parking_management;")
    sql_lines.append("")
    
    for record in records:
        sql = (
            f"INSERT INTO rooms (province, city, district, community, building, units, floor, roomnumber, is_audit, audit_start_time, audit_end_time)\n"
            f"VALUES ('{record['province']}', '{record['city']}', '{record['district']}', '{record['community']}', "
            f"'{record['building']}', {record['units']}, {record['floor']}, {record['roomnumber']}, NULL, NULL, NULL);"
        )
        sql_lines.append(sql)
    
    return '\n'.join(sql_lines)

def save_skipped_records(skipped_records, output_file):
    """保存被跳过的记录到文件"""
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("# Skipped Records Report\n\n")
        f.write(f"Total skipped: {len(skipped_records)}\n\n")
        f.write("| Building | Units | Floor | Room | Reason |\n")
        f.write("|----------|-------|-------|------|--------|\n")
        
        for record in skipped_records:
            f.write(f"| {record['building']} | {record['units']} | {record['floor']} | {record['roomnumber']} | {record['reason']} |\n")

def main():
    """Main function"""
    file1 = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_final.sql'
    file2 = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_manual_fixed.sql'
    output_file = r'd:\PakingDemo\parking-demo\room_data_import_cleaned.sql'
    skipped_file = r'd:\PakingDemo\parking-demo\skipped_records.md'
    
    print("Processing first SQL file...")
    records1, skipped1 = process_sql_file(file1)
    print(f"  Extracted {len(records1)} valid records from first file")
    print(f"  Skipped {len(skipped1)} records from first file")
    
    print("Processing second SQL file...")
    records2, skipped2 = process_sql_file(file2)
    print(f"  Extracted {len(records2)} valid records from second file")
    print(f"  Skipped {len(skipped2)} records from second file")
    
    all_records = records1 + records2
    all_skipped = skipped1 + skipped2
    print(f"Total records after merge: {len(all_records)}")
    print(f"Total skipped: {len(all_skipped)}")
    
    print("Removing duplicates...")
    unique_records = remove_duplicates(all_records)
    print(f"Unique records: {len(unique_records)}")
    
    print("Generating INSERT statements...")
    sql_content = generate_insert_sql(unique_records)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(sql_content)
    
    print("Saving skipped records report...")
    save_skipped_records(all_skipped, skipped_file)
    
    print(f"\nDone! SQL file saved to: {output_file}")
    print(f"Skipped records report saved to: {skipped_file}")
    print(f"Total valid INSERT statements: {len(unique_records)}")
    print(f"Total skipped records: {len(all_skipped)}")

if __name__ == '__main__':
    main()
