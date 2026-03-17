"use client";

import React, { useState, useEffect } from 'react';
import { Card, cn } from '@/components/ui/Card';
import {
    Shield,
    Zap,
    Loader2,
    QrCode,
    ShieldCheck,
    Building2,
    CreditCard,
    PiggyBank,
    Plus,
    Trash2,
    AlertTriangle,
} from 'lucide-react';
import PlaidLink from '@/components/shared/PlaidLink';
import { plaidService, authService, budgetService, CategoryBudget, BudgetPeriod } from '@/services/api';
import { useAuth } from '@/services/authStore';
import { toast } from 'sonner';

// ── Category options matching backend ─────────────────────────────────────────
const CATEGORY_OPTIONS = [
    'FOOD_AND_DRINK', 'GROCERIES', 'RESTAURANTS', 'GAS_STATIONS',
    'TRANSPORTATION', 'RENT', 'UTILITIES', 'HEALTH', 'ENTERTAINMENT',
    'SHOPPING', 'SUBSCRIPTION', 'TRAVEL', 'EDUCATION', 'UNCATEGORIZED',
];

// ── Connected account row ─────────────────────────────────────────────────────
interface BankConnection {
    id: string;
    institutionName: string;
    accountName: string | null;
    accountType: string | null;
    accountSubtype: string | null;
    accountMask: string | null;
    currentBalance: number | null;
    creditLimit: number | null;
    active: boolean;
}

function ConnectedAccountRow({ conn }: { conn: BankConnection }) {
    const isCredit = conn.accountType?.toLowerCase() === 'credit';
    const Icon = isCredit ? CreditCard : conn.accountType?.toLowerCase() === 'depository' ? Building2 : PiggyBank;
    const iconBg = isCredit ? 'bg-rose-500/15' : 'bg-blue-500/15';
    const iconColor = isCredit ? 'text-rose-400' : 'text-blue-400';

    return (
        <div className="flex items-center justify-between p-4 bg-white/5 rounded-2xl border border-white/10">
            <div className="flex items-center gap-4">
                <div className={cn('w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0', iconBg)}>
                    <Icon className={cn('w-5 h-5', iconColor)} />
                </div>
                <div>
                    <p className="font-bold text-slate-200 text-sm">
                        {conn.institutionName}
                        {conn.accountMask && (
                            <span className="ml-2 text-slate-500 font-normal">••••{conn.accountMask}</span>
                        )}
                    </p>
                    <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">
                        {conn.accountName || conn.accountSubtype || conn.accountType || 'Account'}
                    </p>
                </div>
            </div>
            <div className="text-right">
                <p className="font-black text-slate-100 text-sm">
                    {conn.currentBalance != null
                        ? `$${conn.currentBalance.toLocaleString(undefined, { maximumFractionDigits: 0 })}`
                        : '—'}
                </p>
                <p className="text-[9px] text-slate-500 uppercase font-bold">{isCredit ? 'Owed' : 'Available'}</p>
                <span className={cn(
                    'text-[9px] px-2 py-0.5 rounded-full font-bold mt-1 inline-block',
                    conn.active ? 'bg-emerald-500/15 text-emerald-400' : 'bg-slate-500/15 text-slate-500'
                )}>
                    {conn.active ? 'Active' : 'Inactive'}
                </span>
            </div>
        </div>
    );
}

// ── Budget limit row ──────────────────────────────────────────────────────────
function BudgetLimitRow({ budget, onDelete }: { budget: CategoryBudget; onDelete: () => void }) {
    return (
        <div className="flex items-center justify-between p-4 bg-white/5 rounded-2xl border border-white/10">
            <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-[#D4AF37]/15 flex items-center justify-center text-[11px] font-black text-[#D4AF37]">
                    {budget.period === 'WEEKLY' ? 'WK' : 'MO'}
                </div>
                <div>
                    <p className="font-bold text-slate-200 text-sm">{budget.category.replace(/_/g, ' ')}</p>
                    <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">{budget.period.toLowerCase()} limit</p>
                </div>
            </div>
            <div className="flex items-center gap-4">
                <p className="font-black text-slate-100">${budget.limitAmount.toLocaleString()}</p>
                <button
                    onClick={onDelete}
                    className="p-1.5 rounded-lg text-slate-600 hover:text-rose-400 hover:bg-rose-500/10 transition-all"
                    title="Remove limit"
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function SettingsPage() {
    const { user } = useAuth();

    const [syncing, setSyncing] = useState(false);
    const [is2faEnabled, setIs2faEnabled] = useState(user?.twoFactorEnabled || false);
    const [showing2faSetup, setShowing2faSetup] = useState(false);
    const [qrCode, setQrCode] = useState('');
    const [secret, setSecret] = useState('');
    const [loading2fa, setLoading2fa] = useState(false);

    const [connections, setConnections] = useState<BankConnection[]>([]);
    const [loadingConnections, setLoadingConnections] = useState(false);

    const [budgets, setBudgets] = useState<CategoryBudget[]>([]);
    const [loadingBudgets, setLoadingBudgets] = useState(false);
    const [newCategory, setNewCategory] = useState('');
    const [newAmount, setNewAmount] = useState('');
    const [newPeriod, setNewPeriod] = useState<BudgetPeriod>('WEEKLY');
    const [savingBudget, setSavingBudget] = useState(false);

    useEffect(() => {
        if (user) setIs2faEnabled(user.twoFactorEnabled || false);
    }, [user?.twoFactorEnabled]);

    useEffect(() => {
        fetchConnections();
        fetchBudgets();
    }, []);

    const fetchConnections = async () => {
        setLoadingConnections(true);
        try {
            const res = await plaidService.getConnections();
            const data = res.data?.data ?? res.data ?? [];
            setConnections(Array.isArray(data) ? data : []);
        } catch { /* user may not have linked yet */ } finally {
            setLoadingConnections(false);
        }
    };

    const fetchBudgets = async () => {
        setLoadingBudgets(true);
        try {
            const res = await budgetService.getAll();
            const data = (res.data as any)?.data ?? res.data ?? [];
            setBudgets(Array.isArray(data) ? data : []);
        } catch { /* ignore */ } finally {
            setLoadingBudgets(false);
        }
    };

    const handleSync = async () => {
        setSyncing(true);
        try {
            await plaidService.syncTransactions();
            toast.success('Transactions synced successfully');
        } catch { toast.error('Sync failed'); } finally { setSyncing(false); }
    };

    const handlePlaidSuccess = () => {
        toast.success('Bank connected! Syncing your transactions...');
        handleSync();
        fetchConnections();
    };

    const handleSetup2FA = async () => {
        setLoading2fa(true);
        try {
            const res = await authService.setup2FA();
            setQrCode(res.data.qrCodeUri);
            setSecret(res.data.secret);
            setShowing2faSetup(true);
        } catch { toast.error('Failed to setup 2FA'); } finally { setLoading2fa(false); }
    };

    const handleToggle2FA = async (enable: boolean) => {
        setLoading2fa(true);
        try {
            await authService.toggle2FA(enable);
            setIs2faEnabled(enable);
            if (!enable) setShowing2faSetup(false);
        } catch { toast.error('Failed to toggle 2FA'); } finally { setLoading2fa(false); }
    };

    const handleCreateBudget = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newCategory || !newAmount) return;
        setSavingBudget(true);
        try {
            await budgetService.create({ category: newCategory, limitAmount: parseFloat(newAmount), period: newPeriod });
            setNewCategory(''); setNewAmount(''); setNewPeriod('WEEKLY');
            await fetchBudgets();
            toast.success('Spending limit saved');
        } catch { toast.error('Failed to save spending limit'); } finally { setSavingBudget(false); }
    };

    const handleDeleteBudget = async (id: string) => {
        try {
            await budgetService.remove(id);
            setBudgets(prev => prev.filter(b => b.id !== id));
            toast.success('Limit removed');
        } catch { toast.error('Failed to remove limit'); }
    };

    const inputClass = "w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-xl text-slate-200 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 transition-all placeholder:text-slate-600";

    return (
        <div className="p-8 max-w-4xl mx-auto space-y-8 pb-20">
            <div>
                <h1 className="text-3xl font-black text-slate-100 tracking-tight">Settings</h1>
                <p className="text-slate-500 mt-1 font-medium">Manage your security and platform preferences.</p>
            </div>

            <div className="space-y-6">
                {/* Security */}
                <Card title="Security & Authentication" subtitle="Protect your account with advanced verification">
                    <div className="mt-6 space-y-6">
                        <div className="flex items-center justify-between p-4 bg-white/5 rounded-2xl border border-white/10">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-white/5 rounded-xl">
                                    <ShieldCheck className={is2faEnabled ? 'text-emerald-400 w-6 h-6' : 'text-slate-400 w-6 h-6'} />
                                </div>
                                <div>
                                    <h4 className="font-bold text-slate-200 flex items-center gap-2">
                                        Two-Factor Authentication (TOTP)
                                        {is2faEnabled && <span className="text-[10px] bg-emerald-500/15 text-emerald-400 px-2 py-0.5 rounded-full">Active</span>}
                                    </h4>
                                    <p className="text-xs text-slate-500">Extra security using Google Authenticator or Authy.</p>
                                </div>
                            </div>
                            {is2faEnabled ? (
                                <button onClick={() => handleToggle2FA(false)} className="text-xs font-bold text-rose-400 hover:text-rose-300 transition-colors">Disable</button>
                            ) : (
                                <button onClick={handleSetup2FA} disabled={loading2fa}
                                    className="bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] px-5 py-2 rounded-xl font-bold text-xs shadow-gold hover:scale-105 transition-all disabled:opacity-50">
                                    {loading2fa ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Setup 2FA'}
                                </button>
                            )}
                        </div>

                        {showing2faSetup && !is2faEnabled && (
                            <div className="p-6 bg-[#D4AF37]/5 border border-[#D4AF37]/20 rounded-2xl">
                                <div className="grid md:grid-cols-2 gap-8 items-center">
                                    <div className="space-y-4">
                                        <div className="flex items-center gap-2 text-[#D4AF37] font-bold">
                                            <QrCode className="w-5 h-5" /><span>Scan QR Code</span>
                                        </div>
                                        <p className="text-xs text-slate-400 leading-relaxed">Scan with your authenticator app, or use the manual secret:</p>
                                        <div className="p-3 bg-white/5 border border-white/10 rounded-lg font-mono text-xs break-all text-[#D4AF37] select-all cursor-pointer">{secret}</div>
                                        <button onClick={() => handleToggle2FA(true)}
                                            className="w-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-3 rounded-xl font-bold text-sm shadow-gold hover:scale-[1.02] transition-all">
                                            I've scanned it — Enable 2FA
                                        </button>
                                    </div>
                                    <div className="flex justify-center">
                                        {qrCode ? (
                                            <div className="p-4 bg-white rounded-2xl shadow-gold"><img src={qrCode} alt="QR Code" className="w-40 h-40" /></div>
                                        ) : (
                                            <div className="w-40 h-40 bg-white/5 rounded-xl flex items-center justify-center"><Loader2 className="w-6 h-6 text-slate-500 animate-spin" /></div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </Card>

                {/* Integrations */}
                <Card title="Integrations" subtitle="Connected financial services">
                    <div className="mt-4">
                        <div className="flex flex-col md:flex-row items-start md:items-center justify-between p-4 bg-white/5 rounded-2xl border border-white/10 gap-4">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-[#D4AF37]/15 rounded-xl"><Zap className="text-[#D4AF37] w-6 h-6" /></div>
                                <div>
                                    <h4 className="font-bold text-slate-200">Plaid Bank Sync</h4>
                                    <p className="text-xs text-slate-500">Securely connect to your financial institutions.</p>
                                </div>
                            </div>
                            <PlaidLink onSuccess={handlePlaidSuccess} />
                        </div>
                    </div>
                </Card>

                {/* Connected Accounts */}
                <Card title="Connected Accounts" subtitle="Your linked bank and credit accounts">
                    <div className="mt-4 space-y-3">
                        {loadingConnections ? (
                            <div className="flex items-center justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-[#D4AF37]" /></div>
                        ) : connections.length === 0 ? (
                            <div className="text-center py-8">
                                <Building2 className="w-10 h-10 text-slate-600 mx-auto mb-3" />
                                <p className="text-slate-500 text-sm font-medium">No accounts connected yet.</p>
                                <p className="text-slate-600 text-xs mt-1">Use Plaid Bank Sync above to link your bank.</p>
                            </div>
                        ) : (
                            connections.map(conn => <ConnectedAccountRow key={conn.id} conn={conn} />)
                        )}
                    </div>
                </Card>

                {/* Spending Limits */}
                <Card title="Spending Limits" subtitle="Set category budgets — get notified the moment you cross them">
                    <div className="mt-4">
                        <div className="p-3 bg-[#D4AF37]/5 border border-[#D4AF37]/20 rounded-xl mb-5 flex items-start gap-3">
                            <AlertTriangle className="w-4 h-4 text-[#D4AF37] flex-shrink-0 mt-0.5" />
                            <p className="text-xs text-slate-400 leading-relaxed">
                                Example: Food $30/week, Gas $30/week, Rent $1500/month. Alerts fire via bell notification + email the moment you exceed a limit.
                            </p>
                        </div>

                        <form onSubmit={handleCreateBudget} className="grid grid-cols-1 sm:grid-cols-4 gap-3 items-end mb-6">
                            <div>
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-wider mb-1.5 block">Category</label>
                                <select value={newCategory} onChange={e => setNewCategory(e.target.value)}
                                    className={inputClass} required
                                    style={{ backgroundColor: 'rgba(30,27,75,0.8)' }}>
                                    <option value="">Select...</option>
                                    {CATEGORY_OPTIONS.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-wider mb-1.5 block">Limit ($)</label>
                                <input type="number" min="1" step="0.01" placeholder="e.g. 30"
                                    value={newAmount} onChange={e => setNewAmount(e.target.value)}
                                    className={inputClass} required />
                            </div>
                            <div>
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-wider mb-1.5 block">Period</label>
                                <select value={newPeriod} onChange={e => setNewPeriod(e.target.value as BudgetPeriod)}
                                    className={inputClass} style={{ backgroundColor: 'rgba(30,27,75,0.8)' }}>
                                    <option value="WEEKLY">Weekly</option>
                                    <option value="MONTHLY">Monthly</option>
                                </select>
                            </div>
                            <button type="submit" disabled={savingBudget || !newCategory || !newAmount}
                                className="flex items-center justify-center gap-2 bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-2.5 rounded-xl font-bold text-sm shadow-gold hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                                {savingBudget ? <Loader2 className="w-4 h-4 animate-spin" /> : <><Plus className="w-4 h-4" /> Add Limit</>}
                            </button>
                        </form>

                        <div className="space-y-3">
                            {loadingBudgets ? (
                                [1, 2].map(i => <div key={i} className="h-16 bg-white/5 rounded-2xl animate-pulse" />)
                            ) : budgets.length === 0 ? (
                                <p className="text-slate-500 text-sm text-center py-4">No spending limits set yet.</p>
                            ) : (
                                budgets.map(b => <BudgetLimitRow key={b.id} budget={b} onDelete={() => handleDeleteBudget(b.id)} />)
                            )}
                        </div>
                    </div>
                </Card>

                {/* Session Management */}
                <Card title="Session Management" subtitle="Manage your current active login sessions">
                    <div className="mt-4 p-4 border border-white/10 rounded-2xl flex items-center justify-between bg-white/5">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-emerald-500/15 flex items-center justify-center">
                                <Shield className="w-5 h-5 text-emerald-400" />
                            </div>
                            <div>
                                <p className="text-sm font-bold text-slate-200">Current Session</p>
                                <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">This Browser · Active Now</p>
                            </div>
                        </div>
                        <button className="text-xs font-bold text-slate-500 hover:text-rose-400 transition-colors">Terminate</button>
                    </div>
                </Card>
            </div>
        </div>
    );
}
