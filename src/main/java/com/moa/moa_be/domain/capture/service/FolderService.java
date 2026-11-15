package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.capture.dto.CaptureItemsResponse;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.domain.capture.dto.FolderResponse;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.entity.Folder;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.capture.repository.FolderRepository;
import com.moa.moa_be.domain.user.repository.UserRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 참고함 폴더 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final CaptureAssembler assembler;

    /** GET /folders */
    public List<FolderResponse> list(Long userId) {
        Map<Long, Long> counts = captureQueryRepository.countReferenceByFolder(userId);
        return folderRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(f -> FolderResponse.of(f, counts.getOrDefault(f.getId(), 0L)))
                .toList();
    }

    /** POST /folders */
    @Transactional
    public FolderResponse create(Long userId, String name) {
        String trimmed = name.trim();
        if (folderRepository.existsByUserIdAndName(userId, trimmed)) {
            throw new CustomException(GlobalErrorCode.DUPLICATE_NAME);
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        try {
            return FolderResponse.of(folderRepository.saveAndFlush(new Folder(user, trimmed)), 0);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(GlobalErrorCode.DUPLICATE_NAME); // 동시에 같은 이름을 만든 경우
        }
    }

    /** PATCH /folders/{id} */
    @Transactional
    public FolderResponse rename(Long userId, Long folderId, String name) {
        Folder folder = getMine(userId, folderId);
        String trimmed = name.trim();
        if (!folder.getName().equals(trimmed)) {
            if (folderRepository.existsByUserIdAndName(userId, trimmed)) {
                throw new CustomException(GlobalErrorCode.DUPLICATE_NAME);
            }
            folder.rename(trimmed);
            try {
                folderRepository.flush();
            } catch (DataIntegrityViolationException e) {
                throw new CustomException(GlobalErrorCode.DUPLICATE_NAME);
            }
        }
        long count = captureQueryRepository.countReferenceByFolder(userId).getOrDefault(folderId, 0L);
        return FolderResponse.of(folder, count);
    }

    /** DELETE /folders/{id} — 안의 항목은 지우지 않고 "폴더 없음"으로 */
    @Transactional
    public void delete(Long userId, Long folderId) {
        Folder folder = getMine(userId, folderId);
        captureQueryRepository.clearFolder(userId, folderId);
        folderRepository.delete(folder);
    }

    /** PATCH /captures/{id}/folder */
    @Transactional
    public CaptureSummaryResponse move(Long userId, Long captureId, Long folderId) {
        return moveAll(userId, List.of(captureId), folderId).items().get(0);
    }

    /** PATCH /captures/folder — 하나라도 없으면 404, 참고 항목이 아니면 409 (전체 실패) */
    @Transactional
    public CaptureItemsResponse moveAll(Long userId, List<Long> captureIds, Long folderId) {
        if (folderId != null) {
            getMine(userId, folderId);
        }
        List<Long> ids = captureIds.stream().distinct().toList();
        List<CaptureRow> rows = captureQueryRepository.findAllByIds(userId, ids);
        if (rows.size() != ids.size()) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND);
        }
        if (rows.stream().anyMatch(r -> r.status() != CaptureStatus.REFERENCE)) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        rows.forEach(r -> r.capture().moveToFolder(folderId));
        Map<Long, CaptureRow> byId = rows.stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.capture().getId(), r -> r));
        // 요청 순서대로 응답
        return new CaptureItemsResponse(assembler.toSummaries(userId, ids.stream().map(byId::get).toList()));
    }

    private Folder getMine(Long userId, Long folderId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
    }
}
