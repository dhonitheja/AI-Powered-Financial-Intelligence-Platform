import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// This function can be marked `async` if using `await` inside
export function middleware(request: NextRequest) {
    const { pathname } = request.nextUrl;

    // Define public routes that don't require authentication
    const isPublicRoute = pathname === '/login' || pathname === '/register' || pathname === '/forgot-password' || pathname === '/reset-password' || pathname === '/' || pathname.startsWith('/_next') || pathname.startsWith('/api/');

    const token = request.cookies.get('ai_finance_jwt');

    if (!token && !isPublicRoute) {
        // Redirect unauthenticated users trying to access protected routes to login
        return NextResponse.redirect(new URL('/login', request.url));
    }

    if (token && (pathname === '/login' || pathname === '/register' || pathname === '/forgot-password' || pathname === '/reset-password')) {
        // Redirect authenticated users trying to access auth pages to dashboard
        return NextResponse.redirect(new URL('/dashboard', request.url));
    }

    return NextResponse.next();
}

export const config = {
    matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
