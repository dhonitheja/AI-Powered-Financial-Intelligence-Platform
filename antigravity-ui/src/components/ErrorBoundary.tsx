"use client";
import { Component, ReactNode } from "react";
import { trackError } from "@/lib/monitoring";

interface Props { children: ReactNode; page: string; }
interface State { hasError: boolean; }

export class WealthixErrorBoundary extends Component<Props, State> {
    state: State = { hasError: false };

    static getDerivedStateFromError(): State {
        return { hasError: true };
    }

    componentDidCatch(error: Error) {
        trackError(error, {
            page: this.props.page,
            component: "ErrorBoundary"
        });
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
                    <p className="text-slate-400 text-sm">
                        Wealthix couldn&apos;t load this section.
                    </p>
                    <button
                        onClick={() => this.setState({ hasError: false })}
                        className="px-4 py-2 bg-blue-500 text-white rounded-xl text-sm hover:bg-blue-400"
                    >
                        Try again
                    </button>
                </div>
            );
        }
        return this.props.children;
    }
}
