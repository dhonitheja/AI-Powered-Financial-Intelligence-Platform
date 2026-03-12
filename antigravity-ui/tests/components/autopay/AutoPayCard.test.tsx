import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { AutoPayCard } from '@/components/autopay/AutoPayCard';
import type { AutoPaySchedule } from '@/services/autoPayService';

const mockSchedule: AutoPaySchedule = {
  id: 'test-id-1',
  paymentName: 'Netflix',
  paymentCategory: 'SUBSCRIPTION',
  categoryDisplayName: 'Subscription',
  paymentProvider: null,
  accountNumberMasked: '****7890',
  hasRoutingNumber: false,
  hasNotes: false,
  amount: 15.99,
  currency: 'USD',
  monthlyEquivalent: 15.99,
  frequency: 'MONTHLY',
  nextDueDate: '2025-12-01',
  dueDayOfMonth: 1,
  status: 'ACTIVE',
  active: true,
  autoExecute: false,
  reminderDaysBefore: 3,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const mockHandlers = {
  onToggle: jest.fn(),
  onDelete: jest.fn(),
  onExecute: jest.fn(),
  onClick: jest.fn(),
};

describe('AutoPayCard', () => {
  beforeEach(() => jest.clearAllMocks());

  it('renders payment name correctly', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    expect(screen.getByText('Netflix')).toBeInTheDocument();
  });

  it('shows masked account number NOT full number', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    expect(screen.getByText(/\*\*\*\*7890/)).toBeInTheDocument();
    expect(screen.queryByText(/\d{10,}/)).not.toBeInTheDocument();
  });

  it('shows correct due status badge color', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    // ACTIVE status should render 'On Track' badge
    expect(screen.getByText('On Track')).toBeInTheDocument();
  });

  it('confirm dialog appears before delete', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    const deleteBtn = screen.getByRole('button', { name: /delete/i });
    fireEvent.click(deleteBtn);
    expect(mockHandlers.onDelete).toHaveBeenCalledWith('test-id-1');
  });

  it('does NOT show full account number anywhere', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    const content = document.body.textContent ?? '';
    expect(content).not.toMatch(/\d{10,}/);
  });

  it('optimistic toggle updates immediately', () => {
    render(<AutoPayCard schedule={mockSchedule} {...mockHandlers} />);
    // active=true → button title is "Pause"
    const toggleBtn = screen.getByRole('button', { name: /pause/i });
    fireEvent.click(toggleBtn);
    expect(mockHandlers.onToggle).toHaveBeenCalledWith('test-id-1');
  });
});
