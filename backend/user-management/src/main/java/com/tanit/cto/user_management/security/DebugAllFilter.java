package com.tanit.cto.user_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class DebugAllFilter extends OncePerRequestFilter {

        @Autowired
        private ServletContext servletContext;

        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                        @NonNull HttpServletResponse response,
                        @NonNull FilterChain chain) throws ServletException, IOException {

                System.out.println(" Enter filter: " + this.getClass().getSimpleName() +
                                " | method=" + request.getMethod() +
                                " | path=" + request.getServletPath() +
                                " | handledBy=" + request.getClass().getName());
                System.out.println("=== All Filters for this request ===");
                servletContext.getFilterRegistrations().forEach((name, reg) -> {
                        System.out.println(name + " -> class=" + reg.getClassName() + " urlPatterns="
                                        + reg.getUrlPatternMappings());
                });

                chain.doFilter(request, response);

                System.out.println(" Exit filter: " + this.getClass().getSimpleName() +
                                " | method=" + request.getMethod() +
                                " | path=" + request.getServletPath() +
                                " | status=" + response.getStatus() +
                                " | handledBy=" + request.getClass().getName());
        }
}
