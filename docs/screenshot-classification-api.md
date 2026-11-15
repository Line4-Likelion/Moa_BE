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

## 실천 사진 AI 분석 요청 (A-09)

1. 분류 결과 저장 응답에서 `PRACTICE` 항목의 `id`를 확인합니다. 이 값이 아래 API의 `captureId`입니다.
2. `POST /captures/{captureId}/analysis`에 Bearer access token과 `multipart/form-data`의 `image` 파일을 전송합니다. 본인 소유의 `PRACTICE` 기록에만 요청할 수 있습니다.
3. 응답은 `202`와 `jobId`, `pollAfterMs`를 반환합니다. `GET /analysis-jobs?ids={jobId}`로 상태를 확인하고 실패 시 원인을 표시해 재시도할 수 있습니다. 성공한 결과는 `GET /captures/{captureId}/analysis`로 조회합니다.

예시:

```sh
curl -X POST "$API_URL/captures/12/analysis" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "image=@screenshot.png"
```

실천을 참고로 변경하거나 분류를 취소하면 해당 분석 결과도 삭제됩니다. 실천으로 다시 변경한 경우 파일을 다시 전송해 분석을 요청해야 합니다. 분석용 이미지는 분석 처리에만 사용하고 저장하지 않습니다. 프론트엔드가 분석 요청을 보내는 연동과 사진 원본 보관은 별도 작업입니다.
