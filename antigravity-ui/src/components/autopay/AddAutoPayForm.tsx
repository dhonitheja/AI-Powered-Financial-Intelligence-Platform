'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { Check, ChevronLeft, ChevronRight, Lock } from 'lucide-react';
import autoPayService, {
    type CreateAutoPayRequest,
    type PaymentCategory,
    type PaymentFrequency,
    type CategoryMetadata,
    FREQUENCY_LABELS,
} from '@/services/autoPayService';
import { CategoryIcon } from './CategoryIcon';

// ── Step indicator ────────────────────────────────────────────────────────────
const STEPS = ['Category', 'Details', 'Schedule', 'Review'];

function StepIndicator({ current }: { current: number }) {
    return (
        <div className="flex items-center gap-0.5 mb-8">
            {STEPS.map((label, i) => (
                <React.Fragment key={label}>
                    <div className="flex flex-col items-center gap-1">
                        <div
                            className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all ${i < current
                                ? 'bg-violet-500 text-white'
                                : i === current
                                    ? 'bg-violet-600 text-white ring-2 ring-violet-400/30'
                                    : 'bg-white/10 text-slate-500'
                                }`}
                        >
                            {i < current ? <Check size={14} /> : i + 1}
                        </div>
                        <span
                            className={`text-xs ${i === current ? 'text-violet-400' : 'text-slate-500'}`}
                        >
                            {label}
                        </span>
                    </div>
                    {i < STEPS.length - 1 && (
                        <div
                            className={`flex-1 h-0.5 mb-4 mx-1 transition-all ${i < current ? 'bg-violet-500' : 'bg-white/10'
                                }`}
                        />
                    )}
                </React.Fragment>
            ))}
        </div>
    );
}

// ── Step 1: Category selection ────────────────────────────────────────────────
function CategoryStep({
    categories,
    selected,
    onSelect,
}: {
    categories: CategoryMetadata[];
    selected: PaymentCategory | null;
    onSelect: (cat: PaymentCategory) => void;
}) {
    return (
        <div>
            <h2 className="text-lg font-bold text-white mb-1">Select Payment Type</h2>
            <p className="text-slate-400 text-sm mb-6">Choose the category that best describes this payment</p>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {categories.map((cat) => (
                    <button
                        key={cat.value}
                        onClick={() => onSelect(cat.value)}
                        className={`flex flex-col items-center gap-2.5 p-4 rounded-2xl border transition-all text-center ${selected === cat.value
                            ? 'border-violet-500 bg-violet-500/15 shadow-lg shadow-violet-500/10'
                            : 'border-white/10 bg-white/5 hover:bg-white/8 hover:border-white/20'
                            }`}
                    >
                        <CategoryIcon category={cat.value} size={20} />
                        <span className="text-xs font-medium text-slate-200 leading-tight">
                            {cat.displayName}
                        </span>
                    </button>
                ))}
            </div>
        </div>
    );
}

// ── Step 2: Payment details ───────────────────────────────────────────────────
function DetailsStep({
    form,
    onChange,
}: {
    form: Partial<CreateAutoPayRequest>;
    onChange: (updates: Partial<CreateAutoPayRequest>) => void;
}) {
    return (
        <div className="space-y-5">
            <div>
                <h2 className="text-lg font-bold text-white mb-1">Payment Details</h2>
                <p className="text-slate-400 text-sm mb-6">
                    Enter the payment information. Sensitive fields are encrypted before saving.
                </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">
                        Payment Name *
                    </label>
                    <input
                        type="text"
                        required
                        maxLength={255}
                        value={form.paymentName ?? ''}
                        onChange={(e) => onChange({ paymentName: e.target.value })}
                        placeholder="e.g., Chase Mortgage, Netflix, HDFC Home Loan"
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm placeholder-slate-500 focus:outline-none focus:border-violet-500/60"
                    />
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">Amount *</label>
                    <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">$</span>
                        <input
                            type="number"
                            required
                            min="0.01"
                            step="0.01"
                            value={form.amount ?? ''}
                            onChange={(e) => onChange({ amount: parseFloat(e.target.value) || undefined as any })}
                            placeholder="0.00"
                            className="w-full pl-7 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        />
                    </div>
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">Currency</label>
                    <select
                        value={form.currency ?? 'USD'}
                        onChange={(e) => onChange({ currency: e.target.value })}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                    >
                        <option value="USD">USD — US Dollar</option>
                        <option value="INR">INR — Indian Rupee</option>
                        <option value="EUR">EUR — Euro</option>
                        <option value="GBP">GBP — British Pound</option>
                        <option value="CAD">CAD — Canadian Dollar</option>
                    </select>
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">Provider / Lender</label>
                    <input
                        type="text"
                        maxLength={255}
                        value={form.paymentProvider ?? ''}
                        onChange={(e) => onChange({ paymentProvider: e.target.value })}
                        placeholder="e.g., Chase, HDFC, Geico"
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm placeholder-slate-500 focus:outline-none focus:border-violet-500/60"
                    />
                </div>

                {/* Encrypted fields section */}
                <div className="sm:col-span-2 pt-2">
                    <div className="flex items-center gap-2 mb-3">
                        <Lock size={13} className="text-emerald-400" />
                        <p className="text-xs text-emerald-400 font-medium">
                            Account details — encrypted with AES-256-GCM before saving
                        </p>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-medium text-slate-400 mb-1.5">
                                Account Number (optional)
                            </label>
                            <input
                                type="password"
                                maxLength={17}
                                autoComplete="off"
                                value={form.accountNumber ?? ''}
                                onChange={(e) => onChange({ accountNumber: e.target.value.replace(/\D/g, '') })}
                                placeholder="••••••••"
                                className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-emerald-500/60 font-mono"
                            />
                            <p className="text-xs text-slate-600 mt-1">Stored encrypted, shown as ****XXXX</p>
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-slate-400 mb-1.5">
                                Routing Number (optional)
                            </label>
                            <input
                                type="password"
                                maxLength={9}
                                autoComplete="off"
                                value={form.routingNumber ?? ''}
                                onChange={(e) => onChange({ routingNumber: e.target.value.replace(/\D/g, '') })}
                                placeholder="••••••••"
                                className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-emerald-500/60 font-mono"
                            />
                        </div>
                    </div>
                </div>

                <div className="sm:col-span-2">
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">
                        Notes (optional, encrypted)
                    </label>
                    <textarea
                        maxLength={1000}
                        rows={2}
                        value={form.notes ?? ''}
                        onChange={(e) => onChange({ notes: e.target.value })}
                        placeholder="Any additional notes about this payment…"
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm placeholder-slate-500 focus:outline-none focus:border-violet-500/60 resize-none"
                    />
                </div>
            </div>
        </div>
    );
}

// ── Step 3: Schedule & reminders ──────────────────────────────────────────────
function ScheduleStep({
    form,
    onChange,
}: {
    form: Partial<CreateAutoPayRequest>;
    onChange: (updates: Partial<CreateAutoPayRequest>) => void;
}) {
    return (
        <div className="space-y-5">
            <div>
                <h2 className="text-lg font-bold text-white mb-1">Schedule & Reminders</h2>
                <p className="text-slate-400 text-sm mb-6">Configure when and how often this payment occurs</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">Frequency *</label>
                    <select
                        required
                        value={form.frequency ?? ''}
                        onChange={(e) => onChange({ frequency: e.target.value as PaymentFrequency })}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                    >
                        <option value="" disabled>Select frequency</option>
                        {(Object.keys(FREQUENCY_LABELS) as PaymentFrequency[]).map((f) => (
                            <option key={f} value={f}>{FREQUENCY_LABELS[f]}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">Next Due Date *</label>
                    <input
                        type="date"
                        required
                        min={new Date().toISOString().split('T')[0]}
                        value={form.nextDueDate ?? ''}
                        onChange={(e) => onChange({ nextDueDate: e.target.value })}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60 [color-scheme:dark]"
                    />
                </div>

                {form.frequency === 'MONTHLY' && (
                    <div>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">
                            Day of Month (1–31)
                        </label>
                        <input
                            type="number"
                            min={1}
                            max={31}
                            value={form.dueDayOfMonth ?? ''}
                            onChange={(e) => onChange({ dueDayOfMonth: parseInt(e.target.value) || undefined as any })}
                            placeholder="e.g., 15"
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        />
                    </div>
                )}

                <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1.5">
                        Remind me (days before due)
                    </label>
                    <select
                        value={form.reminderDaysBefore ?? 3}
                        onChange={(e) => onChange({ reminderDaysBefore: parseInt(e.target.value) })}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                    >
                        {[1, 2, 3, 5, 7, 10, 14].map((d) => (
                            <option key={d} value={d}>{d} {d === 1 ? 'day' : 'days'} before</option>
                        ))}
                    </select>
                </div>

                <div className="sm:col-span-2">
                    <label className="flex items-center gap-3 cursor-pointer">
                        <div
                            onClick={() => onChange({ autoExecute: !form.autoExecute })}
                            className={`relative w-11 h-6 rounded-full transition-colors ${form.autoExecute ? 'bg-violet-600' : 'bg-white/10'
                                }`}
                        >
                            <div
                                className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform ${form.autoExecute ? 'translate-x-5' : ''
                                    }`}
                            />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-white">Auto-execute payments</p>
                            <p className="text-xs text-slate-500">
                                Automatically process payments on due date (requires bank connection)
                            </p>
                        </div>
                    </label>
                </div>
            </div>
        </div>
    );
}

// ── Step 4: Review ────────────────────────────────────────────────────────────
function ReviewStep({
    form,
    categories,
}: {
    form: Partial<CreateAutoPayRequest>;
    categories: CategoryMetadata[];
}) {
    const cat = categories.find((c) => c.value === form.paymentCategory);

    const rows: { label: string; value: string }[] = [
        { label: 'Payment Name', value: form.paymentName ?? '—' },
        { label: 'Category', value: cat?.displayName ?? form.paymentCategory ?? '—' },
        { label: 'Provider', value: form.paymentProvider || '—' },
        { label: 'Amount', value: form.amount ? `${form.currency ?? 'USD'} ${form.amount.toFixed(2)}` : '—' },
        { label: 'Frequency', value: FREQUENCY_LABELS[form.frequency!] ?? '—' },
        { label: 'Next Due Date', value: form.nextDueDate ?? '—' },
        { label: 'Remind', value: `${form.reminderDaysBefore ?? 3} days before` },
        { label: 'Account Number', value: form.accountNumber ? '****' + form.accountNumber.slice(-4) : '—' },
        { label: 'Auto-Execute', value: form.autoExecute ? 'Enabled' : 'Disabled' },
    ];

    return (
        <div>
            <h2 className="text-lg font-bold text-white mb-1">Review & Confirm</h2>
            <p className="text-slate-400 text-sm mb-6">
                Please review your autopay schedule before saving
            </p>

            <div className="bg-white/5 border border-white/10 rounded-2xl overflow-hidden">
                {rows.map(({ label, value }, i) => (
                    <div
                        key={label}
                        className={`flex justify-between items-center px-5 py-3 text-sm ${i < rows.length - 1 ? 'border-b border-white/5' : ''
                            }`}
                    >
                        <span className="text-slate-400">{label}</span>
                        <span className="text-white font-medium">{value}</span>
                    </div>
                ))}
            </div>

            <div className="mt-4 flex items-center gap-2 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
                <Lock size={14} className="text-emerald-400 flex-shrink-0" />
                <p className="text-xs text-emerald-400">
                    Account and routing numbers will be encrypted with AES-256-GCM before storage.
                    Only the last 4 digits will be visible.
                </p>
            </div>
        </div>
    );
}

// ── Main form ─────────────────────────────────────────────────────────────────
export function AddAutoPayForm() {
    const router = useRouter();
    const [step, setStep] = React.useState(0);
    const [form, setForm] = React.useState<Partial<CreateAutoPayRequest>>({
        currency: 'USD',
        reminderDaysBefore: 3,
        autoExecute: false,
    });
    const [categories, setCategories] = React.useState<CategoryMetadata[]>([]);
    const [submitting, setSubmitting] = React.useState(false);

    React.useEffect(() => {
        autoPayService.getCategories().then((res) => setCategories(res.data));
    }, []);

    const updateForm = (updates: Partial<CreateAutoPayRequest>) =>
        setForm((prev) => ({ ...prev, ...updates }));

    const canProceed = (): boolean => {
        if (step === 0) return !!form.paymentCategory;
        if (step === 1) return !!(form.paymentName && form.amount && form.amount > 0);
        if (step === 2) return !!(form.frequency && form.nextDueDate);
        return true;
    };

    const handleSubmit = async () => {
        if (!form.paymentCategory || !form.paymentName || !form.amount || !form.frequency || !form.nextDueDate) {
            toast.error('Please fill in all required fields');
            return;
        }

        setSubmitting(true);
        try {
            await autoPayService.createSchedule(form as CreateAutoPayRequest);
            toast.success('AutoPay schedule created!');
            router.push('/dashboard/autopay');
        } catch (err: any) {
            toast.error(err?.response?.data?.message ?? 'Failed to create schedule');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="max-w-2xl mx-auto">
            <StepIndicator current={step} />

            <div className="bg-white/5 border border-white/10 rounded-2xl p-6 min-h-[420px]">
                {step === 0 && (
                    <CategoryStep
                        categories={categories}
                        selected={form.paymentCategory ?? null}
                        onSelect={(cat) => updateForm({ paymentCategory: cat })}
                    />
                )}
                {step === 1 && <DetailsStep form={form} onChange={updateForm} />}
                {step === 2 && <ScheduleStep form={form} onChange={updateForm} />}
                {step === 3 && <ReviewStep form={form} categories={categories} />}
            </div>

            {/* Navigation */}
            <div className="flex items-center justify-between mt-6">
                <button
                    type="button"
                    onClick={() => step === 0 ? router.back() : setStep((s) => s - 1)}
                    className="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-400 hover:text-white transition-colors"
                >
                    <ChevronLeft size={16} />
                    {step === 0 ? 'Cancel' : 'Back'}
                </button>

                {step < STEPS.length - 1 ? (
                    <button
                        type="button"
                        disabled={!canProceed()}
                        onClick={() => setStep((s) => s + 1)}
                        className="flex items-center gap-2 px-5 py-2.5 bg-violet-600 text-white text-sm font-medium rounded-xl disabled:opacity-40 disabled:cursor-not-allowed hover:bg-violet-500 transition-all shadow-lg shadow-violet-500/20"
                    >
                        Continue
                        <ChevronRight size={16} />
                    </button>
                ) : (
                    <button
                        type="button"
                        disabled={submitting}
                        onClick={handleSubmit}
                        className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 text-white text-sm font-medium rounded-xl disabled:opacity-40 hover:bg-emerald-500 transition-all shadow-lg shadow-emerald-500/20"
                    >
                        {submitting ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Saving…
                            </>
                        ) : (
                            <>
                                <Check size={16} />
                                Save Schedule
                            </>
                        )}
                    </button>
                )}
            </div>
        </div>
    );
}
