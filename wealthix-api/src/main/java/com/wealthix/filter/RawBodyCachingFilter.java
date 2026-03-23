package com.wealthix.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Caches the raw request body bytes so that webhook signature verification
 * can read the exact bytes Plaid sent — BEFORE Spring parses the JSON.
 *
 * Without this, PlaidWebhookController would have to re-serialize the parsed
 * Map back to JSON, which can produce a different byte sequence (different
 * key order, whitespace, etc.) and cause HMAC verification to fail.
 *
 * Only activates for webhook paths to avoid buffering large unrelated requests.
 */
@Component
public class RawBodyCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpReq) {
            String path = httpReq.getRequestURI();
            if (path.contains("/webhook") || path.contains("/webhooks/")) {
                CachedBodyHttpServletRequest cachedRequest =
                        new CachedBodyHttpServletRequest(httpReq);
                chain.doFilter(cachedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    // ── Wrapper that reads and caches the body bytes ──────────────────────────

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        /** Returns the cached raw body as a UTF-8 string for HMAC computation. */
        public String getRawBody() {
            return new String(cachedBody, java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public int read() { return byteStream.read(); }
                @Override public boolean isFinished() { return byteStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }
}
