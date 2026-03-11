import React, { useEffect, useState } from 'react';
import { Card, CardContent } from '@/components/ui/Card';
import { Trophy, Star, Shield, Award, ChevronRight } from 'lucide-react';
import { gamificationService } from '@/services/api';
import clsx from 'clsx';

export function GamificationCard() {
    const [profile, setProfile] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadProfile = async () => {
            try {
                const res = await gamificationService.getProfile();
                setProfile(res.data?.data || res.data);
            } catch (e) {
                console.error("Failed to fetch gamification profile", e);
            } finally {
                setLoading(false);
            }
        };
        loadProfile();
    }, []);

    if (loading) {
        return (
            <Card className="animate-pulse h-32 flex items-center justify-center">
                <div className="flex gap-2 text-slate-400">
                    <Trophy className="w-5 h-5 animate-pulse" /> Loading Level...
                </div>
            </Card>
        );
    }

    if (!profile) return null;

    const progress = Math.min(((profile.points % 100) / 100) * 100, 100);

    const getTierColor = (tier: string) => {
        switch (tier) {
            case 'BRONZE': return 'from-orange-800 to-amber-600 border-amber-800/20';
            case 'SILVER': return 'from-slate-500 to-slate-400 border-slate-500/20';
            case 'GOLD': return 'from-yellow-600 to-amber-500 border-yellow-500/20';
            case 'PLATINUM': return 'from-cyan-600 to-blue-500 border-blue-500/20';
            case 'DIAMOND': return 'from-indigo-600 to-purple-500 border-purple-500/20';
            default: return 'from-slate-700 to-slate-500 border-slate-700/20';
        }
    };

    return (
        <Card className={clsx(
            "relative overflow-hidden border-2 transition-all",
            profile.tier === 'GOLD' ? 'border-yellow-500/30 shadow-yellow-500/10' :
            profile.tier === 'PLATINUM' ? 'border-blue-500/30 shadow-blue-500/10' :
            profile.tier === 'DIAMOND' ? 'border-purple-500/30 flex shadow-purple-500/10' :
            'border-slate-200'
        )}>
            {/* Background Glow */}
            <div className={clsx(
                "absolute -top-24 -right-24 w-48 h-48 rounded-full blur-3xl opacity-20 pointer-events-none",
                profile.tier === 'GOLD' ? 'bg-yellow-500' :
                profile.tier === 'PLATINUM' ? 'bg-blue-500' :
                profile.tier === 'DIAMOND' ? 'bg-purple-500' :
                'bg-slate-400'
            )} />
            
            <CardContent className="p-5 flex items-center gap-5 relative z-10 w-full">
                {/* Level / Tier Badge */}
                <div className={clsx(
                    "w-16 h-16 rounded-2xl flex flex-col items-center justify-center text-white shadow-lg bg-gradient-to-br flex-shrink-0",
                    getTierColor(profile.tier)
                )}>
                    <span className="text-xs font-bold tracking-widest uppercase opacity-80 mt-1">{profile.tier}</span>
                    <span className="text-2xl font-black leading-none">{profile.level}</span>
                </div>

                {/* Info & Progress */}
                <div className="flex-1">
                    <div className="flex justify-between items-end mb-2">
                        <div>
                            <h3 className="text-lg font-extrabold text-slate-800 leading-tight">Level {profile.level}</h3>
                            <p className="text-xs font-medium text-slate-500 flex items-center gap-1">
                                <Star className="w-3.5 h-3.5 text-yellow-500 fill-yellow-500" />
                                {profile.points} Total Points
                            </p>
                        </div>
                        <p className="text-xs font-bold text-slate-600 bg-slate-100 px-2 py-1 rounded-lg">
                            {profile.nextLevelPoints - profile.points} to next
                        </p>
                    </div>

                    {/* Progress Bar */}
                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                        <div 
                            className={clsx(
                                "h-full rounded-full transition-all duration-1000 ease-out bg-gradient-to-r",
                                getTierColor(profile.tier)
                            )} 
                            style={{ width: `${progress}%` }} 
                        />
                    </div>
                </div>
            </CardContent>

            {/* Badges Earned Ribbon */}
            {profile.badges && profile.badges.length > 0 && (
                <div className="bg-slate-50 border-t border-slate-100 px-5 py-3 flex items-center justify-between">
                    <p className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                        <Award className="w-4 h-4 text-emerald-500" />
                        Recent Badges
                    </p>
                    <div className="flex items-center gap-2">
                        {profile.badges.slice(-3).map((badge: any) => (
                            <div key={badge.id} className="w-6 h-6 rounded-full bg-white border border-slate-200 flex items-center justify-center text-xs shadow-sm hover:scale-110 transition-transform cursor-pointer" title={badge.name}>
                                {badge.icon}
                            </div>
                        ))}
                        {profile.badges.length > 3 && (
                            <span className="text-xs font-bold text-slate-400">+{profile.badges.length - 3}</span>
                        )}
                    </div>
                </div>
            )}
        </Card>
    );
}
