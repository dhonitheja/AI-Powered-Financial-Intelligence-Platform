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
        <div className={cn("bg-white rounded-xl border border-slate-200 shadow-sm p-6 card-hover", className)}>
            {(title || subtitle) && (
                <div className="mb-4">
                    {title && <h3 className="text-lg font-semibold text-primary">{title}</h3>}
                    {subtitle && <p className="text-sm text-muted">{subtitle}</p>}
                </div>
            )}
            {children}
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
