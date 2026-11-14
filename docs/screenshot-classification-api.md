# 스크린샷 분류 결과 API

로그인한 사용자의 Bearer access token이 필요합니다. 분류 결과는 사용자별로 관리됩니다.
사용자 응답의 `id`는 `usr_` 뒤에 DB의 숫자 ID를 붙인 형식입니다 (예: `usr_31`). 인증과 DB 관계에는 숫자 ID를 사용합니다.

## 결과 저장

`POST /screenshot-classifications`

```json
{
  "items": [
    { "fileKey": "device-specific-photo-id-1", "classification": "PRACTICE" },
    { "fileKey": "device-specific-photo-id-2", "classification": "REFERENCE" }
  ]
}
```

- `fileKey`는 호출 측이 제공하는 사진 식별자이며, 사용자별로 중복될 수 없습니다. 기기 식별자는 다른 기기에서 달라질 수 있으므로 여러 기기 지원을 위한 식별 방식은 팀과 협의해야 합니다.
- `classification`은 `PRACTICE`, `REFERENCE`, `DELETE` 중 하나입니다.
- 한 요청은 1~20장입니다. 요청 안에 중복된 식별자가 있거나 이미 저장한 식별자가 있으면 전체 요청이 실패합니다.
- 응답의 `data`에는 각 기록의 `id`, `fileKey`, `classification`, `classifiedAt`이 들어갑니다.
- `DELETE`는 삭제 결과 기록만 저장합니다. 프론트엔드는 실제 기기 삭제가 성공한 후에만 이 값을 전송해야 합니다.

## 결과 조회와 취소

- `GET /screenshot-classifications`: 로그인한 사용자의 기록을 최신순으로 조회합니다.
- `PATCH /screenshot-classifications/{id}`: `{"classification":"PRACTICE"}` 또는 `{"classification":"REFERENCE"}`로 분류를 변경합니다. 삭제 기록은 변경할 수 없습니다.
- `DELETE /screenshot-classifications/{id}`: 로그인한 사용자의 해당 분류 기록을 취소합니다. 기기에서 이미 삭제된 사진은 복구되지 않습니다.

현재 API는 분류 기록만 저장합니다. 사진 원본 업로드, 원본 조회, AI 분석 요청, 프론트엔드 연동은 아직 구현되지 않았습니다. 사진 저장 방식과 API 형식은 팀에서 협의한 명세에 맞춰 조정해야 합니다.
