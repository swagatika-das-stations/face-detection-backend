package com.stations.facedetection.Security.Config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        System.out.println("=".repeat(70));
        System.out.println("INCOMING REQUEST TO APPLICATION");
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("URL: " + httpRequest.getRequestURL());
        System.out.println("Path: " + httpRequest.getServletPath());
        System.out.println("Content-Type: " + httpRequest.getContentType());
        System.out.println("=".repeat(70));
        
        chain.doFilter(request, response);
    }
}
