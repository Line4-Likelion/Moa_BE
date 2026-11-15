package com.moa.moa_be.domain.user.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.moa.moa_be.domain.user.entity.User;
import org.junit.jupiter.api.Test;

class UserResponseTest {
    @Test
    void formatsDatabaseIdWithUserPrefix() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(31L);

        assertEquals("usr_31", new UserResponse(user).getId());
    }
}
