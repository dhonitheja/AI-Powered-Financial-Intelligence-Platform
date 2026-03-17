"use client";

import { useEffect, useRef, useCallback } from "react";
import { plaidService } from "@/services/api";

const POLL_INTERVAL_MS = 30_000; // 30 seconds

interface SyncStatus {
    accountCount: number;
    lastSyncedAt: number; // Unix epoch seconds
}

interface UsePollingSync {
    /** Called whenever the backend reports a new lastSyncedAt value */
    onNewData: () => void;
    /** Set to false to disable polling (e.g. when not logged in) */
    enabled?: boolean;
}

export function usePollingSync({ onNewData, enabled = true }: UsePollingSync) {
    const lastSyncedAt = useRef<number>(0);
    const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const isVisible = useRef<boolean>(true);

    // Keep a stable ref to the latest onNewData so poll() never needs it as a dep
    const onNewDataRef = useRef(onNewData);
    useEffect(() => {
        onNewDataRef.current = onNewData;
    }, [onNewData]);

    // poll has NO deps — it is stable for the lifetime of the component
    const poll = useCallback(async () => {
        if (!isVisible.current) return;
        try {
            const res = await plaidService.getSyncStatus();
            const status: SyncStatus = res.data;

            if (
                lastSyncedAt.current !== 0 &&
                status.lastSyncedAt > lastSyncedAt.current
            ) {
                console.log("[PollingSync] New sync detected — refreshing dashboard");
                onNewDataRef.current();
            }
            lastSyncedAt.current = status.lastSyncedAt;
        } catch {
            // Silently ignore — user may not have any linked accounts yet
        }
    }, []); // stable — no deps

    useEffect(() => {
        if (!enabled) return;

        const handleVisibility = () => {
            isVisible.current = document.visibilityState === "visible";
            if (isVisible.current) {
                poll();
            }
        };
        document.addEventListener("visibilitychange", handleVisibility);

        poll(); // baseline
        intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);

        return () => {
            document.removeEventListener("visibilitychange", handleVisibility);
            if (intervalRef.current) clearInterval(intervalRef.current);
        };
    }, [enabled, poll]); // poll is now stable, so this only runs once
}
