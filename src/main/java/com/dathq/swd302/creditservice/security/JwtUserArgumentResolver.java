package com.dathq.swd302.creditservice.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class JwtUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(JwtUser.class) != null &&
                parameter.getParameterType().equals(JwtClaims.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object claims = webRequest.getAttribute("jwtClaims", RequestAttributes.SCOPE_REQUEST);
        if (claims instanceof JwtClaims) {
            return claims;
        }

        // If the token was not present or valid, return an empty claims object or throw
        // an exception based on your auth logic.
        // Assuming requests reaching here without claims handle anonymous users or
        // throw 401 via another mechanism.
        return null;
    }
}
