#!/bin/bash
# 최초 1회 실행: Let's Encrypt 인증서를 발급받기 위한 부트스트랩 스크립트
# 실행 전 조건:
#   1. .env 파일에 DOMAIN, CERTBOT_EMAIL 등이 채워져 있어야 함
#   2. 가비아에서 해당 도메인의 A 레코드가 이 서버의 공인(고정) IP를 가리키고 있어야 함
#   3. 보안그룹에서 80, 443 포트가 열려있어야 함
#
# 사용법: chmod +x init-letsencrypt.sh && ./init-letsencrypt.sh

set -e

if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

if [ -z "$DOMAIN" ]; then
  echo "오류: .env 파일에 DOMAIN 이 설정되어 있지 않습니다."
  exit 1
fi

domain="$DOMAIN"
rsa_key_size=4096
data_path="./certbot/conf"
email="$CERTBOT_EMAIL"
staging=0 # Let's Encrypt rate limit 테스트가 필요하면 1로 변경

if [ -d "$data_path/live/$domain" ]; then
  read -p "이미 $domain 에 대한 인증서 데이터가 있습니다. 계속해서 교체할까요? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi

if [ ! -e "$data_path/options-ssl-nginx.conf" ] || [ ! -e "$data_path/ssl-dhparams.pem" ]; then
  echo "### 권장 TLS 파라미터 다운로드 중..."
  mkdir -p "$data_path"
  curl -s https://raw.githubusercontent.com/certbot/certbot/main/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf > "$data_path/options-ssl-nginx.conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/main/certbot/certbot/ssl-dhparams.pem > "$data_path/ssl-dhparams.pem"
fi

echo "### $domain 용 더미 인증서 생성 중 (nginx 부팅용)..."
path="/etc/letsencrypt/live/$domain"
mkdir -p "$data_path/live/$domain"
docker compose run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:$rsa_key_size -days 1 \
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo "### nginx 시작 중..."
docker compose up -d --force-recreate nginx

echo "### 더미 인증서 삭제 중..."
docker compose run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/$domain && \
  rm -Rf /etc/letsencrypt/archive/$domain && \
  rm -Rf /etc/letsencrypt/renewal/$domain.conf" certbot

echo "### $domain 용 Let's Encrypt 인증서 요청 중..."
case "$email" in
  "") email_arg="--register-unsafely-without-email" ;;
  *) email_arg="--email $email" ;;
esac

if [ "$staging" != "0" ]; then staging_arg="--staging"; fi

docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $email_arg \
    -d $domain \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --force-renewal" certbot

echo "### nginx 재시작(인증서 반영) 중..."
docker compose exec nginx nginx -s reload

echo "완료! https://$domain 으로 접속해 보세요."
