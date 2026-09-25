package com.ucc.chatbot.controller;

import com.ucc.chatbot.service.DemoAcademicService;
import com.ucc.chatbot.model.DemoAris;
import com.ucc.chatbot.model.DemoLms;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentToolsControllerTest {

    @Test
    void studentEndpointsReturn401WhenUnauthenticated() {
        DemoAcademicService academicService = mock(DemoAcademicService.class);
        StudentToolsController controller = new StudentToolsController(academicService);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        ResponseEntity<List<DemoAris>> courses = controller.getCourses();
        assertEquals(401, courses.getStatusCode().value());

        ResponseEntity<DemoAris> fees = controller.getFees();
        assertEquals(401, fees.getStatusCode().value());
    }
}
