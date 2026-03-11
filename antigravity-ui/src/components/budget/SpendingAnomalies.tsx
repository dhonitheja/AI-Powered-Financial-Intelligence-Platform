import React, { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { chatService } from '@/services/api';
import { AlertTriangle, CheckCircle, ShieldCheck, Activity } from 'lucide-react';
import clsx from 'clsx';

export function SpendingAnomalies() {
    const [anomalies, setAnomalies] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    const loadAnomalies = async () => {
        setLoading(true);
        try {
            const res = await chatService.getAnomalies();
            setAnomalies(res.data?.data || res.data);
        } catch (e) {
            console.error("Failed to fetch anomalies", e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAnomalies();
    }, []);

    const acknowledge = async (id: string) => {
        try {
            await chatService.acknowledgeAnomaly(id);
            // Ignore response and just reload to reflect state
            loadAnomalies();
        } catch (e) {
            console.error("Acknowledge failed", e);
        }
    };

    if (loading) {
        return (
            <Card className="animate-pulse flex items-center justify-center p-8 h-64">
                <p className="text-slate-400 font-medium flex items-center gap-2">
                    <Activity className="w-5 h-5 animate-spin" /> Analyzing spending patterns...
                </p>
            </Card>
        );
    }

    if (!anomalies || !anomalies.anomalies || anomalies.anomalies.length === 0) {
        return (
            <Card className="p-8 text-center flex flex-col items-center justify-center h-64 bg-emerald-50/50 border-emerald-100">
                <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mb-4">
                    <ShieldCheck className="w-8 h-8 text-emerald-600" />
                </div>
                <h3 className="text-lg font-bold text-emerald-900">No Anomalies Detected</h3>
                <p className="text-sm text-emerald-700 mt-2">
                    Your spending patterns look normal based on your historical data.
                </p>
            </Card>
        );
    }

    return (
        <Card>
            <CardHeader className="border-b border-slate-100 bg-slate-50 rounded-t-xl">
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="text-lg flex items-center gap-2">
                            <AlertTriangle className={clsx("w-5 h-5", anomalies.risk_level === "HIGH" ? "text-rose-500" : "text-amber-500")} />
                            Recent Spending Anomalies
                        </CardTitle>
                        <p className="text-xs text-slate-500 mt-1">
                            Unusual activities detected in your recent transactions
                        </p>
                    </div>
                    <div className={clsx(
                        "px-3 py-1 rounded-full text-xs font-bold",
                        anomalies.risk_level === "HIGH" ? "bg-rose-100 text-rose-700" : "bg-amber-100 text-amber-700"
                    )}>
                        {anomalies.risk_level} RISK
                    </div>
                </div>
            </CardHeader>
            <CardContent className="p-0 divide-y divide-slate-100">
                {anomalies.anomalies.map((anomaly: any, idx: number) => (
                    <div key={idx} className="p-5 flex items-start gap-4 hover:bg-slate-50 transition-colors">
                        <div className={clsx(
                            "w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 mt-1",
                            anomaly.severity === "HIGH" ? "bg-rose-100 text-rose-600" : "bg-amber-100 text-amber-600"
                        )}>
                            <AlertTriangle className="w-5 h-5" />
                        </div>
                        <div className="flex-1">
                            <h4 className="font-bold text-slate-800 flex items-center gap-2">
                                {anomaly.type.replace('_', ' ')}
                                <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px] uppercase font-bold tracking-wider">
                                    {anomaly.category}
                                </span>
                            </h4>
                            <p className="text-sm text-slate-600 mt-1 leading-relaxed">
                                {anomaly.description}
                            </p>
                            
                            {/* Wait, the Python API doesn't return anomaly IDs for these transient suggestions, it only returns a list.
                                We'll just show them as general alerts since they are calculated transiently. */}
                        </div>
                    </div>
                ))}
            </CardContent>
        </Card>
    );
}
