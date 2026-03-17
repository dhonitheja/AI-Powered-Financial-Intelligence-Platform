
const axios = require('axios');

async function main() {
  const baseURL = 'http://localhost:8080/api';
  console.log('--- Seeding EMI/AutoPay Data ---');

  try {
    // 1. Login
    const loginRes = await axios.post(`${baseURL}/auth/signin`, {
      email: 'dhonitheja007@gmail.com',
      password: 'password123'
    });
    const token = loginRes.data.token;
    console.log('Login successful.');

    const headers = { Authorization: `Bearer ${token}` };

    // 2. Create Home Loan EMI
    console.log('Adding Home Loan EMI...');
    await axios.post(`${baseURL}/v1/autopay/schedules`, {
      paymentName: 'Chase Mortgage Payment',
      paymentCategory: 'HOME_LOAN',
      paymentProvider: 'Chase Bank',
      amount: 1250.00,
      currency: 'USD',
      frequency: 'MONTHLY',
      nextDueDate: '2026-04-01',
      dueDayOfMonth: 1,
      autoExecute: true,
      reminderDaysBefore: 5,
      accountNumber: '1234567890',
      routingNumber: '123456789'
    }, { headers });

    // 3. Create Auto Loan EMI
    console.log('Adding Auto Loan EMI...');
    await axios.post(`${baseURL}/v1/autopay/schedules`, {
      paymentName: 'Tesla Model 3 EMI',
      paymentCategory: 'AUTO_LOAN',
      paymentProvider: 'Tesla Finance',
      amount: 499.00,
      currency: 'USD',
      frequency: 'MONTHLY',
      nextDueDate: '2026-03-25',
      dueDayOfMonth: 25,
      autoExecute: false,
      reminderDaysBefore: 3,
      accountNumber: '0987654321',
      routingNumber: '987654321'
    }, { headers });

    console.log('EMI seeding complete!');
  } catch (err) {
    console.error('Error seeding EMI data:', err.response?.data || err.message);
  }
}

main();
