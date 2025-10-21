package com.tanit.cto.user_management.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class FilterLister implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ServletContext servletContext;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        System.out.println("=== Servlet Filter Registrations ===");
        servletContext.getFilterRegistrations().forEach((name, reg) -> {
            System.out.println(name + " -> class=" + reg.getClassName()
                    + " urlPatterns=" + reg.getUrlPatternMappings());
        });
        System.out.println("====================================");
    }

}
