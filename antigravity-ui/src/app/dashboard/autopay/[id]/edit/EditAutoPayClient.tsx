'use client';

import React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { ArrowLeft, Save } from 'lucide-react';
import autoPayService, {
    type AutoPaySchedule,
    type UpdateAutoPayRequest,
    type PaymentFrequency,
    FREQUENCY_LABELS,
} from '@/services/autoPayService';
import { CategoryIcon } from '@/components/autopay/CategoryIcon';

export default function EditAutoPayClient() {
    const { id } = useParams<{ id: string }>();
    const router = useRouter();
    const [schedule, setSchedule] = React.useState<AutoPaySchedule | null>(null);
    const [form, setForm] = React.useState<UpdateAutoPayRequest>({});
    const [saving, setSaving] = React.useState(false);
    const [loading, setLoading] = React.useState(true);

    React.useEffect(() => {
        autoPayService.getSchedule(id).then((res) => {
            const s = res.data;
            setSchedule(s);
            setForm({
                paymentName: s.paymentName,
                paymentProvider: s.paymentProvider ?? undefined,
                amount: s.amount,
                currency: s.currency,
                frequency: s.frequency,
                nextDueDate: s.nextDueDate,
                dueDayOfMonth: s.dueDayOfMonth ?? undefined,
                autoExecute: s.autoExecute,
                reminderDaysBefore: s.reminderDaysBefore,
            });
            setLoading(false);
        }).catch(() => {
            toast.error('Schedule not found');
            router.push('/dashboard/autopay');
        });
    }, [id]);

    const update = (patch: Partial<UpdateAutoPayRequest>) =>
        setForm((prev) => ({ ...prev, ...patch }));

    const handleSave = async () => {
        setSaving(true);
        try {
            await autoPayService.updateSchedule(id, form);
            toast.success('Schedule updated');
            router.push(`/dashboard/autopay/${id}`);
        } catch {
            toast.error('Failed to update schedule');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return (
        <div className="p-6 max-w-2xl mx-auto space-y-4">
            <div className="h-8 w-48 rounded-xl bg-white/5 animate-pulse" />
            <div className="h-80 rounded-2xl bg-white/5 animate-pulse" />
        </div>
    );

    return (
        <div className="p-6 max-w-2xl mx-auto space-y-6">
            <button
                onClick={() => router.push(`/dashboard/autopay/${id}`)}
                className="flex items-center gap-2 text-slate-400 hover:text-white text-sm"
            >
                <ArrowLeft size={16} /> Back to Schedule
            </button>

            <div className="flex items-center gap-3">
                {schedule && <CategoryIcon category={schedule.paymentCategory} size={20} />}
                <div>
                    <h1 className="text-xl font-bold text-white">Edit Schedule</h1>
                    <p className="text-slate-400 text-sm">{schedule?.paymentName}</p>
                </div>
            </div>

            <div className="bg-white/5 border border-white/10 rounded-2xl p-6 space-y-4">
                {[
                    { label: 'Payment Name', key: 'paymentName', type: 'text', max: 255 },
                    { label: 'Provider', key: 'paymentProvider', type: 'text', max: 255 },
                ].map(({ label, key, type, max }) => (
                    <div key={key}>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">{label}</label>
                        <input
                            type={type}
                            maxLength={max}
                            value={(form as any)[key] ?? ''}
                            onChange={(e) => update({ [key]: e.target.value })}
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        />
                    </div>
                ))}

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">Amount</label>
                        <input
                            type="number" min="0.01" step="0.01"
                            value={form.amount ?? ''}
                            onChange={(e) => update({ amount: parseFloat(e.target.value) })}
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">Frequency</label>
                        <select
                            value={form.frequency ?? ''}
                            onChange={(e) => update({ frequency: e.target.value as PaymentFrequency })}
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        >
                            {(Object.keys(FREQUENCY_LABELS) as PaymentFrequency[]).map((f) => (
                                <option key={f} value={f}>{FREQUENCY_LABELS[f]}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">Next Due Date</label>
                        <input
                            type="date"
                            value={form.nextDueDate ?? ''}
                            onChange={(e) => update({ nextDueDate: e.target.value })}
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60 [color-scheme:dark]"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-400 mb-1.5">Reminder (days before)</label>
                        <select
                            value={form.reminderDaysBefore ?? 3}
                            onChange={(e) => update({ reminderDaysBefore: parseInt(e.target.value) })}
                            className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-violet-500/60"
                        >
                            {[1, 2, 3, 5, 7, 10, 14].map((d) => (
                                <option key={d} value={d}>{d} days</option>
                            ))}
                        </select>
                    </div>
                </div>
            </div>

            <div className="flex justify-end gap-3">
                <button
                    onClick={() => router.push(`/dashboard/autopay/${id}`)}
                    className="px-4 py-2.5 text-sm text-slate-400 hover:text-white"
                >
                    Cancel
                </button>
                <button
                    onClick={handleSave}
                    disabled={saving}
                    className="flex items-center gap-2 px-5 py-2.5 bg-violet-600 hover:bg-violet-500 text-white text-sm font-medium rounded-xl disabled:opacity-50 transition-all"
                >
                    {saving ? (
                        <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                        <Save size={15} />
                    )}
                    Save Changes
                </button>
            </div>
        </div>
    );
}
