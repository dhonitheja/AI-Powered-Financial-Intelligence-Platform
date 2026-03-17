import type { Metadata } from 'next';
import { AutoPayDashboard } from '@/components/autopay/AutoPayDashboard';

export const metadata: Metadata = {
    title: 'AutoPay Hub | Antigravity Finance',
    description:
        'Manage all your recurring payments, EMIs, insurance premiums, and subscriptions in one place.',
};

export default function AutoPayPage() {
    return (
        <div className="p-6 max-w-7xl mx-auto">
            <AutoPayDashboard />
        </div>
    );
}
