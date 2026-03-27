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

# ---------- JDK version check (Java 21+ required) ----------------------------
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [[ "$JAVA_VER" -ge 21 ]] 2>/dev/null; then
  success "Java $JAVA_VER detected — OK."
else
  error "Java 21+ is required (found Java $JAVA_VER). Install: https://adoptium.net"
fi

# ---------- port finder -------------------------------------------------------
find_free_port() {
  local port=$1
  while [[ $port -le $MAX_PORT ]]; do
    # nc -z works on both macOS and Linux; falls back to /dev/tcp if nc absent
    if command -v nc &>/dev/null; then
      nc -z 127.0.0.1 "$port" 2>/dev/null || { echo "$port"; return; }
    else
      (echo >/dev/tcp/127.0.0.1/$port) 2>/dev/null || { echo "$port"; return; }
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
SWAGGER_UI_URL="${BASE_URL}/swagger-ui.html"
OPENAPI_JSON_URL="${BASE_URL}/v3/api-docs"

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
echo -e "${CYAN}║${NC}  Swagger UI  : ${GREEN}${SWAGGER_UI_URL}${NC} ${YELLOW}(add -Pswagger to build)${NC}"
echo -e "${CYAN}║${NC}  OpenAPI JSON: ${GREEN}${OPENAPI_JSON_URL}${NC} ${YELLOW}(add -Pswagger to build)${NC}"
echo -e "${CYAN}║${NC}  Postman     : ${GREEN}docs/postman_collection.json${NC}"
echo -e "${CYAN}║${NC}  OpenAPI YAML: ${GREEN}docs/openapi.yaml${NC}"
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
