package com.example.trip_service.aop.passengerAuth;

import com.example.trip_service.exception.UnauthorizedException;
import com.example.trip_service.security.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class AuthenticatedPassengerAspect {

    @Before("@annotation(RequirePassenger)")
    public void ensurePassengerAuthenticated() {
        if (UserContext.getUserId() == null || !Objects.equals(UserContext.getUserRole(), "ROLE_USER")) {
            System.out.println("In Aspect, user id is null or role is not ROLE_USER");
            throw new UnauthorizedException("User not authenticated or does not have USER role");
        }
    }
}
