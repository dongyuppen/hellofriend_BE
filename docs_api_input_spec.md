# API 입력 데이터 명세 (업데이트: 학번→전화번호, 학과→학교)

> 최신 스펙은 항상 Swagger에서 실시간 확인 가능합니다: `https://ssok.cloud/swagger-ui.html`

## 변경 사항 요약

| 기존 필드 | 변경 후 필드 | 타입 변경 | 비고 |
|---|---|---|---|
| `studentNumber` (학번, 10자리 숫자) | `phoneNumber` | `int` → `String` | 숫자만 11자리 (`^\d{11}$`), 하이픈 없이 전송 |
| `department` (학과, enum) | `school` | `Department enum` → `String` | 자유 텍스트 직접 입력 (enum 아님, 학교명 그대로 문자열 전송) |

## 1. `POST /api/users/create-info` — 정보 등록

```json
{
  "name": "홍길동",
  "phoneNumber": "01012345678",
  "instagramId": "hong_gd",
  "age": 23,
  "gender": "MALE",
  "school": "한양대학교",
  "mbti": "INFP",
  "selectGender": "FEMALE",
  "bio": "자기소개"
}
```

- `phoneNumber`: 숫자만 11자리 문자열. 형식이 틀리면 `400 INVALID_REQUEST`로 응답.
- `school`: 자유 입력 텍스트. 빈 값 불가.
- `gender`, `selectGender`, `mbti`: enum, 기존과 동일.

## 2. `GET /api/users/check-info` — 정보 조회

Query String: `?name=홍길동&phoneNumber=01012345678`

## 3. `POST /api/draw` — 뽑기

```json
{
  "name": "홍길동",
  "phoneNumber": "01012345678"
}
```

응답(`DrawResponse`)에서도 `department` → `school`로 변경되었습니다.

## 4. 에러 응답

기존과 동일하게 `status` + `errorCode` + `message` 형식입니다. 자세한 표는 이전에 전달드린 프론트 공지 참고. 이번 변경으로 `INVALID_REQUEST`가 전화번호 형식/학교명 누락 등 입력값 검증 실패 시에도 내려갑니다.
