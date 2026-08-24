#!/bin/bash
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
