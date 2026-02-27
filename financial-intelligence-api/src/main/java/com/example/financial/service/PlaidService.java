package com.example.financial.service;

import com.example.financial.dto.AccountSummaryDTO;
import com.example.financial.dto.FinancialSummaryDTO;
import com.example.financial.entity.Transaction;
import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.repository.UserBankConnectionRepository;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaidService {
    private static final Logger log = LoggerFactory.getLogger(PlaidService.class);

    private final PlaidApi plaidApi;
    private final EncryptionService encryptionService;
    private final UserBankConnectionRepository connectionRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public PlaidService(PlaidApi plaidApi,
            EncryptionService encryptionService,
            UserBankConnectionRepository connectionRepository,
            TransactionRepository transactionRepository,
            TransactionService transactionService) {
        this.plaidApi = plaidApi;
        this.encryptionService = encryptionService;
        this.connectionRepository = connectionRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    // ─── Link Token ────────────────────────────────────────────────────────────

    public String createLinkToken(String userId) throws IOException {
        LinkTokenCreateRequestUser user = new LinkTokenCreateRequestUser().clientUserId(userId);
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(user)
                .clientName("AI Finance Assistant")
                .products(Arrays.asList(Products.TRANSACTIONS))
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en");

        Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
        LinkTokenCreateResponse body = response.body();
        if (response.isSuccessful() && body != null) {
            return body.getLinkToken();
        }
        throw new RuntimeException("Failed to create link token: " + errorBody(response));
    }

    // ─── Token Exchange ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> exchangePublicToken(String userId, String publicToken) throws IOException {
        ItemPublicTokenExchangeRequest exchangeRequest = new ItemPublicTokenExchangeRequest().publicToken(publicToken);
        Response<ItemPublicTokenExchangeResponse> response = plaidApi.itemPublicTokenExchange(exchangeRequest)
                .execute();

        if (!response.isSuccessful() || response.body() == null) {
            String err = errorBody(response);
            log.error("[Exchange] Plaid API ERROR: {}", err);
            throw new RuntimeException("Failed to exchange public token: " + err);
        }

        String accessToken = response.body().getAccessToken();
        String itemId = response.body().getItemId();

        log.info("[Exchange] Plaid success. item_id: {}, access_token_length: {}",
                itemId, accessToken != null ? accessToken.length() : "null");
        String encryptedToken = encryptionService.encrypt(accessToken);

        // Fetch accounts immediately to create per-account connection rows
        List<AccountBase> accounts = fetchAccounts(accessToken);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("item_id", itemId);
        result.put("institution_name", "Linked Institution");

        List<Map<String, Object>> resultAccounts = new ArrayList<>();

        if (accounts.isEmpty()) {
            // Fallback: create one generic connection if Plaid returns no accounts
            UserBankConnection conn = new UserBankConnection();
            conn.setUserId(userId);
            conn.setEncryptedAccessToken(encryptedToken);
            conn.setItemId(itemId);
            conn.setInstitutionName("Linked Institution");
            conn.setAccountType("CHECKING");
            connectionRepository.save(conn);
        } else {
            for (AccountBase account : accounts) {
                UserBankConnection conn = new UserBankConnection();
                conn.setUserId(userId);
                conn.setEncryptedAccessToken(encryptedToken);
                conn.setItemId(itemId);
                conn.setInstitutionName("Linked Institution");
                conn.setAccountId(account.getAccountId());
                conn.setAccountName(account.getName());
                conn.setAccountType(resolveAccountType(account));
                conn.setCurrentBalance(safeBalance(account));
                conn.setCreditLimit(resolveCreditLimit(account));
                connectionRepository.save(conn);

                Map<String, Object> accMap = new HashMap<>();
                accMap.put("account_id", account.getAccountId());
                accMap.put("name", account.getName());
                accMap.put("type", resolveAccountType(account));
                resultAccounts.add(accMap);
            }
        }

        if (!resultAccounts.isEmpty()) {
            result.put("accounts", resultAccounts);
        }

        log.info("[Exchange] Database save success. Token saved for user {}, item {}", userId, itemId);

        return result;
    }

    // ─── Transaction Sync ──────────────────────────────────────────────────────

    @Transactional
    public int syncTransactions(String userId) throws IOException {
        List<UserBankConnection> connections = connectionRepository.findByUserId(userId);
        if (connections == null || connections.isEmpty()) {
            log.warn("Sync failed - no tokens found for user {}", userId);
            throw new IllegalStateException("No bank connected");
        }
        int totalImported = 0;

        // Track processed access tokens to avoid duplicate API calls for multi-account
        // items
        List<String> processedItemIds = new ArrayList<>();

        for (UserBankConnection connection : connections) {
            if (processedItemIds.contains(connection.getItemId()))
                continue;
            processedItemIds.add(connection.getItemId());

            String encrypted = connection.getEncryptedAccessToken();
            if (encrypted == null || encrypted.isEmpty()) {
                log.warn("[Plaid] Encrypted access token missing for connection id {}", connection.getId());
                throw new IllegalStateException("Missing access token for user");
            }
            String accessToken = encryptionService.decrypt(encrypted);

            // Refresh balances for all accounts sharing this access token
            refreshBalances(userId, accessToken, connection.getItemId());

            // Sync transactions
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            // Build accountId → accountType map for this item's connections
            Map<String, String> accountTypeMap = new HashMap<>();
            connectionRepository.findByUserId(userId).stream()
                    .filter(c -> connection.getItemId().equals(c.getItemId()) && c.getAccountId() != null)
                    .forEach(c -> accountTypeMap.put(c.getAccountId(),
                            c.getAccountType() != null ? c.getAccountType() : "CHECKING"));

            TransactionsGetRequest txRequest = new TransactionsGetRequest()
                    .accessToken(accessToken)
                    .startDate(startDate)
                    .endDate(endDate);

            Response<TransactionsGetResponse> response = plaidApi.transactionsGet(txRequest).execute();

            if (response.isSuccessful() && response.body() != null) {
                for (com.plaid.client.model.Transaction pt : response.body().getTransactions()) {
                    if (!transactionRepository.existsByPlaidTransactionId(pt.getTransactionId())) {
                        // Resolve accountType from connection map; default to CHECKING if unknown
                        String plaidAccountId = pt.getAccountId();
                        String accountType = accountTypeMap.getOrDefault(plaidAccountId, "CHECKING");

                        List<String> ptCategories = pt.getCategory();
                        String category = (ptCategories != null && !ptCategories.isEmpty())
                                ? ptCategories.get(0)
                                : "Other";
                        Transaction t = Transaction.builder()
                                .description(pt.getName())
                                .amount(-pt.getAmount())
                                .transactionDate(pt.getDate().atStartOfDay())
                                .category(category)
                                .plaidTransactionId(pt.getTransactionId())
                                .accountId(plaidAccountId)
                                .accountType(accountType)
                                .build();

                        transactionService.createTransaction(t);
                        totalImported++;
                    }
                }
            } else {
                String err = errorBody(response);
                log.error("[Plaid] Failed to sync transactions for item {}: {}", connection.getItemId(), err);
                throw new IOException(err);
            }
        }
        return totalImported;
    }

    // ─── Account Financial Summary ─────────────────────────────────────────────

    /**
     * Builds a full financial position summary for the user from stored account
     * data.
     * Does not make a live Plaid API call — uses cached balances from the DB.
     */
    public FinancialSummaryDTO getFinancialSummary(String userId) {
        List<UserBankConnection> connections = connectionRepository.findByUserId(userId);

        double totalAssets = 0;
        double totalLiabilities = 0;
        double totalCreditLimit = 0;
        List<AccountSummaryDTO> accounts = new ArrayList<>();

        for (UserBankConnection conn : connections) {
            double balance = conn.getCurrentBalance() != null ? conn.getCurrentBalance() : 0.0;
            String type = conn.getAccountType() != null ? conn.getAccountType() : "CHECKING";

            Double utilization = null;
            Double creditLimit = conn.getCreditLimit();

            if ("CREDIT".equals(type)) {
                totalLiabilities += balance;
                if (creditLimit != null && creditLimit > 0) {
                    totalCreditLimit += creditLimit;
                    utilization = Math.round((balance / creditLimit) * 1000.0) / 10.0;
                }
            } else {
                totalAssets += balance;
            }

            accounts.add(new AccountSummaryDTO(
                    conn.getAccountId(),
                    conn.getAccountName() != null ? conn.getAccountName() : conn.getInstitutionName(),
                    conn.getInstitutionName(),
                    type,
                    balance,
                    creditLimit,
                    utilization));
        }

        return new FinancialSummaryDTO(totalAssets, totalLiabilities, totalCreditLimit, accounts);
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private List<AccountBase> fetchAccounts(String accessToken) {
        try {
            AccountsGetRequest req = new AccountsGetRequest().accessToken(accessToken);
            Response<AccountsGetResponse> res = plaidApi.accountsGet(req).execute();
            AccountsGetResponse body = res.body();
            if (res.isSuccessful() && body != null) {
                return body.getAccounts() != null ? body.getAccounts() : List.of();
            }
        } catch (Exception e) {
            log.warn("[Plaid] Could not fetch accounts: {}", e.getMessage());
        }
        return List.of();
    }

    private void refreshBalances(String userId, String accessToken, String itemId) {
        try {
            List<AccountBase> accounts = fetchAccounts(accessToken);
            List<UserBankConnection> conns = connectionRepository.findByUserId(userId);
            for (AccountBase account : accounts) {
                conns.stream()
                        .filter(c -> itemId.equals(c.getItemId()) && account.getAccountId().equals(c.getAccountId()))
                        .findFirst()
                        .ifPresent(c -> {
                            c.setCurrentBalance(safeBalance(account));
                            c.setCreditLimit(resolveCreditLimit(account));
                            connectionRepository.save(c);
                        });
            }
        } catch (Exception e) {
            log.warn("[Plaid] Balance refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Maps a Plaid account type/subtype to our internal enum string.
     * Plaid types: depository (checking/savings), credit (credit card), loan,
     * investment
     */
    private String resolveAccountType(AccountBase account) {
        if (account.getType() == null)
            return "CHECKING";
        String type = account.getType().getValue().toLowerCase();
        AccountSubtype subtype = account.getSubtype();
        String subtypeStr = (subtype != null) ? subtype.getValue().toLowerCase() : "";

        if ("credit".equals(type))
            return "CREDIT";
        if ("depository".equals(type)) {
            if ("savings".equals(subtypeStr))
                return "SAVINGS";
            return "CHECKING";
        }
        return "CHECKING";
    }

    private Double safeBalance(AccountBase account) {
        if (account.getBalances() == null)
            return 0.0;
        Double current = account.getBalances().getCurrent();
        return current != null ? current : 0.0;
    }

    private Double resolveCreditLimit(AccountBase account) {
        if (!"CREDIT".equals(resolveAccountType(account)))
            return null;
        if (account.getBalances() == null)
            return null;
        return account.getBalances().getLimit();
    }

    private String errorBody(Response<?> response) {
        try {
            okhttp3.ResponseBody errBody = response.errorBody();
            return errBody != null ? errBody.string() : "Unknown error";
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
