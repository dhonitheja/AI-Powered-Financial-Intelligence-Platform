"use client";

import React, { useCallback, useState, useEffect, useMemo } from 'react';
import { usePlaidLink, PlaidLinkOnSuccess, PlaidLinkOptions, PlaidLinkOnExit } from 'react-plaid-link';
import { plaidService } from '@/services/api';
import { Loader2, Link as LinkIcon, CheckCircle2, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

interface PlaidLinkProps {
    onSuccess?: () => void;
    userId?: string;
}

// We removed PlaidLinkTrigger entirely to avoid breaking Hook sequencing rules
const PlaidLink: React.FC<PlaidLinkProps> = ({ onSuccess, userId = "default-user" }) => {
    const [token, setToken] = useState<string | null>(null);
    const [isConnecting, setIsConnecting] = useState(false);
    const [isSyncing, setIsSyncing] = useState(false);

    type ConnectionStatus = 'idle' | 'linking' | 'exchanging' | 'syncing' | 'connected' | 'error';
    const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('idle');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const startConnect = async () => {
        setErrorMessage(null);
        setIsConnecting(true);
        setConnectionStatus('linking');
        try {
            console.log("[Plaid] Fetching link token...");
            const response = await plaidService.createLinkToken(userId);
            setToken(response.data.link_token);
            // The inner PlaidLinkTrigger gets rendered and opens modal.
        } catch (error: any) {
            console.error('[Plaid] Error creating link token:', error);
            setErrorMessage(error?.response?.data?.error || "Failed to initialize bank connection. Check console.");
            setConnectionStatus('error');
            setIsConnecting(false);
        }
    };

    const handlePlaidExit = useCallback<PlaidLinkOnExit>((error, metadata) => {
        console.log("[Plaid] Modal closed/exited. Status:", metadata.status);
        if (error) {
            console.error("[Plaid] Exit Error:", error);
        }
        setToken(null);
        setIsConnecting(false);
        setConnectionStatus('idle');
    }, []);

    const handlePlaidSuccess = useCallback<PlaidLinkOnSuccess>(async (public_token, metadata) => {
        console.log("[Plaid] User linked bank! Commencing exchange for public_token...");
        setIsConnecting(true);
        setConnectionStatus('exchanging');
        setErrorMessage(null);

        // We close the modal from Plaid's end automatically by react-plaid-link
        // But we clean up our token so we dont re-render old session
        setToken(null);

        try {
            // 1. Await exchange completion
            console.log("[Plaid] EXCHANGING public_token...");
            const exchangeRes = await plaidService.exchangePublicToken(public_token, userId);

            console.log("[Plaid] Exchange Endpoint Response:", exchangeRes.data);

            if (exchangeRes.status !== 200) {
                console.error("[Plaid] Exchange HTTP Error Status:", exchangeRes.status, exchangeRes.data);
                throw new Error(exchangeRes.data?.error || `Exchange failed with status ${exchangeRes.status}`);
            }

            if (!exchangeRes.data?.success) {
                console.error("[Plaid] Exchange 200 OK but Success=false:", exchangeRes.data);
                throw new Error(exchangeRes.data?.error || "Exchange endpoint returned 200 but success flag was false (no specific error provided).");
            }

            console.log("[Plaid] EXCHANGE COMPLETE. Access token securely stored.");

            // 2. Await sync transactions
            setConnectionStatus('syncing');
            setIsSyncing(true);
            console.log("[Plaid] SYNCING transactions...");

            const syncRes = await plaidService.syncTransactions(userId);
            console.log("[Plaid] SYNC COMPLETE. Imported:", syncRes.data.imported_count);

            setConnectionStatus('connected');
            setIsSyncing(false);
            setIsConnecting(false);

            if (onSuccess) onSuccess();

        } catch (error: any) {
            console.error('[Plaid] API Flow Error:', error);
            setErrorMessage(
                error?.response?.data?.error ||
                error?.message ||
                "An error occurred during bank verification or sync."
            );
            setConnectionStatus('error');
            setIsConnecting(false);
            setIsSyncing(false);
        }
    }, [onSuccess, userId]);

    // ─── Hook Plaid Link unconditionally at top level ───
    const config: PlaidLinkOptions = {
        token: token,
        onSuccess: handlePlaidSuccess,
        onExit: handlePlaidExit,
    };

    const { open, ready } = usePlaidLink(config);

    // Watch for when token is fetched and ready state aligns
    useEffect(() => {
        if (token && ready) {
            open();
        }
    }, [token, ready, open]);

    if (connectionStatus === 'connected') {
        return (
            <div className="flex items-center gap-2 text-success font-bold text-sm bg-emerald-50 px-4 py-2 rounded-xl border border-emerald-100">
                <CheckCircle2 className="w-4 h-4" />
                Bank Connected Successfully
            </div>
        );
    }

    return (
        <div className="flex flex-col items-start gap-2">
            <button
                onClick={startConnect}
                disabled={isConnecting || isSyncing}
                className="bg-primary text-white px-6 py-2.5 rounded-xl font-bold text-sm shadow-lg shadow-teal-900/20 flex items-center gap-2 transition-all hover:-translate-y-1 hover:shadow-teal-900/30 disabled:opacity-50 disabled:translate-y-0"
            >
                {isConnecting || isSyncing ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                    <LinkIcon className="w-4 h-4" />
                )}
                {connectionStatus === 'exchanging' && 'Securing Connection...'}
                {connectionStatus === 'syncing' && 'Syncing Transactions...'}
                {connectionStatus === 'linking' && 'Opening Secure Gateway...'}
                {connectionStatus === 'idle' && 'Connect Bank Account'}
                {connectionStatus === 'error' && 'Retry Bank Connection'}
            </button>

            {connectionStatus === 'error' && errorMessage && (
                <div className="flex items-center gap-1.5 text-xs text-rose-500 font-medium">
                    <AlertCircle className="w-3.5 h-3.5" />
                    {errorMessage}
                </div>
            )}
        </div>
    );
};

export default PlaidLink;
