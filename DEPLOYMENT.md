# hellofriend_BE 배포 가이드 (AWS EC2 + Docker + 가비아 도메인)

이 문서는 이번에 추가된 파일들(`Dockerfile`, `docker-compose.yml`, `nginx/`, `init-letsencrypt.sh`, `application-prod.yml`)을 사용해서 AWS EC2에 Docker로 배포하고, 가비아에서 구매한 도메인을 연결해 HTTPS까지 적용하는 전체 과정을 다룹니다.

## 추가/변경된 파일

- `Dockerfile` — Gradle로 빌드 → JRE 21 이미지로 실행하는 멀티스테이지 빌드
- `src/main/resources/application-prod.yml` — 배포용 설정(환경변수로 DB 접속 정보 주입)
- `docker-compose.yml` — app(Spring Boot) + mysql + nginx + certbot 4개 컨테이너 구성
- `nginx/templates/app.conf.template` — 리버스 프록시 + HTTPS 설정 (도메인은 환경변수로 치환)
- `init-letsencrypt.sh` — Let's Encrypt 인증서 최초 발급 스크립트
- `.env.example` — 배포에 필요한 환경변수 예시
- `.gitignore` — `.env`, `certbot/`(인증서, 비밀키) 제외 항목 추가

---

## 1단계. EC2 인스턴스 준비

1. AWS 콘솔 → EC2 → 인스턴스 시작
   - AMI: **Ubuntu Server 22.04 LTS**
   - 인스턴스 유형: **t3.small (2GB RAM) 이상 권장**
     - t2.micro/t3.micro(1GB RAM)는 프리티어지만, 컨테이너 안에서 Gradle 빌드를 돌리면 메모리 부족(OOM)으로 빌드가 죽는 경우가 많습니다. 프리티어를 꼭 써야 한다면 2~3단계 아래 "메모리가 부족할 때(스왑 추가)"를 참고하세요.
   - 키 페어: SSH 접속용 키 생성 및 다운로드
   - 보안 그룹 인바운드 규칙:
     - `SSH (22)` — 내 IP만 허용 권장
     - `HTTP (80)` — 0.0.0.0/0
     - `HTTPS (443)` — 0.0.0.0/0
2. **탄력적 IP(Elastic IP)**를 할당해서 인스턴스에 연결하세요. (재부팅해도 IP가 안 바뀌어야 도메인 연결이 안전합니다.)

### 메모리가 부족할 때 (스왑 추가)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 2단계. EC2에 Docker 설치

EC2에 SSH로 접속한 뒤:

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

# sudo 없이 docker 명령 쓰려면 (재로그인 필요)
sudo usermod -aG docker $USER
```

설치 확인: `docker --version`, `docker compose version`

---

## 3단계. 가비아 도메인 DNS 연결

가비아 → My가비아 → 서비스 관리 → DNS 관리(네임서버 설정)에서 A 레코드를 추가합니다.

| 타입 | 호스트 | 값(IP) | TTL |
|---|---|---|---|
| A | @ | EC2 탄력적 IP | 600 |
| A | www | EC2 탄력적 IP | 600 |

DNS 전파는 보통 몇 분~1시간 정도 걸립니다. 아래 명령으로 반영 여부를 확인하세요.

```bash
dig +short your-domain.com
```

> ⚠️ 5단계(인증서 발급)를 진행하기 *전에* 반드시 도메인이 EC2 IP로 정상 연결되어 있어야 합니다. Let's Encrypt가 HTTP로 실제 접속해서 확인하기 때문입니다.

---

## 4단계. 프로젝트 배포 & 환경변수 설정

```bash
git clone <레포_주소> hellofriend_BE
cd hellofriend_BE

cp .env.example .env
nano .env   # DOMAIN, CERTBOT_EMAIL, DB_* 값을 실제 값으로 채우기
```

`.env` 예시:

```
DOMAIN=your-domain.com
CERTBOT_EMAIL=you@example.com
DB_NAME=helf
DB_USERNAME=helf_user
DB_PASSWORD=강력한비밀번호
DB_ROOT_PASSWORD=강력한루트비밀번호
DDL_AUTO=update
```

---

## 5단계. 최초 배포 (앱 + DB → SSL 인증서 발급 → 전체 기동)

```bash
# 1) app, mysql만 먼저 빌드/기동 (nginx는 인증서가 없어서 아직 못 띄움)
docker compose up -d --build app mysql

# 2) 로그로 정상 기동 확인
docker compose logs -f app

# 3) Let's Encrypt 인증서 발급 (최초 1회만 실행)
chmod +x init-letsencrypt.sh
./init-letsencrypt.sh

# 4) 전체 서비스 기동 (nginx, certbot 자동갱신 포함)
docker compose up -d
```

정상적으로 끝나면 `https://your-domain.com` 으로 접속됩니다. 인증서는 certbot 컨테이너가 12시간마다 자동으로 갱신을 시도합니다(만료 30일 이내일 때만 실제 갱신).

---

## 6단계. 프론트엔드 CORS 확인

`src/main/java/dreamdays/Helf/config/CorsConfig.java`에 허용 도메인이 하드코딩되어 있습니다.

```java
config.setAllowedOriginPatterns(List.of(
        "http://localhost:3000",
        "https://eulji-hf.netlify.app"
));
```

프론트엔드가 배포된 실제 주소가 다르다면 이 목록에 추가하고 재배포하세요.

---

## 7단계. 이후 재배포 (코드 업데이트 시)

```bash
cd hellofriend_BE
git pull
docker compose up -d --build app
```

`mysql`, `nginx`, `certbot`은 그대로 두고 `app`만 다시 빌드/재시작하면 됩니다.

---

## 참고 사항

- **DB 스키마 관리**: 현재 Flyway/Liquibase 같은 마이그레이션 도구가 없어서 `DDL_AUTO=update`로 Hibernate가 자동으로 테이블을 생성/수정합니다. 초기 개발 단계에는 편하지만, 서비스가 커지면 데이터 유실 위험이 있는 컬럼 변경도 자동 반영될 수 있으니 나중에는 Flyway 도입을 권장합니다.
- **MySQL 포트는 외부에 노출하지 않습니다** — `docker-compose.yml`에서 mysql 컨테이너는 내부 네트워크(`helf-net`)에서만 접근 가능하고 호스트에 포트를 열지 않습니다. 외부 DB 툴로 접속하려면 SSH 터널링을 사용하세요.
- **로그 확인**: `docker compose logs -f app` / `docker compose logs -f nginx`
- **컨테이너 상태 확인**: `docker compose ps`
- **전체 재기동**: `docker compose down && docker compose up -d`
