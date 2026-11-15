package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.capture.dto.CaptureParts.ChecklistItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.Progress;
import com.moa.moa_be.domain.capture.dto.ChecklistCheckResponse;
import com.moa.moa_be.domain.capture.entity.ChecklistCheck;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.capture.repository.ChecklistCheckRepository;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PATCH /captures/{id}/checklist/{item_id} (C-05). 전부 체크해도 자동 완료하지 않음 */
@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ScreenshotClassificationRepository classificationRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final ChecklistCheckRepository checklistCheckRepository;
    private final CaptureAssembler assembler;

    @Transactional
    public ChecklistCheckResponse check(Long userId, Long captureId, String itemId, boolean checked) {
        classificationRepository.findOwnedForUpdate(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        CaptureRow row = captureQueryRepository.findOne(userId, captureId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        AnalysisResult result = assembler.readResult(row);
        int itemNo = parseItemNo(itemId, result);

        Map<Boolean, List<ChecklistCheck>> byCurrent = checklistCheckRepository.findAllByCaptureId(captureId).stream()
                .collect(Collectors.partitioningBy(c -> CaptureAssembler.isCurrent(c.getRecordedAt(), row.analysis())));
        // 다시 분석하기 전에 남은 체크는 정리 (같은 항목을 새로 넣기 전에 먼저 반영)
        if (!byCurrent.get(false).isEmpty()) {
            checklistCheckRepository.deleteAll(byCurrent.get(false));
            checklistCheckRepository.flush();
        }
        List<ChecklistCheck> checks = byCurrent.get(true);
        Set<Integer> current = new HashSet<>(assembler.currentCheckedItems(checks, row.analysis()));

        if (checked && current.add(itemNo)) {
            checklistCheckRepository.save(new ChecklistCheck(row.capture(), itemNo));
        } else if (!checked && current.remove(itemNo)) {
            checks.stream().filter(c -> c.getItemNo() == itemNo).forEach(checklistCheckRepository::delete);
        }

        ChecklistItem item = CaptureAssembler.checklist(result, current).get(itemNo - 1);
        Progress progress = CaptureAssembler.progress(result, current);
        return new ChecklistCheckResponse(item, progress, progress.done() == progress.total());
    }

    /** "chk_3" → 3. 체크리스트가 없거나 범위를 벗어나면 404 */
    private static int parseItemNo(String itemId, AnalysisResult result) {
        int size = result == null || result.checklist() == null ? 0 : result.checklist().size();
        if (itemId != null && itemId.startsWith(CaptureAssembler.CHECKLIST_ID_PREFIX)) {
            try {
                int no = Integer.parseInt(itemId.substring(CaptureAssembler.CHECKLIST_ID_PREFIX.length()));
                if (no >= 1 && no <= size) {
                    return no;
                }
            } catch (NumberFormatException ignored) {
                // 아래에서 404
            }
        }
        throw new CustomException(GlobalErrorCode.NOT_FOUND);
    }
}
