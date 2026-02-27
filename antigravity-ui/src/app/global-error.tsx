"use client";

import { useEffect } from "react";
import { Loader2 } from "lucide-react";

export default function GlobalError({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    useEffect(() => {
        console.error("Global Layout Error:", error);

        if (
            error.message.includes("Failed to load chunk") ||
            error.message.includes("Loading chunk") ||
            error.name === 'ChunkLoadError'
        ) {
            window.location.reload();
        }
    }, [error]);

    return (
        <html lang="en">
            <body>
                <div className="flex flex-col items-center justify-center min-h-screen bg-[#F8FAFC]">
                    <div className="bg-white p-8 rounded-2xl shadow-xl shadow-slate-900/5 text-center flex flex-col items-center gap-6 border border-slate-100 max-w-sm">
                        <div className="bg-rose-50 w-16 h-16 rounded-full flex items-center justify-center">
                            <Loader2 className="w-8 h-8 text-rose-500 animate-[spin_3s_linear_infinite]" />
                        </div>
                        <div className="space-y-1">
                            <h2 className="text-xl font-bold text-slate-800 tracking-tight">Application Sync Error</h2>
                            <p className="text-slate-500 bg-slate-50 p-3 rounded-lg text-xs leading-relaxed border border-slate-100">
                                {error.message}
                            </p>
                        </div>
                        <button
                            onClick={() => window.location.reload()}
                            className="bg-primary hover:-translate-y-0.5 transition-all text-white w-full rounded-xl py-3 font-semibold shadow-lg shadow-teal-900/20"
                        >
                            Hard Reload Application
                        </button>
                    </div>
                </div>
            </body>
        </html>
    );
}
