#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
从月票订单SQL文件中提取building、units、floor、roomnumber等字段
生成用于插入到新表的SQL语句
"""

import re
import os

def extract_insert_values(line):
    """从INSERT语句中提取VALUES部分的值"""
    # 匹配VALUES后面的括号内容
    match = re.search(r"VALUES\s*\((.*?)\);", line, re.IGNORECASE)
    if not match:
        return None
    
    values_str = match.group(1)
    
    # 解析字段值（处理逗号分隔，但要注意引号内的逗号）
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
    
    # 添加最后一个值
    if current_value:
        values.append(current_value.strip())
    
    return values

def clean_value(value):
    """清理字段值（去除引号）"""
    value = value.strip()
    if value.startswith("'") and value.endswith("'"):
        return value[1:-1]
    return value

def process_sql_file(file_path):
    """处理SQL文件，提取需要的字段"""
    records = []
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        # 合并多行INSERT语句
        lines = content.split('\n')
        
        current_statement = ""
        for line in lines:
            line = line.strip()
            if not line or line.startswith('--') or line.startswith('USE'):
                continue
            
            current_statement += " " + line
            
            # 如果语句结束（以;结尾）
            if line.endswith(';'):
                if 'INSERT INTO' in current_statement:
                    values = extract_insert_values(current_statement)
                    if values and len(values) >= 8:
                        # 提取前8个字段：province, city, district, community, building, units, floor, roomnumber
                        record = {
                            'province': clean_value(values[0]),
                            'city': clean_value(values[1]),
                            'district': clean_value(values[2]),
                            'community': clean_value(values[3]),
                            'building': clean_value(values[4]),
                            'units': clean_value(values[5]),
                            'floor': clean_value(values[6]),
                            'roomnumber': clean_value(values[7])
                        }
                        records.append(record)
                
                current_statement = ""
    
    return records

def remove_duplicates(records):
    """去除重复记录"""
    seen = set()
    unique_records = []
    
    for record in records:
        # 创建一个唯一标识
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
    sql_lines.append("-- 房间信息数据插入")
    sql_lines.append("-- 生成时间: 自动生成")
    sql_lines.append(f"-- 总记录数: {len(records)}")
    sql_lines.append("")
    sql_lines.append("USE parking_management;")
    sql_lines.append("")
    
    for record in records:
        sql = (
            f"INSERT INTO rooms (province, city, district, community, building, units, floor, roomnumber, is_audit, audit_start_time, audit_end_time)\n"
            f"VALUES ('{record['province']}', '{record['city']}', '{record['district']}', '{record['community']}', "
            f"'{record['building']}', '{record['units']}', '{record['floor']}', '{record['roomnumber']}', NULL, NULL, NULL);"
        )
        sql_lines.append(sql)
    
    return '\n'.join(sql_lines)

def main():
    """Main function"""
    # File paths
    file1 = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_final.sql'
    file2 = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_manual_fixed.sql'
    output_file = r'd:\PakingDemo\parking-demo\room_data_import.sql'
    
    print("Processing first SQL file...")
    records1 = process_sql_file(file1)
    print(f"  Extracted {len(records1)} records from first file")
    
    print("Processing second SQL file...")
    records2 = process_sql_file(file2)
    print(f"  Extracted {len(records2)} records from second file")
    
    # Merge records
    all_records = records1 + records2
    print(f"Total records after merge: {len(all_records)}")
    
    # Remove duplicates
    print("Removing duplicates...")
    unique_records = remove_duplicates(all_records)
    print(f"Unique records: {len(unique_records)}")
    
    # Generate SQL
    print("Generating INSERT statements...")
    sql_content = generate_insert_sql(unique_records)
    
    # Save to file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(sql_content)
    
    print(f"\nDone! SQL file saved to: {output_file}")
    print(f"Total INSERT statements generated: {len(unique_records)}")
    
    # Show first 5 examples
    print("\nFirst 5 records:")
    for i, record in enumerate(unique_records[:5], 1):
        print(f"{i}. Building: {record['building']}, Unit: {record['units']}, Floor: {record['floor']}, Room: {record['roomnumber']}")

if __name__ == '__main__':
    main()
