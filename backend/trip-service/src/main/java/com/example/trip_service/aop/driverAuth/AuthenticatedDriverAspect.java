package com.example.trip_service.aop.driverAuth;

import com.example.trip_service.exception.UnauthorizedException;
import com.example.trip_service.security.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class AuthenticatedDriverAspect {

    @Before("@annotation(RequireDriver)")
    public void ensureDriverAuthenticated() {
        if (UserContext.getUserId() == null || !Objects.equals(UserContext.getUserRole(), "ROLE_DRIVER")) {
            System.out.println("In Aspect, user id is null or role is not ROLE_DRIVER");
            throw new UnauthorizedException("User not authenticated or does not have DRIVER role");
        }
    }
}
