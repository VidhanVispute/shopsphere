package com.shopsphere.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();
        String userId    = request.getHeader("X-User-Id");
        String userRole  = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email");

        if (userId != null)    template.header("X-User-Id", userId);
        if (userRole != null)  template.header("X-User-Role", userRole);
        if (userEmail != null) template.header("X-User-Email", userEmail);
    }
}