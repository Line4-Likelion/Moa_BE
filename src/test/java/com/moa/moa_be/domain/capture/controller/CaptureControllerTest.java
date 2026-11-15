package com.moa.moa_be.domain.capture.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import com.moa.moa_be.domain.analysis.entity.CategorySource;
import com.moa.moa_be.domain.capture.dto.CaptureActionRequest;
import com.moa.moa_be.domain.capture.dto.CaptureActionResponse;
import com.moa.moa_be.domain.capture.dto.CaptureDetailResponse;
import com.moa.moa_be.domain.capture.dto.CaptureListResponse;
import com.moa.moa_be.domain.capture.dto.CaptureParts.CategoryInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.ChecklistItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.DeadlineInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.FactItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.GuideInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.LinkItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.LinkType;
import com.moa.moa_be.domain.capture.dto.CaptureParts.Progress;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.domain.capture.dto.ChecklistCheckResponse;
import com.moa.moa_be.domain.capture.dto.FactsUpdateRequest;
import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.domain.capture.service.CaptureActionService;
import com.moa.moa_be.domain.capture.service.CaptureService;
import com.moa.moa_be.domain.capture.service.ChecklistService;
import com.moa.moa_be.domain.capture.service.FactService;
import com.moa.moa_be.domain.capture.service.FolderService;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import com.moa.moa_be.global.exception.GlobalExceptionHandler;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CaptureControllerTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-11-11T22:10:00+09:00");

    private CaptureService captureService;
    private CaptureActionService actionService;
    private ChecklistService checklistService;
    private FactService factService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        captureService = mock(CaptureService.class);
        actionService = mock(CaptureActionService.class);
        checklistService = mock(ChecklistService.class);
        factService = mock(FactService.class);
        CaptureController controller = new CaptureController(captureService, actionService, checklistService,
                factService, mock(FolderService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CaptureExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void detailUsesSnakeCaseInsideBaseResponse() throws Exception {
        when(captureService.detail(1L, 12L)).thenReturn(detail());

        mvc.perform(get("/captures/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.file_key").value("media:1"))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.category.is_custom").value(false))
                .andExpect(jsonPath("$.data.category.todo_count").value(2))
                .andExpect(jsonPath("$.data.deadline.date").value("2026-11-30"))
                .andExpect(jsonPath("$.data.deadline.box[2]").value(0.3))
                .andExpect(jsonPath("$.data.checklist_progress.done").value(1))
                .andExpect(jsonPath("$.data.status_changed_at").value("2026-11-11T22:10:00+09:00"))
                .andExpect(jsonPath("$.data.category_source").value("AI"))
                .andExpect(jsonPath("$.data.facts[0].value").doesNotExist())
                .andExpect(jsonPath("$.data.facts[0].edited").value(false))
                .andExpect(jsonPath("$.data.links[0].type").value("MAP"))
                .andExpect(jsonPath("$.data.checklist[0].id").value("chk_1"))
                .andExpect(jsonPath("$.data.next_alarm_at").isEmpty())
                .andExpect(jsonPath("$.data.fileKey").doesNotExist());
    }

    @Test
    void listHasNextCursorAndFacets() throws Exception {
        when(captureService.list(eq(1L), eq("TODO"), eq(true), eq("deadline"), isNull(), isNull(), isNull(),
                isNull())).thenReturn(new CaptureListResponse(List.of(summary()), "bzoyMA",
                new CaptureListResponse.Facets(1, 3)));

        mvc.perform(get("/captures").param("status", "TODO").param("snoozed", "true").param("sort", "deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].snooze_until").value("2026-11-20"))
                .andExpect(jsonPath("$.data.next_cursor").value("bzoyMA"))
                .andExpect(jsonPath("$.data.facets.snoozed").value(1))
                .andExpect(jsonPath("$.data.facets.total").value(3));
    }

    @Test
    void referenceListHasNoFacets() throws Exception {
        when(captureService.list(eq(1L), eq("REFERENCE"), isNull(), isNull(), isNull(), isNull(), eq("none"),
                isNull())).thenReturn(new CaptureListResponse(List.of(), null, null));

        mvc.perform(get("/captures").param("status", "REFERENCE").param("folder_id", "none"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.facets").doesNotExist());
    }

    @Test
    void notFoundUsesCommonErrorFormat() throws Exception {
        when(captureService.detail(1L, 99L)).thenThrow(new CustomException(GlobalErrorCode.NOT_FOUND));

        mvc.perform(get("/captures/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void actionReadsSnakeCaseBodyAndReturnsUndo() throws Exception {
        when(actionService.act(eq(1L), eq(12L), any())).thenReturn(new CaptureActionResponse(summary(),
                new CaptureActionResponse.Undo("UNDO", "und_7xk2", AT)));

        mvc.perform(post("/captures/12/actions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SNOOZE\",\"snooze_until\":\"2026-11-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capture.id").value(12))
                .andExpect(jsonPath("$.data.undo.token").value("und_7xk2"))
                .andExpect(jsonPath("$.data.undo.expires_at").value("2026-11-11T22:10:00+09:00"));

        ArgumentCaptor<CaptureActionRequest> captor = ArgumentCaptor.forClass(CaptureActionRequest.class);
        verify(actionService).act(eq(1L), eq(12L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(CaptureAction.SNOOZE, captor.getValue().action());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 11, 20), captor.getValue().snoozeUntil());
    }

    @Test
    void invalidTransitionIs409() throws Exception {
        when(actionService.act(eq(1L), eq(12L), any()))
                .thenThrow(new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION));

        mvc.perform(post("/captures/12/actions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"COMPLETE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void missingActionIs400() throws Exception {
        mvc.perform(post("/captures/12/actions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void factValidationErrorHasFieldReasons() throws Exception {
        when(factService.update(eq(1L), eq(12L), any(FactsUpdateRequest.class)))
                .thenThrow(FieldValidationException.of("deadline", "존재하지 않는 날짜예요"));

        mvc.perform(patch("/captures/12/facts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facts\":[{\"key\":\"deadline\",\"value\":\"2026-02-30\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요"))
                .andExpect(jsonPath("$.data.fields.deadline").value("존재하지 않는 날짜예요"));
    }

    @Test
    void checklistResponse() throws Exception {
        when(checklistService.check(1L, 12L, "chk_2", true)).thenReturn(new ChecklistCheckResponse(
                new ChecklistItem("chk_2", "예약하기", true), new Progress(2, 2), true));

        mvc.perform(patch("/captures/12/checklist/chk_2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.checked").value(true))
                .andExpect(jsonPath("$.data.progress.total").value(2))
                .andExpect(jsonPath("$.data.all_done").value(true));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/captures/12")).andExpect(status().isNoContent());
        verify(captureService).delete(1L, 12L);
    }

    private static CaptureSummaryResponse summary() {
        return new CaptureSummaryResponse(12L, "media:1", CaptureStatus.TODO, null, "연남 소바실", null, null, null,
                LocalDate.of(2026, 11, 20), null, false, null, AT, AT);
    }

    private static CaptureDetailResponse detail() {
        return new CaptureDetailResponse(12L, "media:1", CaptureStatus.TODO,
                new CategoryInfo(3L, "맛집", false, 2), "연남 소바실", "가을 한정 소바",
                new DeadlineInfo(DeadlineStatus.FOUND, LocalDate.of(2026, 11, 30), "11/30까지",
                        new double[]{0.1, 0.2, 0.3, 0.05}),
                new Progress(1, 2), null, null, false, null, AT, AT, CategorySource.AI,
                List.of(new FactItem("price", "가격", null, null, false)),
                List.of(new LinkItem(LinkType.MAP, "naver.me", "https://naver.me/x")),
                new GuideInfo("방문하기", List.of("예약"), List.of()),
                List.of(new ChecklistItem("chk_1", "예약하기", true), new ChecklistItem("chk_2", "방문", false)),
                null, null);
    }
}
