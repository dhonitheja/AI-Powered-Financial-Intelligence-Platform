"use client";

import { useState, useEffect } from "react";
import { loadStripe, Stripe } from "@stripe/stripe-js";
import { Elements, PaymentElement, useStripe, useElements } from "@stripe/react-stripe-js";
import { CreditCard, Plus, Trash2, X, Loader2, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import autoPayService from "@/services/autoPayService";

interface PaymentMethod {
    paymentMethodId: string;
    brand: string;
    last4: string;
    expMonth: number;
    expYear: number;
    isDefault: boolean;
}

interface PaymentMethodsModalProps {
    isOpen: boolean;
    onClose: () => void;
}

function AddCardForm({ onSuccess, onCancel }: { onSuccess: () => void, onCancel: () => void }) {
    const stripe = useStripe();
    const elements = useElements();
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!stripe || !elements) return;

        setIsLoading(true);

        const { error, setupIntent } = await stripe.confirmSetup({
            elements,
            redirect: "if_required", // We don't want to redirect the page right now
        });

        if (error) {
            toast.error(error.message || "Failed to set up payment method");
            setIsLoading(false);
        } else if (setupIntent && setupIntent.status === "succeeded") {
            // Setup intent succeeded, attach the card to backend
            try {
                if (typeof setupIntent.payment_method === 'string') {
                    await autoPayService.attachPaymentMethod(setupIntent.payment_method);
                    toast.success("Payment method added successfully!");
                    onSuccess();
                } else {
                    toast.error("Unexpected setup intent response.");
                }
            } catch (err: any) {
                toast.error(err.response?.data?.message || "Failed to save card on our end.");
            } finally {
                setIsLoading(false);
            }
        } else {
            setIsLoading(false);
            toast.error("Setup incomplete. Status: " + setupIntent?.status);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="rounded-xl border border-white/5 bg-white/5 p-4">
                <PaymentElement />
            </div>

            <div className="flex justify-end gap-3 pt-2">
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isLoading}
                    className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors disabled:opacity-50"
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={!stripe || isLoading}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-purple-600 hover:bg-purple-500 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    Save Card
                </button>
            </div>
        </form>
    );
}

export default function PaymentMethodsModal({ isOpen, onClose }: PaymentMethodsModalProps) {
    const [methods, setMethods] = useState<PaymentMethod[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isAdding, setIsAdding] = useState(false);

    const [clientSecret, setClientSecret] = useState<string | null>(null);
    const [stripePromise, setStripePromise] = useState<Promise<Stripe | null> | null>(null);

    useEffect(() => {
        if (isOpen) {
            loadMethods();
        } else {
            // Reset state when closed
            setIsAdding(false);
            setClientSecret(null);
        }
    }, [isOpen]);

    const loadMethods = async () => {
        setIsLoading(true);
        try {
            const res = await autoPayService.listPaymentMethods();
            setMethods(res.data);
        } catch (err: any) {
            toast.error(err.response?.data?.message || "Failed to load payment methods");
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddClick = async () => {
        try {
            setIsAdding(true);
            const { data } = await autoPayService.createSetupIntent();
            setClientSecret(data.clientSecret);
            if (!stripePromise) {
                setStripePromise(loadStripe(data.publishableKey));
            }
        } catch (err: any) {
            toast.error("Failed to initialize secure payment form");
            setIsAdding(false);
        }
    };

    const handleDetach = async (id: string) => {
        try {
            await autoPayService.detachPaymentMethod(id);
            toast.success("Payment method removed");
            loadMethods();
        } catch (err: any) {
            toast.error(err.response?.data?.message || "Failed to remove payment method");
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

            <div className="relative w-full max-w-md bg-[#0F1117]/95 border border-white/10 rounded-2xl shadow-2xl p-6 shadow-purple-900/20 max-h-[90vh] overflow-y-auto">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-semibold text-white flex items-center gap-2">
                        <CreditCard className="w-5 h-5 text-purple-400" />
                        Payment Methods
                    </h2>
                    <button
                        onClick={onClose}
                        className="p-2 text-slate-400 hover:text-white hover:bg-white/5 rounded-full transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {!isAdding ? (
                    <div className="space-y-4">
                        {isLoading ? (
                            <div className="flex justify-center py-8">
                                <Loader2 className="w-6 h-6 animate-spin text-purple-500" />
                            </div>
                        ) : methods.length === 0 ? (
                            <div className="text-center py-8 bg-white/5 rounded-xl border border-white/5 border-dashed">
                                <CreditCard className="w-10 h-10 text-slate-500 mx-auto mb-3" />
                                <p className="text-sm text-slate-400">No payment methods saved.</p>
                                <p className="text-xs text-slate-500 mt-1">Add a card to enable fully automated AutoPay executions.</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {methods.map((method) => (
                                    <div key={method.paymentMethodId} className="flex items-center justify-between p-4 bg-white/[0.03] border border-white/5 rounded-xl hover:bg-white/[0.06] transition-colors relative overflow-hidden group">
                                        {method.isDefault && (
                                            <div className="absolute top-0 right-0 px-2 py-0.5 bg-purple-500/20 text-purple-300 text-[10px] font-medium rounded-bl-lg uppercase tracking-wide">
                                                Default
                                            </div>
                                        )}
                                        <div className="flex items-center gap-4">
                                            <div className="w-12 h-8 bg-white/10 rounded flex items-center justify-center uppercase font-bold text-xs text-slate-300">
                                                {method.brand === 'unknown' ? 'CARD' : method.brand}
                                            </div>
                                            <div>
                                                <p className="text-sm font-medium text-white">
                                                    •••• {method.last4}
                                                </p>
                                                <p className="text-xs text-slate-400">
                                                    Expires {method.expMonth.toString().padStart(2, '0')}/{method.expYear.toString().slice(-2)}
                                                </p>
                                            </div>
                                        </div>
                                        <button
                                            onClick={() => handleDetach(method.paymentMethodId)}
                                            className="p-2 text-slate-400 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all rounded-full hover:bg-red-500/10"
                                            title="Remove card"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}

                        <button
                            onClick={handleAddClick}
                            className="w-full py-3 flex items-center justify-center gap-2 border border-dashed border-purple-500/30 text-purple-400 hover:text-purple-300 hover:bg-purple-500/5 rounded-xl font-medium text-sm transition-all"
                        >
                            <Plus className="w-4 h-4" />
                            Add New Card
                        </button>
                    </div>
                ) : (
                    <div>
                        {clientSecret && stripePromise ? (
                            <Elements stripe={stripePromise} options={{
                                clientSecret,
                                appearance: {
                                    theme: 'night',
                                    variables: {
                                        colorPrimary: '#9333ea',
                                        colorBackground: 'transparent',
                                        colorText: '#f8fafc',
                                        colorDanger: '#ef4444',
                                        spacingUnit: '4px',
                                        borderRadius: '8px',
                                    }
                                }
                            }}>
                                <AddCardForm
                                    onSuccess={() => {
                                        setIsAdding(false);
                                        loadMethods();
                                    }}
                                    onCancel={() => setIsAdding(false)}
                                />
                            </Elements>
                        ) : (
                            <div className="flex justify-center py-8">
                                <Loader2 className="w-6 h-6 animate-spin text-purple-500" />
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
