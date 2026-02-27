/**
 * usePollingSync — polls /api/plaid/sync-status every POLL_INTERVAL_MS.
 *
 * Security notes:
 *  - The endpoint returns only { accountCount, lastSyncedAt } — no userId is
 *    ever sent from the frontend or returned from the server.
 *  - The hook compares lastSyncedAt across polls; if it changes, it fires
 *    onNewData() so the caller can re-fetch transactions.
 *  - Polling is automatically paused when the browser tab is hidden
 *    (via visibilitychange) and resumed when visible again.
 *  - The hook is cancelled on unmount — no stale intervals.
 */

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

    const poll = useCallback(async () => {
        if (!isVisible.current) return; // skip polls when tab is hidden
        try {
            const res = await plaidService.getSyncStatus();
            const status: SyncStatus = res.data;

            if (
                lastSyncedAt.current !== 0 &&          // skip the first poll (baseline)
                status.lastSyncedAt > lastSyncedAt.current  // new data detected
            ) {
                console.log("[PollingSync] New sync detected — refreshing dashboard");
                onNewData();
            }
            lastSyncedAt.current = status.lastSyncedAt;
        } catch {
            // Silently ignore — user may not have any linked accounts yet
        }
    }, [onNewData]);

    useEffect(() => {
        if (!enabled) return;

        // Visibility-aware polling pause/resume
        const handleVisibility = () => {
            isVisible.current = document.visibilityState === "visible";
            if (isVisible.current) {
                poll(); // immediate catch-up poll on tab focus
            }
        };
        document.addEventListener("visibilitychange", handleVisibility);

        // Run first poll immediately to set the baseline
        poll();
        intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);

        return () => {
            document.removeEventListener("visibilitychange", handleVisibility);
            if (intervalRef.current) clearInterval(intervalRef.current);
        };
    }, [enabled, poll]);
}
