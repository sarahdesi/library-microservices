package com.library.user_service.security;

import com.library.user_service.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controllers declare a CurrentUser parameter instead of reading the headers by hand */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String id = request.getHeader(CurrentUser.USER_ID_HEADER);
        String role = request.getHeader(CurrentUser.USER_ROLE_HEADER);
        if (id == null && role == null) {
            return CurrentUser.internal();
        }
        try {
            return new CurrentUser(Long.valueOf(id), role);
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Authentication required");
        }
    }
}
