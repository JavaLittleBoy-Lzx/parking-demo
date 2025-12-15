#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删除SQL文件中包含NULL字段的INSERT语句
"""

input_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_fixed.sql'
output_file = r'd:\PakingDemo\parking-demo\月票订单_20251121214212571001_cleaned.sql'

with open(input_file, 'r', encoding='utf-8') as f:
    lines = f.readlines()

cleaned_lines = []
skip_next = False

skipped_count = 0
for i, line in enumerate(lines):
    # 如果这一行包含NULL，跳过这一行
    if 'INSERT INTO' in line and 'NULL' in line:
        skipped_count += 1
        continue
    # 如果上一行是INSERT INTO，这一行是VALUES且包含NULL
    elif 'VALUES' in line and 'NULL' in line:
        # 检查上一个非空行是否是INSERT INTO
        for j in range(len(cleaned_lines)-1, -1, -1):
            if cleaned_lines[j].strip():
                if 'INSERT INTO' in cleaned_lines[j]:
                    # 删除之前添加的INSERT INTO行
                    cleaned_lines.pop()
                    skipped_count += 1
                break
        continue
    else:
        cleaned_lines.append(line)

# 写入清理后的文件
with open(output_file, 'w', encoding='utf-8') as f:
    f.writelines(cleaned_lines)

print(f"\nProcessing completed!")
print(f"Original lines: {len(lines)}")
print(f"Cleaned lines: {len(cleaned_lines)}")
print(f"Deleted lines: {len(lines) - len(cleaned_lines)}")
print(f"Skipped INSERT statements with NULL: {skipped_count}")
print(f"Output file created successfully!")
