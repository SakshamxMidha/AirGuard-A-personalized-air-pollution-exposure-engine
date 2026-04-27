#!/usr/bin/env bash
set -e

# ── AirGuard Dev Startup ───────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

echo -e "${CYAN}"
echo "  █████╗ ██╗██████╗  ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ "
echo " ██╔══██╗██║██╔══██╗██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗"
echo " ███████║██║██████╔╝██║  ███╗██║   ██║███████║██████╔╝██║  ██║"
echo " ██╔══██║██║██╔══██╗██║   ██║██║   ██║██╔══██║██╔══██╗██║  ██║"
echo " ██║  ██║██║██║  ██║╚██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝"
echo " ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ "
echo -e "${NC}"
echo -e " ${GREEN}Personalized Air Pollution Exposure Engine${NC}"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
  echo -e "${YELLOW}⚠  Java 21+ not found. Install from: https://adoptium.net${NC}"
  exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
  echo -e "${YELLOW}⚠  Java 21+ required. Found: Java $JAVA_VERSION${NC}"
  exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
  echo -e "${YELLOW}⚠  Maven not found. Install from: https://maven.apache.org${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Java $(java -version 2>&1 | awk -F '"' '/version/ {print $2}') detected${NC}"

# Build backend
echo ""
echo -e "${CYAN}▶ Building backend...${NC}"
cd backend
mvn -B package -DskipTests -q
echo -e "${GREEN}✓ Backend built successfully${NC}"

# Start backend in background
echo ""
echo -e "${CYAN}▶ Starting backend on port 8080...${NC}"
java -jar target/airguard-backend-*.jar &
BACKEND_PID=$!
echo -e "${GREEN}✓ Backend started (PID: $BACKEND_PID)${NC}"

# Wait for backend
echo -e "${CYAN}▶ Waiting for backend to be ready...${NC}"
for i in {1..30}; do
  if curl -s http://localhost:8080/api/actuator/health &>/dev/null; then
    break
  fi
  sleep 2
done

cd ..

echo ""
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}  AirGuard is running!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo ""
echo -e "  Backend API : ${CYAN}http://localhost:8080/api${NC}"
echo -e "  Swagger UI  : ${CYAN}http://localhost:8080/api/swagger-ui.html${NC}"
echo -e "  H2 Console  : ${CYAN}http://localhost:8080/api/h2-console${NC}"
echo ""
echo -e "  Frontend    : Open ${CYAN}frontend/index.html${NC} in your browser"
echo -e "                (or use VS Code Live Server)"
echo ""
echo -e "  Press ${YELLOW}Ctrl+C${NC} to stop"

trap "echo ''; echo 'Shutting down...'; kill $BACKEND_PID 2>/dev/null; exit 0" INT
wait $BACKEND_PID
