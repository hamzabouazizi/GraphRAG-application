package com.tanit.cto.user_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DebugAllFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain chain) throws ServletException, IOException {

                System.out.println("Enter filter: " + this.getClass().getSimpleName() +
                                " path=" + request.getServletPath());

                chain.doFilter(request, response);

                System.out.println("Exit filter: " + this.getClass().getSimpleName() +
                                " path=" + request.getServletPath() +
                                " status=" + response.getStatus());
        }
}
