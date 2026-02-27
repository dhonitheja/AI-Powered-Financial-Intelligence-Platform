import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "@/styles/globals.css";
import Shell from "@/components/shared/Shell";
import AuthProvider from "@/components/shared/AuthProvider";
import FloatingChatWidget from "@/components/shared/FloatingChatWidget";
import { Toaster } from "sonner";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
    title: "Antigravity | AI Financial Intelligence",
    description: "Next-generation financial tracking with GPT-4o-mini-powered risk analysis and personal finance chat.",
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en">
            <body className={inter.className}>
                <AuthProvider>
                    <Shell>{children}</Shell>
                    {/* Floating AI chat — visible on all pages when authenticated */}
                    <FloatingChatWidget />
                    <Toaster position="top-right" richColors />
                </AuthProvider>
            </body>
        </html>
    );
}
