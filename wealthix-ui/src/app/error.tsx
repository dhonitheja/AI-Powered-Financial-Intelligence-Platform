"use client";

import { useEffect } from "react";
import { Loader2 } from "lucide-react";

export default function Error({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    useEffect(() => {
        console.error("App error caught by Error Boundary:", error);

        // ChunkLoadError mostly happens when Next.js dev server is restarted 
        // and browser is trying to fetch old chunks from previous compilation
        if (
            error.message.includes("Failed to load chunk") ||
            error.message.includes("Loading chunk") ||
            error.name === 'ChunkLoadError'
        ) {
            const reloaded = sessionStorage.getItem("chunkReloadAttempted");
            if (!reloaded) {
                sessionStorage.setItem("chunkReloadAttempted", "true");
                window.location.reload(); // Force a fresh page reload from the server
            } else {
                sessionStorage.removeItem("chunkReloadAttempted");
            }
        }
    }, [error]);

    return (
        <div className="flex h-screen items-center justify-center bg-[#F8FAFC] flex-col gap-5">
            <div className="bg-rose-50 p-4 rounded-full">
                <Loader2 className="w-8 h-8 text-rose-500" />
            </div>
            <div className="text-center space-y-2">
                <h2 className="text-xl font-bold text-slate-800 tracking-tight">Application Sync Error</h2>
                <p className="text-slate-500 text-sm max-w-sm">
                    {error.message.includes("Loading chunk")
                        ? "The application was updated. Refreshing the browser..."
                        : "An unexpected error occurred while loading the application."}
                </p>
            </div>
            <button
                onClick={() => {
                    sessionStorage.removeItem("chunkReloadAttempted");
                    window.location.reload();
                }}
                className="bg-primary text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:-translate-y-0.5 transition-all shadow-lg shadow-teal-900/20"
            >
                Force Refresh
            </button>
        </div>
    );
}
