"use client";

import React, { useState, useEffect } from 'react';
import { Card } from '@/components/ui/Card';
import {
    User,
    Bell,
    Shield,
    CreditCard,
    Globe,
    Zap,
    ChevronRight,
    Github,
    RefreshCw,
    Loader2,
    CheckCircle2,
    QrCode,
    Camera,
    ShieldCheck,
    Lock
} from 'lucide-react';
import PlaidLink from '@/components/shared/PlaidLink';
import { plaidService, authService } from '@/services/api';
import { useAuth } from '@/services/authStore';
import { toast } from 'sonner';

export default function SettingsPage() {
    const { user } = useAuth();
    const [syncing, setSyncing] = useState(false);
    const [syncResult, setSyncResult] = useState<{ count: number } | null>(null);

    // 2FA State
    const [is2faEnabled, setIs2faEnabled] = useState(user?.twoFactorEnabled || false);
    const [showing2faSetup, setShowing2faSetup] = useState(false);
    const [qrCode, setQrCode] = useState("");
    const [secret, setSecret] = useState("");
    const [loading2fa, setLoading2fa] = useState(false);

    useEffect(() => {
        if (user) {
            setIs2faEnabled(user.twoFactorEnabled || false);
        }
    }, [user?.twoFactorEnabled]);

    const handleSync = async () => {
        setSyncing(true);
        setSyncResult(null);
        try {
            await plaidService.syncTransactions();
            setSyncResult({ count: 5 }); // Demo count
        } catch (error) {
            console.error('Sync failed:', error);
        } finally {
            setSyncing(false);
        }
    };

    const handlePlaidSuccess = () => {
        toast.success('Bank connected securely! Syncing your transaction history...');
        handleSync();
    };

    const handleSetup2FA = async () => {
        setLoading2fa(true);
        try {
            const res = await authService.setup2FA();
            setQrCode(res.data.qrCodeUri);
            setSecret(res.data.secret);
            setShowing2faSetup(true);
        } catch (err) {
            console.error("Failed to setup 2FA", err);
        } finally {
            setLoading2fa(false);
        }
    };

    const handleToggle2FA = async (enable: boolean) => {
        setLoading2fa(true);
        try {
            await authService.toggle2FA(enable);
            setIs2faEnabled(enable);
            if (!enable) setShowing2faSetup(false);
        } catch (err) {
            console.error("Failed to toggle 2FA", err);
        } finally {
            setLoading2fa(false);
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto space-y-8 pb-20">
            <div>
                <h1 className="text-3xl font-black text-primary tracking-tight">Settings</h1>
                <p className="text-slate-500 mt-1 font-medium">Manage your security and platform preferences.</p>
            </div>

            <div className="space-y-6">
                {/* Security Section */}
                <Card title="Security & Authentication" subtitle="Protect your account with advanced verification">
                    <div className="mt-6 space-y-6">
                        <div className="flex items-center justify-between p-4 bg-slate-50 rounded-2xl border border-slate-100">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-white rounded-xl shadow-sm">
                                    <ShieldCheck className={is2faEnabled ? "text-success w-6 h-6" : "text-slate-400 w-6 h-6"} />
                                </div>
                                <div>
                                    <h4 className="font-bold text-primary flex items-center gap-2">
                                        Two-Factor Authentication (TOTP)
                                        {is2faEnabled && <span className="text-[10px] bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded-full">Active</span>}
                                    </h4>
                                    <p className="text-xs text-slate-500">Adds an extra layer of security using Google Authenticator or Authy.</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                {is2faEnabled ? (
                                    <button
                                        onClick={() => handleToggle2FA(false)}
                                        className="text-xs font-bold text-rose-500 hover:underline"
                                    >
                                        Disable
                                    </button>
                                ) : (
                                    <button
                                        onClick={handleSetup2FA}
                                        disabled={loading2fa}
                                        className="bg-primary text-white px-5 py-2 rounded-xl font-bold text-xs shadow-sm shadow-teal-900/10 hover:scale-105 transition-all"
                                    >
                                        {loading2fa ? <Loader2 className="w-4 h-4 animate-spin" /> : "Setup 2FA"}
                                    </button>
                                )}
                            </div>
                        </div>

                        {showing2faSetup && !is2faEnabled && (
                            <div className="p-6 bg-teal-50/50 border border-teal-100 rounded-2xl animate-in slide-in-from-top-4 duration-500">
                                <div className="grid md:grid-cols-2 gap-8 items-center">
                                    <div className="space-y-4">
                                        <div className="flex items-center gap-2 text-secondary font-bold">
                                            <QrCode className="w-5 h-5" />
                                            <span>Scan QR Code</span>
                                        </div>
                                        <p className="text-xs text-slate-600 leading-relaxed">
                                            Scan the image on the right with your authenticator app. If you can't scan, use the manual secret below:
                                        </p>
                                        <div className="p-3 bg-white border border-teal-100 rounded-lg font-mono text-xs break-all text-secondary select-all cursor-pointer" title="Click to select">
                                            {secret}
                                        </div>
                                        <button
                                            onClick={() => handleToggle2FA(true)}
                                            className="w-full bg-secondary text-white py-3 rounded-xl font-bold text-sm shadow-lg shadow-teal-900/10 hover:shadow-teal-900/20 transition-all"
                                        >
                                            I've scanned it, Enable 2FA
                                        </button>
                                    </div>
                                    <div className="flex justify-center">
                                        {qrCode ? (
                                            <div className="p-4 bg-white rounded-2xl border-4 border-white shadow-xl">
                                                <img src={qrCode} alt="QR Code" className="w-40 h-40" />
                                            </div>
                                        ) : (
                                            <div className="w-40 h-40 bg-slate-100 rounded-xl flex items-center justify-center">
                                                <Loader2 className="w-6 h-6 text-slate-300 animate-spin" />
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </Card>

                <Card title="Integrations" subtitle="Connected financial services">
                    <div className="space-y-6 mt-4">
                        <div className="flex flex-col md:flex-row items-start md:items-center justify-between p-4 bg-slate-50 rounded-2xl border border-slate-100 gap-4">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-white rounded-xl shadow-sm">
                                    <Zap className="text-secondary w-6 h-6" />
                                </div>
                                <div>
                                    <h4 className="font-bold text-primary">Plaid Bank Sync</h4>
                                    <p className="text-xs text-slate-500">Securely connect to your financial institutions.</p>
                                </div>
                            </div>
                            <PlaidLink onSuccess={handlePlaidSuccess} />
                        </div>
                    </div>
                </Card>

                <Card title="Session Management" subtitle="Manage your current active login sessions">
                    <div className="mt-4 p-4 border border-slate-100 rounded-2xl flex items-center justify-between bg-slate-50">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
                                <Shield className="w-5 h-5 text-emerald-600" />
                            </div>
                            <div>
                                <p className="text-sm font-bold text-primary">Current Session</p>
                                <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">This Browser • Active Now</p>
                            </div>
                        </div>
                        <button className="text-xs font-bold text-slate-400 hover:text-primary transition-colors">Terminate</button>
                    </div>
                </Card>
            </div>
        </div>
    );
}
