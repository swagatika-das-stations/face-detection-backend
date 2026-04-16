package com.stations.facedetection.Security.Config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        log.info("Incoming request: method={}, url={}, path={}, contentType={}",
                httpRequest.getMethod(),
                httpRequest.getRequestURL(),
                httpRequest.getServletPath(),
                httpRequest.getContentType());

        chain.doFilter(request, response);
    }
}