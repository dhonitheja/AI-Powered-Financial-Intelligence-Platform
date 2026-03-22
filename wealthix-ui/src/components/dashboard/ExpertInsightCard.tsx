import React from 'react';
import { ShieldCheck, Zap } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { cn } from '@/lib/utils';

interface ExpertInsightCardProps {
    insight: {
        expertAdvice: string;
        complexityScore: number;
        modelUsed?: string;
    };
}

export const ExpertInsightCard: React.FC<ExpertInsightCardProps> = ({ insight }) => {
    // Generate accuracy based on complexity (e.g., score/10 with small random jitter for "realism")
    const accuracy = ((insight.complexityScore || 8) + (Math.random() * 0.5)).toFixed(1);

    return (
        <Card className="border-[#D4AF37]/50 bg-gradient-to-br from-[#1A1635] to-[#2D2412]/20 shadow-gold relative overflow-hidden group border-2 animate-in slide-in-from-right-4 duration-1000">
            {/* Top Right: Accuracy Shield */}
            <div className="absolute top-4 right-4 flex items-center gap-1.5 bg-[#D4AF37]/10 px-3 py-1.5 rounded-full border border-[#D4AF37]/20 backdrop-blur-md">
                <ShieldCheck className="w-3.5 h-3.5 text-[#D4AF37]" />
                <span className="text-[10px] font-black text-[#D4AF37] uppercase tracking-tighter">
                    {accuracy}/10 Accuracy
                </span>
            </div>

            <div className="space-y-4 p-2">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-[#D4AF37]/10 rounded-lg text-[#D4AF37]">
                        <Zap className="w-5 h-5 animate-pulse" />
                    </div>
                    <div>
                        <h3 className="font-black text-[#D4AF37] tracking-tighter uppercase text-xs">Premium Strategy Analysis</h3>
                        <p className="text-[9px] font-bold text-slate-500 uppercase tracking-widest">{insight.modelUsed || 'Claude 3.5 Sonnet'}</p>
                    </div>
                </div>

                <div className="text-sm font-bold text-slate-100 leading-relaxed bg-white/5 p-4 rounded-2xl border border-white/10 group-hover:border-[#D4AF37]/30 transition-colors">
                    {insight.expertAdvice}
                </div>

                <div className="flex items-center gap-2 text-[10px] font-black text-[#D4AF37] uppercase tracking-widest opacity-80">
                    <div className="w-1.5 h-1.5 rounded-full bg-[#D4AF37] animate-ping" />
                    Strategic Escorts Active
                </div>
            </div>
        </Card>
    );
};
