import type { Metadata } from 'next';
import { AddAutoPayForm } from '@/components/autopay/AddAutoPayForm';

export const metadata: Metadata = {
    title: 'Add Autopay Schedule | AutoPay Hub',
    description: 'Create a new recurring payment schedule with encrypted account management.',
};

export default function NewAutoPayPage() {
    return (
        <div className="p-6 max-w-4xl mx-auto">
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-white">New Autopay Schedule</h1>
                <p className="text-slate-400 text-sm mt-1">
                    Set up a recurring payment in a few simple steps
                </p>
            </div>
            <AddAutoPayForm />
        </div>
    );
}
