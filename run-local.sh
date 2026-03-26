#!/usr/bin/env bash
# =============================================================================
# run-local.sh — End-to-end local runner for Mock Ecom MCP Server
# Finds an available port, updates config, builds and starts the server.
# =============================================================================
set -euo pipefail

# ---------- configurable defaults --------------------------------------------
START_PORT=${PORT:-8080}
MAX_PORT=9090
PROFILE=${SPRING_PROFILE:-dev}
SKIP_BUILD=${SKIP_BUILD:-false}
SKIP_TESTS=${SKIP_TESTS:-true}
# -----------------------------------------------------------------------------

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ---------- dependency checks -------------------------------------------------
for cmd in mvn; do
  command -v "$cmd" &>/dev/null || error "mvn is not installed or not on PATH."
done
command -v java &>/dev/null || error "java is not installed or not on PATH."

# ---------- JDK version check + auto-switch to JDK 21 ------------------------
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

if [[ "$JAVA_VER" -ne 21 ]] 2>/dev/null; then
  warn "Active Java version is $JAVA_VER (found: $(java -version 2>&1 | head -1))."
  warn "This project requires Java 21 due to Lombok compatibility. Searching for JDK 21…"

  # Try macOS java_home selector
  if command -v /usr/libexec/java_home &>/dev/null; then
    JDK21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    if [[ -n "$JDK21" ]]; then
      export JAVA_HOME="$JDK21"
      export PATH="$JAVA_HOME/bin:$PATH"
      info "Switched to JDK 21 via java_home: $JAVA_HOME"
    fi
  fi

  # Try SDKMAN
  if [[ -z "${JAVA_HOME:-}" ]] && [[ -d "${HOME}/.sdkman/candidates/java" ]]; then
    JDK21=$(find "${HOME}/.sdkman/candidates/java" -maxdepth 1 -type d -name "21*" 2>/dev/null | sort -V | tail -1)
    if [[ -n "$JDK21" ]]; then
      export JAVA_HOME="$JDK21"
      export PATH="$JAVA_HOME/bin:$PATH"
      info "Switched to JDK 21 via SDKMAN: $JAVA_HOME"
    fi
  fi

  # Try common Linux JVM paths
  if [[ -z "${JAVA_HOME:-}" ]]; then
    for candidate in \
        /usr/lib/jvm/java-21-openjdk-amd64 \
        /usr/lib/jvm/java-21-openjdk \
        /usr/lib/jvm/temurin-21 \
        /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home; do
      if [[ -x "$candidate/bin/java" ]]; then
        export JAVA_HOME="$candidate"
        export PATH="$JAVA_HOME/bin:$PATH"
        info "Switched to JDK 21: $JAVA_HOME"
        break
      fi
    done
  fi

  # Re-check after attempting to switch
  JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
  if [[ "$JAVA_VER" -ne 21 ]] 2>/dev/null; then
    warn "Could not automatically switch to JDK 21."
    warn "Install JDK 21 (e.g. 'sdk install java 21-tem' or 'brew install openjdk@21')"
    warn "then re-run with: JAVA_HOME=\$(/usr/libexec/java_home -v 21) ./run-local.sh"
    warn "Proceeding with Java $JAVA_VER — build may fail with Lombok."
  else
    success "Now using Java $JAVA_VER at $JAVA_HOME"
  fi
else
  success "Java $JAVA_VER detected — OK."
fi

# ---------- port finder -------------------------------------------------------
find_free_port() {
  local port=$1
  while [[ $port -le $MAX_PORT ]]; do
    if ! (echo >/dev/tcp/127.0.0.1/$port) 2>/dev/null; then
      echo "$port"
      return
    fi
    (( port++ ))
  done
  error "No free port found between $START_PORT and $MAX_PORT."
}

info "Scanning for a free port starting at $START_PORT …"
APP_PORT=$(find_free_port "$START_PORT")

if [[ "$APP_PORT" -ne "$START_PORT" ]]; then
  warn "Port $START_PORT is in use — using port $APP_PORT instead."
else
  success "Port $APP_PORT is available."
fi

# ---------- derived URLs ------------------------------------------------------
BASE_URL="http://localhost:${APP_PORT}"
MCP_SSE_URL="${BASE_URL}/sse"
H2_CONSOLE_URL="${BASE_URL}/h2-console"
HEALTH_URL="${BASE_URL}/actuator/health"

# ---------- build -------------------------------------------------------------
cd "$(dirname "$0")"

if [[ "$SKIP_BUILD" != "true" ]]; then
  info "Building project (profile: $PROFILE, skip tests: $SKIP_TESTS) …"
  MVN_ARGS="clean package"
  [[ "$SKIP_TESTS" == "true" ]] && MVN_ARGS="$MVN_ARGS -DskipTests"
  mvn $MVN_ARGS -q || error "Maven build failed."
  success "Build complete."
else
  warn "Skipping build (SKIP_BUILD=true)."
fi

# ---------- locate jar --------------------------------------------------------
JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)
[[ -n "$JAR" ]] || error "No JAR found in target/. Run without SKIP_BUILD=true first."
info "Using JAR: $JAR"

# ---------- print summary before launch ---------------------------------------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║         Mock Ecom MCP Server — Local Runtime             ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║${NC}  Profile     : ${GREEN}${PROFILE}${NC}"
echo -e "${CYAN}║${NC}  Port        : ${GREEN}${APP_PORT}${NC}"
echo -e "${CYAN}║${NC}  MCP SSE     : ${GREEN}${MCP_SSE_URL}${NC}"
echo -e "${CYAN}║${NC}  Health      : ${GREEN}${HEALTH_URL}${NC}"
[[ "$PROFILE" == "dev" ]] && \
echo -e "${CYAN}║${NC}  H2 Console  : ${GREEN}${H2_CONSOLE_URL}${NC}"
echo -e "${CYAN}║${NC}  DB          : ${GREEN}H2 in-memory (dev) / PostgreSQL (prod)${NC}"
echo -e "${CYAN}║${NC}  Auth secret : ${GREEN}mock-platform-secret-key${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${YELLOW}MCP client config snippet:${NC}"
echo '  {'
echo '    "mcpServers": {'
echo '      "mock-ecom": {'
echo "        \"url\": \"${MCP_SSE_URL}\","
echo '        "type": "sse"'
echo '      }'
echo '    }'
echo '  }'
echo ""
info "Starting server … (Ctrl-C to stop)"
echo ""

# ---------- launch ------------------------------------------------------------
exec java \
  -Dserver.port="${APP_PORT}" \
  -Dspring.profiles.active="${PROFILE}" \
  -jar "$JAR"
