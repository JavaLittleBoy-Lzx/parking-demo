#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
更新人工检查数据的SQL文件结构
1. 添加 credit_score 字段，默认值为 100
2. 将 isaudit 和 permitverify 从 '02' 改为 '是'
"""

import re
import sys
import io

# 设置标准输出编码为UTF-8
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def update_sql_structure(input_file, output_file):
    """更新SQL文件结构"""
    
    print(f"开始处理文件: {input_file}")
    
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    updated_lines = []
    insert_count = 0
    
    for line in lines:
        # 检查是否是INSERT语句
        if line.strip().startswith('INSERT INTO monthly_ticket_owners'):
            # 更新INSERT语句的列名部分，添加 credit_score
            updated_line = line.replace(
                'isaudit, permitverify)',
                'isaudit, permitverify, credit_score)'
            )
            updated_lines.append(updated_line)
        elif line.strip().startswith('VALUES'):
            # 更新VALUES部分
            # 提取VALUES内容
            values_match = re.search(r"VALUES\s*\((.*?)\);", line, re.DOTALL)
            if values_match:
                values_content = values_match.group(1)
                
                # 分割字段值
                values = []
                current = []
                in_quotes = False
                for char in values_content:
                    if char == "'" and (not current or current[-1] != '\\'):
                        in_quotes = not in_quotes
                    current.append(char)
                    if char == ',' and not in_quotes:
                        values.append(''.join(current[:-1]).strip())
                        current = []
                if current:
                    values.append(''.join(current).strip())
                
                # 修改 isaudit (索引12) 和 permitverify (索引13)
                if len(values) >= 14:
                    # 将 '02' 改为 '是'
                    if values[12] == "'02'":
                        values[12] = "'是'"
                    if values[13] == "'02'":
                        values[13] = "'是'"
                    
                    # 添加 credit_score = 100
                    values.append('100')
                
                # 重新组装VALUES语句
                new_values = ', '.join(values)
                updated_line = f"VALUES ({new_values});\n"
                updated_lines.append(updated_line)
                insert_count += 1
            else:
                updated_lines.append(line)
        else:
            updated_lines.append(line)
    
    # 写入输出文件
    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(updated_lines)
    
    print(f"\n处理完成!")
    print(f"更新的INSERT语句数: {insert_count}")
    print(f"输出文件: {output_file}")

if __name__ == '__main__':
    input_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_manual_fixed.sql'
    output_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_manual_final.sql'
    
    update_sql_structure(input_file, output_file)
