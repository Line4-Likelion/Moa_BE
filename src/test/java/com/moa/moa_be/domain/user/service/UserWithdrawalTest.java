package com.moa.moa_be.domain.user.service;

import static org.mockito.Mockito.*;

import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import com.moa.moa_be.domain.category.service.CategoryService;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserWithdrawalTest {
    @Test
    void deletesAnalysisCategoriesAndClassificationsBeforeUser() {
        var users = mock(UserRepository.class);
        var categories = mock(CategoryService.class);
        var analyses = mock(ScreenshotAnalysisRepository.class);
        var classifications = mock(ScreenshotClassificationRepository.class);
        var user = User.builder().email("user@example.com").password("password").nickname("user").build();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        var service = new UserService(users, mock(PasswordEncoder.class), classifications, categories, analyses);

        service.withdrawUser(1L);

        InOrder order = inOrder(analyses, categories, classifications, users);
        order.verify(analyses).deleteAllByUserId(1L);
        order.verify(categories).deleteAllByUserId(1L);
        order.verify(classifications).deleteByUserId(1L);
        order.verify(users).delete(user);
    }
}
