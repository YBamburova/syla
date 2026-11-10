package com.syla.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounds JSON input in memory after the security chain; never logs request contents. */
@Component
class JsonRequestLimit extends OncePerRequestFilter {
    private static final int MAX_BYTES = 256 * 1024;
    private final ObjectMapper mapper;
    JsonRequestLimit(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
            chain.doFilter(request, response); return;
        }
        byte[] data = request.getContentLengthLong() > MAX_BYTES ? new byte[MAX_BYTES + 1]
                : request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (data.length > MAX_BYTES) {
            response.setStatus(413); response.setContentType("application/problem+json");
            mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(
                    HttpStatus.PAYLOAD_TOO_LARGE, "JSON requests are limited to 256 KiB."));
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(data);
                return new ServletInputStream() {
                    @Override public int read() { return input.read(); }
                    @Override public boolean isFinished() { return input.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous request body"); }
                };
            }
            @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
        }, response);
    }
}
