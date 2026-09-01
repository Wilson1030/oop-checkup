#!/usr/bin/env bash
# ============================================================
#  oop-checkup 启动脚本（macOS / Linux / Git Bash）
#
#  用法:  ./checkup.sh <项目路径> [选项]
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/target/oop-checkup.jar"

if [ ! -f "$JAR" ]; then
    echo ""
    echo "[错误] 找不到 $JAR"
    echo ""
    echo "请先在项目目录执行构建:"
    echo "    mvn package"
    echo ""
    exit 1
fi

if [ $# -eq 0 ]; then
    cat <<'EOF'

oop-checkup - 面向对象转换检查表

用法:
    ./checkup.sh <项目路径> [选项]

选项:
    --detail N         每个检查项最多展开 N 处（默认 3）
    --include-tests    包含测试目录（默认排除）
    --summary          只输出一行摘要
    --batch            把路径下每个子目录各当一个项目

示例:
    ./checkup.sh examples/before
    ./checkup.sh ~/我的作业 --detail 20
    ./checkup.sh ~/我的作业 --detail 20 > 报告.txt

EOF
    exit 0
fi

exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
