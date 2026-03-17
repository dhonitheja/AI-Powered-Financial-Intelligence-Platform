/**
 * Client-side error tracking for Wealthix UI.
 * NEVER log: account numbers, amounts, JWT tokens, PII.
 * SAFE to log: page path, component name, error type.
 */

export function trackError(
    error: Error,
    context: { page: string; component: string }) {
    // Only log sanitized error info
    console.error("[Wealthix]", {
        message: error.message,
        page: context.page,
        component: context.component,
        timestamp: new Date().toISOString(),
        // Stack trace in dev only
        ...(process.env.NODE_ENV === "development"
            ? { stack: error.stack } : {})
    });

    // In production: send to your error tracking service
    // e.g., Sentry, Datadog — with PII scrubbing enabled
}

export function trackPageView(path: string) {
    // Log page views without query params
    // (query params might contain sensitive data)
    const safePath = path.split("?")[0];
    console.info("[Wealthix] Page view:", safePath);
}
