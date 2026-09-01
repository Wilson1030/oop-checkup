#!/usr/bin/env bash
# ============================================================================
#  Regression test.
#
#  Runs the tool against the committed fixtures and asserts exact output.
#  This is what CI executes; run it locally before pushing.
#
#  Only fixtures tracked in git are used (examples/, testdata/), so this works
#  on a fresh clone. The 13 validation samples are third-party repositories and
#  are not committed — see runs/ for their recorded results.
# ============================================================================
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

JAR="target/oop-checkup.jar"
PASS=0
FAIL=0

green() { printf '\033[32m%s\033[0m\n' "$1"; }
red()   { printf '\033[31m%s\033[0m\n' "$1"; }

ok()   { PASS=$((PASS+1)); green "  PASS  $1"; }
bad()  { FAIL=$((FAIL+1)); red   "  FAIL  $1"; [ $# -gt 1 ] && printf '        %s\n' "$2"; }

run() { java -Dfile.encoding=UTF-8 -jar "$JAR" "$@" 2>&1; }

# --- assert that a summary line contains an expected pattern -----------------
expect_summary() {
  local desc="$1" path="$2" want="$3"
  local got
  got="$(run "$path" --summary)"
  if [[ "$got" == *"$want"* ]]; then
    ok "$desc"
  else
    bad "$desc" "expected to contain: $want"
    printf '        actual:              %s\n' "$got"
  fi
}

echo "=============================================="
echo " oop-checkup regression"
echo "=============================================="

if [ ! -f "$JAR" ]; then
  red "jar not found: $JAR  (run: mvn package)"
  exit 1
fi

# ---------------------------------------------------------------- detections
echo
echo "-- detections --"

expect_summary "examples/before findings" \
  "examples/before" \
  "|1:2(2) |2:1? |3:1(1) |4:2(1) |5:1(0) |6:1(0)"

expect_summary "examples/after  findings (0 violations)" \
  "examples/after" \
  "|1:0(0) |2:1? |3:0(0) |4:0(0) |5:0(0) |6:0(0)"

expect_summary "testdata/MainBloat triggers item 6" \
  "testdata/MainBloat" \
  "|6:1(0)"

# ---------------------------------------------------------------- determinism
echo
echo "-- determinism --"

for lang in zh en; do
  a="$(run examples/before --lang $lang --detail 20)"
  b="$(run examples/before --lang $lang --detail 20)"
  if [ "$a" = "$b" ]; then
    ok "identical output across runs ($lang)"
  else
    bad "identical output across runs ($lang)"
  fi
done

# --------------------------------------------------- language does not affect
echo
echo "-- language independence --"

zh="$(run examples/before --summary)"
en="$(run examples/before --lang en --summary)"
if [ "$zh" = "$en" ]; then
  ok "zh and en produce identical findings"
else
  bad "zh and en produce identical findings" "$zh vs $en"
fi

# -------------------------------------------------------- llm does not affect
echo
echo "-- llm independence --"

cfg="$(mktemp)"
cat > "$cfg" <<'JSON'
{"enabled":true,"baseUrl":"http://127.0.0.1:9/v1","apiKey":"sk-x","model":"m","timeoutMs":1500}
JSON

plain="$(run examples/before --summary)"
withllm="$(run examples/before --summary --config "$cfg")"
rm -f "$cfg"

if [ "$plain" = "$withllm" ]; then
  ok "findings unchanged with an LLM configured"
else
  bad "findings unchanged with an LLM configured" "$plain vs $withllm"
fi

# ------------------------------------------------- unreachable llm degrades
out="$(run examples/before --detail 1 --config <(echo '{"enabled":true,"baseUrl":"http://127.0.0.1:9/v1","apiKey":"sk-x","model":"m","timeoutMs":1500}') 2>&1)" || true
if [[ "$out" == *"检查项"* || "$out" == *"Item"* ]]; then
  ok "report still produced when the endpoint is unreachable"
else
  bad "report still produced when the endpoint is unreachable"
fi

# ------------------------------------------------------------ fixtures build
echo
echo "-- fixtures compile --"

tmp="$(mktemp -d)"
for v in before after; do
  if javac -encoding UTF-8 -d "$tmp/$v" examples/$v/library/*.java 2>"$tmp/$v.err"; then
    ok "examples/$v compiles"
  else
    bad "examples/$v compiles" "$(head -3 "$tmp/$v.err")"
  fi
done
if javac -encoding UTF-8 -d "$tmp/mb" testdata/MainBloat/*.java 2>"$tmp/mb.err"; then
  ok "testdata/MainBloat compiles"
else
  bad "testdata/MainBloat compiles" "$(head -3 "$tmp/mb.err")"
fi
rm -rf "$tmp"

# ------------------------------------------------------------------- summary
echo
echo "=============================================="
printf ' %d passed, %d failed\n' "$PASS" "$FAIL"
echo "=============================================="
[ "$FAIL" -eq 0 ] || exit 1
