import re
import sys
import io

# 设置输出编码
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# 读取SQL文件
with open('月票订单_20251121214212571001_cleaned.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# 查找所有包含 -数字/数字 格式的房间号
pattern = r"VALUES\s*\([^)]*'([^']*-\d+/\d+[^']*)'[^)]*\);"
matches = re.findall(pattern, content)

print(f"Total found: {len(matches)} records with special format\n")
print("Unique room numbers:")
unique_roomnumbers = sorted(set(matches))
for room in unique_roomnumbers:
    count = matches.count(room)
    print(f"{room} - appears {count} times")
