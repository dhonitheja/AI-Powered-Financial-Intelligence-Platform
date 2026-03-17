
const axios = require('axios');

async function main() {
  const baseURL = 'http://localhost:8080/api';
  console.log('--- Seeding Demo User Data ---');

  try {
    // 1. Login
    const loginRes = await axios.post(`${baseURL}/auth/signin`, {
      email: 'dhonitheja007@gmail.com',
      password: 'password123'
    });
    const token = loginRes.data.token;
    console.log('Login successful.');

    const headers = { Authorization: `Bearer ${token}` };

    // 2. Get Me to confirm
    const meRes = await axios.get(`${baseURL}/auth/me`, { headers });
    const userEmail = meRes.data.email;
    console.log(`Verified user: ${userEmail}`);

    // Transactions to inject
    const today = new Date();
    const marchStart = new Date(2026, 2, 1);
    const febStart = new Date(2026, 1, 1);

    const transactions = [
      // --- MARCH (Current Period) ---
      { description: 'Starbucks Coffee', amount: 5.50, category: 'FOOD_AND_DRINK', transactionDate: '2026-03-15' },
      { description: 'Amazon Order #123', amount: 89.99, category: 'ENTERTAINMENT', transactionDate: '2026-03-14' },
      { description: 'Shell Gas Station', amount: 45.00, category: 'TRANSPORTATION', transactionDate: '2026-03-13' },
      { description: 'Whole Foods Market', amount: 120.50, category: 'FOOD_AND_DRINK', transactionDate: '2026-03-10' },
      { description: 'Netflix Subscription', amount: 15.99, category: 'ENTERTAINMENT', transactionDate: '2026-03-05' },
      { description: 'Salary Deposit', amount: 2500.00, category: 'INCOME', transactionDate: '2026-03-01' },
      
      // --- FEBRUARY (Previous Period) ---
      { description: 'Feb Rent Payment', amount: 1200.00, category: 'RENT', transactionDate: '2026-02-01' },
      { description: 'Amazon (Gaming)', amount: 150.00, category: 'ENTERTAINMENT', transactionDate: '2026-02-15' },
      { description: 'Shell Gas Station', amount: 35.00, category: 'TRANSPORTATION', transactionDate: '2026-02-10' },
      { description: 'Grocery Store X', amount: 95.00, category: 'FOOD_AND_DRINK', transactionDate: '2026-02-20' },
      { description: 'Gym Membership', amount: 50.00, category: 'FITNESS', transactionDate: '2026-02-05' },
    ];

    console.log(`Injecting ${transactions.length} transactions...`);

    for (const tx of transactions) {
      // The API expects LocalDateTime or specific format? Transaction entity has LocalDate.
      // DTO usually has formatted string. 
      // Let's check TransactionRequest.java
      await axios.post(`${baseURL}/transactions`, {
        description: tx.description,
        amount: tx.amount,
        category: tx.category,
        transactionDate: `${tx.transactionDate}T12:00:00.000Z`
      }, { headers });
    }

    console.log('Seeding complete!');
  } catch (err) {
    console.error('Error seeding data:', err.response?.data || err.message);
  }
}

main();
