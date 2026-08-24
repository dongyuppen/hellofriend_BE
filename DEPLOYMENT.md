# hellofriend_BE 배포 가이드 (AWS EC2 + Docker + 가비아 도메인)

`ssok.cloud` 도메인으로 실제 배포까지 완료한 내용을 정리한 문서입니다. EC2 인스턴스를 새로 만들거나, 서버가 죽어서 재배포해야 하거나, 이 구조를 다른 프로젝트에 재사용할 때 이 문서 하나만 보고 따라 하면 됩니다.

## 현재 배포 상태 요약

| 항목 | 값 |
|---|---|
| 도메인 | `ssok.cloud` (가비아 구매) |
| 서버 | AWS EC2 (Ubuntu 22.04) |
| 배포 방식 | Docker Compose 4개 컨테이너 (`app`, `mysql`, `nginx`, `certbot`) |
| DB | MySQL 8.0 컨테이너 (RDS 아님, 외부 미노출) |
| HTTPS | Let's Encrypt (certbot 자동 갱신) |
| API 문서 | `https://ssok.cloud/swagger-ui.html` |

## 구성 파일

- `Dockerfile` — Gradle 빌드 → JRE 21 실행, 멀티스테이지 빌드
- `docker-compose.yml` — app + mysql + nginx + certbot 구성
- `src/main/resources/application-prod.yml` — 배포용 설정. `SPRING_PROFILES_ACTIVE=prod`로 활성화되며 DB 접속 정보 등은 전부 환경변수로 주입받음
- `nginx/templates/app.conf.template` — 리버스 프록시 + HTTPS 설정 (`${DOMAIN}`은 nginx 공식 이미지의 envsubst 기능으로 자동 치환)
- `init-letsencrypt.sh` — Let's Encrypt 인증서 최초 발급 스크립트
- `.env.example` — 배포에 필요한 환경변수 예시 (`.env`로 복사해서 사용, git에는 커밋 안 함)
- `.gitignore` — `.env`, `certbot/`(인증서·비밀키) 제외 처리됨

---

## 1단계. EC2 인스턴스 준비

1. AWS 콘솔 → EC2 → 인스턴스 시작
   - AMI: **Ubuntu Server 22.04 LTS**
   - 인스턴스 유형: **t3.small(2GB RAM) 이상 권장**
     - 프리티어인 t2.micro/t3.micro(1GB RAM)에서는 컨테이너 안에서 Gradle 빌드 시 메모리 부족(OOM)으로 빌드가 죽을 수 있습니다. 꼭 써야 한다면 아래 스왑 설정을 먼저 하세요.
   - 키 페어: SSH 접속용 `.pem` 키 생성 및 다운로드
   - 보안 그룹 인바운드: `SSH(22)`는 내 IP만, `HTTP(80)`/`HTTPS(443)`는 `0.0.0.0/0`
2. **탄력적 IP(Elastic IP)**를 할당해서 인스턴스에 연결 (재부팅해도 IP가 바뀌지 않아야 도메인 연결이 안전함)

### 메모리가 부족할 때 (스왑 2GB 추가)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

`free -h`로 `Swap: 2.0Gi`가 보이면 정상 적용된 것.

### SSH 접속

```bash
chmod 400 키페어파일.pem
ssh -i 키페어파일.pem ubuntu@EC2_퍼블릭_IP또는탄력적IP
```

---

## 2단계. EC2에 Docker 설치

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo usermod -aG docker $USER
```

`usermod` 이후에는 `exit` 후 재접속해야 `sudo` 없이 `docker` 명령이 됩니다.

설치 확인: `docker --version`, `docker compose version`

---

## 3단계. 가비아 도메인 DNS 연결

가비아 → My가비아 → 서비스 관리 → DNS 관리에서 A 레코드 추가.

| 타입 | 호스트 | 값 | TTL |
|---|---|---|---|
| A | @ | EC2 탄력적 IP | 600 |
| A | www | EC2 탄력적 IP | 600 |

반영 확인 (로컬 터미널에서):

```bash
dig +short ssok.cloud
```

> ⚠️ 5단계(인증서 발급) 전에 반드시 도메인이 EC2 IP로 연결되어 있어야 합니다. Let's Encrypt가 HTTP로 실제 접속해서 소유권을 확인하기 때문입니다.

---

## 4단계. 프로젝트 클론 + 환경변수 설정

```bash
git clone <레포_주소> hellofriend_BE
cd hellofriend_BE

docker ps   # sudo 없이 되는지 확인 (안되면 재로그인)

cp .env.example .env
nano .env
```

`.env` 채우는 예시:

```
DOMAIN=ssok.cloud
CERTBOT_EMAIL=본인_이메일@example.com
DB_NAME=helf
DB_USERNAME=helf_user
DB_PASSWORD=강력한비밀번호로변경
DB_ROOT_PASSWORD=강력한루트비밀번호로변경
DDL_AUTO=update
SWAGGER_ENABLED=true
```

---

## 5단계. 최초 배포 (앱+DB → SSL 발급 → 전체 기동)

```bash
# 1) app, mysql만 먼저 빌드/기동 (nginx는 인증서가 없어서 아직 못 띄움)
docker compose up -d --build app mysql
docker compose logs -f app     # 정상 기동 확인 후 Ctrl+C

# 2) Let's Encrypt 인증서 발급 (최초 1회만)
chmod +x init-letsencrypt.sh
./init-letsencrypt.sh

# 3) 전체 서비스 기동 (nginx, 인증서 자동갱신용 certbot 포함)
docker compose up -d
docker compose ps   # 4개 컨테이너 모두 Up인지 확인
```

정상이면 `https://ssok.cloud`로 접속됩니다. 인증서는 certbot 컨테이너가 12시간마다 자동으로 갱신을 시도합니다 (만료 30일 이내일 때만 실제 갱신).

---

## 6단계. 배포 확인

루트(`/`)는 매핑된 컨트롤러가 없어서 화이트라벨 에러 페이지가 뜨는 게 정상입니다. 실제 동작 확인은 API로:

```bash
curl -i https://ssok.cloud/api/users/all
```

`HTTP/2 200`과 함께 JSON(`[]` 또는 목록)이 오면 HTTPS → nginx → Spring Boot 앱 → MySQL까지 전체 스택이 정상입니다.

Swagger API 문서: `https://ssok.cloud/swagger-ui.html`

> 현재 Spring Security가 없어서 Swagger UI는 인증 없이 누구나 접근 가능합니다. 민감한 API가 늘어나면 `.env`에서 `SWAGGER_ENABLED=false`로 끄거나 nginx 단에서 접근 제한(IP 화이트리스트, basic auth)을 추가하세요.

---

## 7단계. 이후 재배포 (코드 업데이트 시)

```bash
cd ~/hellofriend_BE
git pull
docker compose up -d --build app
```

`mysql`, `nginx`, `certbot`은 그대로 두고 `app`만 다시 빌드/재시작하면 됩니다.

> `git pull`이 "Your local changes would be overwritten" 에러로 막히면, 서버에서 직접 파일을 수정한 이력이 남아있는 겁니다. 내용이 원격과 같다면 `git checkout -- <파일>`로 로컬 변경을 버리고 `git pull`, 다르다면 `git stash` 후 `git pull` → `git stash pop`으로 병합하세요.

---

## 8단계. MySQL 데이터 직접 조회

mysql 컨테이너는 보안상 호스트에 포트를 열어두지 않았습니다. 컨테이너 내부로 들어가서 조회하세요.

```bash
docker compose exec mysql mysql -u root -p
# 비밀번호: .env의 DB_ROOT_PASSWORD

# 또는 앱 계정으로
docker compose exec mysql mysql -u<DB_USERNAME> -p<DB_PASSWORD> <DB_NAME>
```

```sql
SHOW DATABASES;
USE helf;
SHOW TABLES;
SELECT * FROM user;
```

---

## 트러블슈팅 (실제로 겪었던 문제와 해결)

### 1) Let's Encrypt 인증서 발급 시 `Connection refused`

**원인**: `init-letsencrypt.sh`가 `options-ssl-nginx.conf`/`ssl-dhparams.pem`를 GitHub raw에서 `curl`로 받아오는데, 이게 실패해서 빈 파일이 생성되고 nginx가 설정 파싱 에러로 계속 재시작(크래시 루프)에 빠져 80번 포트에 아무것도 안 떠 있었음.

**해결**: 외부 파일 다운로드 없이 TLS 설정(`ssl_protocols`, `ssl_ciphers` 등)을 `nginx/templates/app.conf.template`에 직접 인라인으로 작성하도록 변경. 현재 저장소 버전은 이미 이 방식이 적용되어 있어 재발하지 않습니다.

### 2) Swagger 적용 후 앱이 기동 자체를 못 함

```
Invalid mapping pattern detected: /swagger-ui/**/*index.html
No more pattern data allowed after {*...} or ** pattern element
```

**원인**: `springdoc-openapi-starter-webmvc-ui` 2.8.15 이상 버전이 Spring Boot 3.4.x의 엄격해진 경로 패턴 검증과 충돌하는 회귀 버그 ([springdoc-openapi#3210](https://github.com/springdoc/springdoc-openapi/issues/3210)). `spring.mvc.pathmatch.matching-strategy=ant_path_matcher` 설정으로도 해결되지 않음.

**해결**: `build.gradle`에서 springdoc 버전을 **2.8.14로 고정**. 나중에 springdoc을 업그레이드할 일이 있으면 이 이슈가 해당 버전에서 fix됐는지 먼저 GitHub에서 확인하고, 로컬에서 기동 테스트 후 배포하세요.

### 3) Docker 이미지가 재빌드되는데 코드 변경이 반영 안 됨

`git pull`이 로컬 변경 충돌로 실패했는데 이를 놓치고 `docker compose up -d --build`를 실행하면, 소스가 그대로라 Docker 빌드 캐시가 전부 `CACHED`로 처리되어 예전 코드로 빌드됩니다. 빌드 로그에서 `COPY src src`, `RUN ./gradlew clean bootJar` 단계가 `CACHED`로 나오면 실제로는 아무것도 안 바뀐 것이니, `git pull`이 진짜로 성공했는지(`git log -1`로 최신 커밋 확인) 먼저 점검하세요.

---

## 참고 사항

- **DB 스키마 관리**: Flyway/Liquibase 같은 마이그레이션 도구가 없어서 `DDL_AUTO=update`로 Hibernate가 자동으로 테이블을 생성/수정합니다. 초기 개발 단계엔 편하지만, 서비스가 커지면 의도치 않은 컬럼 변경/삭제가 자동 반영될 위험이 있으니 이후 Flyway 도입을 권장합니다.
- **MySQL 포트 미노출**: `docker-compose.yml`에서 mysql은 내부 네트워크(`helf-net`)에서만 접근 가능. 외부 GUI 툴(TablePlus 등)로 접속하려면 SSH 터널링 설정이 별도로 필요합니다.
- **CORS**: `src/main/java/dreamdays/Helf/config/CorsConfig.java`에 허용 도메인이 하드코딩되어 있습니다 (`localhost:3000`, `eulji-hf.netlify.app`). 프론트엔드 배포 주소가 다르면 여기에 추가 후 재배포하세요.
- **로그 확인**: `docker compose logs -f app` / `docker compose logs -f nginx`
- **컨테이너 상태 확인**: `docker compose ps`
- **전체 재기동**: `docker compose down && docker compose up -d`
