package com.moa.moa_be.domain.screenshot.service;

import com.moa.moa_be.domain.screenshot.dto.ClassificationBatchRequest;
import com.moa.moa_be.domain.screenshot.dto.ClassificationResponse;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.domain.user.repository.UserRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreenshotClassificationService {
    private final ScreenshotClassificationRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public List<ClassificationResponse> save(Long userId, ClassificationBatchRequest request) {
        if (request.items().size() > 20) throw new CustomException(GlobalErrorCode.BATCH_LIMIT_EXCEEDED);
        List<String> keys = request.items().stream().map(ClassificationBatchRequest.Item::fileKey).toList();
        if (new HashSet<>(keys).size() != keys.size() || repository.existsByUserIdAndFileKeyIn(userId, keys)) {
            throw new CustomException(GlobalErrorCode.DUPLICATE_FILE_KEY);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        try {
            return repository.saveAllAndFlush(request.items().stream()
                    .map(item -> new ScreenshotClassification(user, item.fileKey(), item.classification())).toList())
                    .stream().map(ClassificationResponse::from).toList();
        } catch (DataIntegrityViolationException ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().contains("uk_screenshot_user_file")) {
                    throw new CustomException(GlobalErrorCode.DUPLICATE_FILE_KEY);
                }
                cause = cause.getCause();
            }
            throw ex;
        }
    }

    public List<ClassificationResponse> list(Long userId) {
        return repository.findByUserIdOrderByIdDesc(userId).stream().map(ClassificationResponse::from).toList();
    }

    @Transactional
    public ClassificationResponse change(Long userId, Long id, Classification classification) {
        ScreenshotClassification entity = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        if (entity.getClassification() == Classification.DELETE || classification == Classification.DELETE) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        entity.changeClassification(classification);
        return ClassificationResponse.from(entity);
    }

    @Transactional
    public void cancel(Long userId, Long id) {
        ScreenshotClassification entity = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        repository.delete(entity);
    }
}
