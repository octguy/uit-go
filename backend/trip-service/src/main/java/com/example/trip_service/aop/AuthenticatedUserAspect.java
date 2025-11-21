package com.example.trip_service.aop;

import com.example.trip_service.exception.UnauthorizedException;
import com.example.trip_service.security.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthenticatedUserAspect {

    @Before("@annotation(RequireUser)")
    public void ensureUserAuthenticated() {
        if (UserContext.getUserId() == null) {
            throw new UnauthorizedException("User not authenticated");
        }
    }
}
