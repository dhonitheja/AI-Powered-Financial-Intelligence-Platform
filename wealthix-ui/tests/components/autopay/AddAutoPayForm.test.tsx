import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AddAutoPayForm } from '@/components/autopay/AddAutoPayForm';
import autoPayService from '@/services/autoPayService';
import { useRouter } from 'next/navigation';

// Mock dependencies
jest.mock('@/services/autoPayService');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

const mockCategories = [
  { value: 'SUBSCRIPTION', displayName: 'Subscription', icon: 'zap' },
  { value: 'RENT', displayName: 'Rent', icon: 'home' },
];

describe('AddAutoPayForm', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush, back: jest.fn() });
    (autoPayService.getCategories as jest.Mock).mockResolvedValue({ data: mockCategories });
  });

  it('step1 cannot proceed without category', async () => {
    render(<AddAutoPayForm />);
    const continueBtn = screen.getByRole('button', { name: /continue/i });
    expect(continueBtn).toBeDisabled();

    await waitFor(() => expect(screen.getByText('Subscription')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Subscription'));
    expect(continueBtn).not.toBeDisabled();
  });

  it('step2 cannot proceed without name + amount', async () => {
    render(<AddAutoPayForm />);
    
    // Step 0 -> Step 1
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    const continueBtn = screen.getByRole('button', { name: /continue/i });
    expect(continueBtn).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/payment name/i), { target: { value: 'Netflix' } });
    expect(continueBtn).toBeDisabled(); // still need amount

    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '15.99' } });
    expect(continueBtn).not.toBeDisabled();
  });

  it('step2 rejects negative amounts', async () => {
    render(<AddAutoPayForm />);
    
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    fireEvent.change(screen.getByLabelText(/payment name/i), { target: { value: 'Netflix' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '-5.00' } });
    
    expect(screen.getByRole('button', { name: /continue/i })).toBeDisabled();
  });

  it('step2 account number field is type=password', async () => {
    render(<AddAutoPayForm />);
    
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    const accInput = screen.getByLabelText(/account number/i);
    expect(accInput).toHaveAttribute('type', 'password');
  });

  it('step3 cannot proceed without due date', async () => {
    render(<AddAutoPayForm />);
    
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));
    fireEvent.change(screen.getByLabelText(/payment name/i), { target: { value: 'Netflix' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '15.99' } });
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    const nextBtn = screen.getByRole('button', { name: /continue/i });
    // frequency defaults to empty or first option? AddAutoPayForm.tsx: "Select frequency" disabled
    expect(nextBtn).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/frequency/i), { target: { value: 'MONTHLY' } });
    expect(nextBtn).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/next due date/i), { target: { value: '2025-12-01' } });
    expect(nextBtn).not.toBeDisabled();
  });

  it('step4 shows review without full account number', async () => {
    render(<AddAutoPayForm />);
    
    // Fill all steps
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));
    fireEvent.change(screen.getByLabelText(/payment name/i), { target: { value: 'Netflix' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '15.99' } });
    fireEvent.change(screen.getByLabelText(/account number/i), { target: { value: '1234567890' } });
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));
    fireEvent.change(screen.getByLabelText(/frequency/i), { target: { value: 'MONTHLY' } });
    fireEvent.change(screen.getByLabelText(/next due date/i), { target: { value: '2025-12-01' } });
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    expect(screen.getByText(/review & confirm/i)).toBeInTheDocument();
    expect(screen.getByText(/7890/)).toBeInTheDocument();
    expect(screen.queryByText('1234567890')).not.toBeInTheDocument();
  });

  it('submit calls createSchedule API', async () => {
    (autoPayService.createSchedule as jest.Mock).mockResolvedValue({ status: 201 });
    render(<AddAutoPayForm />);
    
    // Fill all steps
    await waitFor(() => fireEvent.click(screen.getByText('Subscription')));
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));
    fireEvent.change(screen.getByLabelText(/payment name/i), { target: { value: 'Netflix' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '15.99' } });
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));
    fireEvent.change(screen.getByLabelText(/frequency/i), { target: { value: 'MONTHLY' } });
    fireEvent.change(screen.getByLabelText(/next due date/i), { target: { value: '2025-12-01' } });
    fireEvent.click(screen.getByRole('button', { name: /continue/i }));

    const saveBtn = screen.getByRole('button', { name: /save schedule/i });
    fireEvent.click(saveBtn);

    await waitFor(() => expect(autoPayService.createSchedule).toHaveBeenCalled());
    expect(mockPush).toHaveBeenCalledWith('/dashboard/autopay');
  });
});
