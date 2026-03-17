import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

interface CardProps {
    children: React.ReactNode;
    className?: string;
    title?: string;
    subtitle?: string;
}

export const Card = ({ children, className, title, subtitle }: CardProps) => {
    return (
        <div className={cn("glass-obsidian glow-card premium-border rounded-3xl p-8 hover:shadow-[0_20px_50px_rgba(0,0,0,0.5)] transition-all duration-500 overflow-hidden group relative", className)}>
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-blue-500/10 via-purple-500/5 to-transparent rounded-bl-full opacity-50 group-hover:opacity-100 transition-opacity duration-500" />
            <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-gradient-to-tr from-purple-500/10 via-blue-500/5 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-700" />
            
            {(title || subtitle) && (
                <div className="mb-6 relative z-10">
                    {title && <h3 className="text-xl font-black text-white tracking-tight group-hover:text-blue-200 transition-colors uppercase">{title}</h3>}
                    {subtitle && <p className="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] mt-1.5">{subtitle}</p>}
                </div>
            )}
            <div className="relative z-10">{children}</div>
        </div>
    );
};
export const CardHeader = ({ className, children }: { className?: string, children: React.ReactNode }) => (
    <div className={cn("p-6", className)}>{children}</div>
);

export const CardTitle = ({ className, children }: { className?: string, children: React.ReactNode }) => (
    <h3 className={cn("text-lg font-semibold text-primary", className)}>{children}</h3>
);

export const CardContent = ({ className, children }: { className?: string, children: React.ReactNode }) => (
    <div className={cn("p-6 pt-0", className)}>{children}</div>
);
