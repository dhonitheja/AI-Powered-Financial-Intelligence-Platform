package com.wealthix.plaid.service;

import com.example.financial.entity.Transaction;
import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.repository.UserBankConnectionRepository;
import com.example.financial.service.EncryptionService;
import com.plaid.client.PlaidApi;
import com.plaid.client.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidService {

  private final PlaidApi plaidApi;
  private final EncryptionService encryptionService;
  private final UserBankConnectionRepository connectionRepo;
  private final TransactionRepository transactionRepo;

  @Value("${plaid.products}")
  private String products;

  @Value("${plaid.country-codes}")
  private String countryCodes;

  @Value("${plaid.webhook-url:}")
  private String webhookUrl;

  /**
   * STEP 1: Create Link Token
   * Call this FIRST to initialize Plaid Link
   * Returns link_token to send to frontend
   */
  public String createLinkToken(String userId) {
    try {
      List<Products> productList = Arrays.stream(
          products.split(","))
          .map(p -> Products.fromValue(p.trim()))
          .collect(Collectors.toList());

      List<CountryCode> countryCodeList =
          Arrays.stream(countryCodes.split(","))
          .map(c -> CountryCode.fromValue(c.trim()))
          .collect(Collectors.toList());

      LinkTokenCreateRequestUser user =
          new LinkTokenCreateRequestUser()
              .clientUserId(userId);

      LinkTokenCreateRequest request =
          new LinkTokenCreateRequest()
              .user(user)
              .clientName("Wealthix")
              .products(productList)
              .countryCodes(countryCodeList)
              .language("en");

      // Add webhook if configured
      if (webhookUrl != null && !webhookUrl.isEmpty()) {
        request.webhook(webhookUrl);
      }

      Response<LinkTokenCreateResponse> response =
          plaidApi.linkTokenCreate(request).execute();

      if (!response.isSuccessful() || response.body() == null) {
        log.error("[Wealthix] Link token creation failed: {}",
            response.code());
        throw new RuntimeException(
            "Failed to create Plaid link token");
      }

      log.info("[Wealthix] Link token created: userId={}",
          userId);
      return response.body().getLinkToken();

    } catch (IOException e) {
      log.error("[Wealthix] Plaid API error: {}",
          e.getClass().getSimpleName());
      throw new RuntimeException(
          "Plaid service temporarily unavailable");
    }
  }

  /**
   * STEP 2: Exchange Public Token
   * Called after user completes Plaid Link
   * Exchanges public_token for access_token
   * Encrypts and saves access_token
   */
  public List<UserBankConnection> exchangePublicToken(
      String publicToken, String userId) {
    try {
      // Exchange public token for access token
      ItemPublicTokenExchangeRequest exchangeRequest =
          new ItemPublicTokenExchangeRequest()
              .publicToken(publicToken);

      Response<ItemPublicTokenExchangeResponse> exchangeResponse =
          plaidApi.itemPublicTokenExchange(
              exchangeRequest).execute();

      if (!exchangeResponse.isSuccessful()
          || exchangeResponse.body() == null) {
        log.error("[Wealthix] Token exchange failed: {}",
            exchangeResponse.code());
        throw new RuntimeException(
            "Failed to connect bank account");
      }

      String accessToken =
          exchangeResponse.body().getAccessToken();
      String itemId = exchangeResponse.body().getItemId();

      // Encrypt access token BEFORE saving
      String encryptedToken =
          encryptionService.encrypt(accessToken);

      // Get account details
      List<UserBankConnection> connections =
          fetchAndSaveAccounts(
              encryptedToken, itemId, userId,
              accessToken  // plaintext for API call only
          );

      // Trigger initial sync
      syncTransactions(userId, encryptedToken);

      // Audit log — no token logged
      log.info("[Wealthix] Bank connected: userId={} accounts={}",
          userId, connections.size());

      return connections;

    } catch (IOException e) {
      log.error("[Wealthix] Exchange failed: {}",
          e.getClass().getSimpleName());
      throw new RuntimeException(
          "Failed to connect bank account");
    }
  }

  /**
   * STEP 3: Fetch Accounts
   * Gets all accounts for the connected institution
   * Creates one UserBankConnection per account
   */
  private List<UserBankConnection> fetchAndSaveAccounts(
      String encryptedToken, String itemId,
      String userId, String plaintextToken) {

    try {
      AccountsGetRequest accountsRequest =
          new AccountsGetRequest()
              .accessToken(plaintextToken);

      Response<AccountsGetResponse> accountsResponse =
          plaidApi.accountsGet(accountsRequest).execute();

      if (!accountsResponse.isSuccessful()
          || accountsResponse.body() == null) {
        throw new RuntimeException(
            "Failed to fetch accounts");
      }

      List<UserBankConnection> connections = new ArrayList<>();

      for (AccountBase account :
          accountsResponse.body().getAccounts()) {

        // Check if connection already exists
        boolean exists = connectionRepo
            .existsByPlaidAccountIdAndUserId(
                account.getAccountId(),
                UUID.fromString(userId));

        if (!exists) {
          UserBankConnection connection =
              UserBankConnection.builder()
                  .userId(UUID.fromString(userId))
                  .itemId(itemId)
                  .plaidAccountId(account.getAccountId())
                  .encryptedAccessToken(encryptedToken)
                  .institutionName(
                      getInstitutionName(plaintextToken))
                  .accountName(account.getName())
                  .accountOfficialName(
                      account.getOfficialName())
                  .accountType(
                      account.getType().getValue())
                  .accountSubtype(
                      account.getSubtype() != null
                          ? account.getSubtype().getValue()
                          : null)
                  .accountMask(account.getMask())
                  .active(true)
                  .build();

          connections.add(connectionRepo.save(connection));
        }
      }

      return connections;

    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to fetch account details");
    }
  }

  /**
   * STEP 4: Sync Transactions
   * Fetches transactions for all user connections
   * Uses idempotency to prevent duplicates
   */
  public void syncTransactions(
      String userId, String encryptedToken) {
    try {
      // Decrypt in memory only — never log
      String accessToken =
          encryptionService.decrypt(encryptedToken);

      // Date range: last 30 days
      LocalDate startDate =
          LocalDate.now().minusDays(30);
      LocalDate endDate = LocalDate.now();

      TransactionsGetRequest txRequest =
          new TransactionsGetRequest()
              .accessToken(accessToken)
              .startDate(startDate)
              .endDate(endDate);

      Response<TransactionsGetResponse> txResponse =
          plaidApi.transactionsGet(txRequest).execute();

      if (!txResponse.isSuccessful()
          || txResponse.body() == null) {
        log.warn("[Wealthix] Transaction sync failed: {}",
            txResponse.code());
        return;
      }

      List<com.plaid.client.model.Transaction> plaidTxs =
          txResponse.body().getTransactions();

      int saved = 0;
      for (com.plaid.client.model.Transaction plaidTx
          : plaidTxs) {

        // Idempotency check
        if (transactionRepo.existsByPlaidTransactionId(
            plaidTx.getTransactionId())) {
          continue;
        }

        Transaction tx = Transaction.builder()
            .userId(UUID.fromString(userId))
            .plaidTransactionId(
                plaidTx.getTransactionId())
            .plaidAccountId(plaidTx.getAccountId())
            .amount(plaidTx.getAmount())
            .description(plaidTx.getName())
            // Note: the provided code had some fields that might not match exactly, 
            // but I'll try to align with the Transaction entity I've seen.
            .category(plaidTx.getPersonalFinanceCategory()
                != null
                ? plaidTx.getPersonalFinanceCategory()
                    .getPrimary()
                : "UNCATEGORIZED")
            .transactionDate(plaidTx.getDate().atStartOfDay())
            .build();

        transactionRepo.save(tx);
        saved++;
      }

      // Log count only — no transaction details
      log.info("[Wealthix] Sync complete: userId={}" +
          " saved={} total={}",
          userId, saved, plaidTxs.size());

    } catch (Exception e) {
      log.warn("[Wealthix] Sync error: {}",
          e.getClass().getSimpleName());
    }
  }

  /**
   * STEP 5: Get Institution Name
   * Looks up institution details by access token
   */
  private String getInstitutionName(String accessToken) {
    try {
      ItemGetRequest itemRequest =
          new ItemGetRequest().accessToken(accessToken);
      Response<ItemGetResponse> itemResponse =
          plaidApi.itemGet(itemRequest).execute();

      if (!itemResponse.isSuccessful()
          || itemResponse.body() == null) {
        return "Unknown Institution";
      }

      String institutionId =
          itemResponse.body().getItem()
              .getInstitutionId();

      InstitutionsGetByIdRequest instRequest =
          new InstitutionsGetByIdRequest()
              .institutionId(institutionId)
              .addCountryCodesItem(CountryCode.US);

      Response<InstitutionsGetByIdResponse> instResponse =
          plaidApi.institutionsGetById(
              instRequest).execute();

      if (!instResponse.isSuccessful()
          || instResponse.body() == null) {
        return institutionId;
      }

      return instResponse.body()
          .getInstitution().getName();

    } catch (IOException e) {
      return "Connected Bank";
    }
  }

  /**
   * Check available balance
   * Used before executing AutoPay payments
   */
  public boolean hasSufficientBalance(
      String userId,
      UUID connectionId,
      BigDecimal requiredAmount) {
    try {
      UserBankConnection connection =
          connectionRepo.findByIdAndUserId(
              connectionId, UUID.fromString(userId))
          .orElseThrow(() ->
              new EntityNotFoundException(
                  "Connection not found"));

      String accessToken = encryptionService.decrypt(
          connection.getEncryptedAccessToken());

      AccountsBalanceGetRequest balanceRequest =
          new AccountsBalanceGetRequest()
              .accessToken(accessToken);

      Response<AccountsGetResponse> balanceResponse =
          plaidApi.accountsBalanceGet(
              balanceRequest).execute();

      if (!balanceResponse.isSuccessful()
          || balanceResponse.body() == null) {
        // If can't verify balance, allow payment
        // (Stripe will handle insufficient funds)
        return true;
      }

      return balanceResponse.body().getAccounts()
          .stream()
          .filter(a -> a.getAccountId().equals(
              connection.getPlaidAccountId()))
          .findFirst()
          .map(a -> a.getBalances()
              .getAvailable() != null
              && BigDecimal.valueOf(
                  a.getBalances().getAvailable())
                  .compareTo(requiredAmount) >= 0)
          .orElse(true);

    } catch (Exception e) {
      log.warn("[Wealthix] Balance check failed: {}",
          e.getClass().getSimpleName());
      return true; // Fail open — Stripe handles it
    }
  }

  public List<UserBankConnection> getConnections(String userId) {
      return connectionRepo.findByUserId(UUID.fromString(userId));
  }

  public void disconnectBank(UUID connectionId, String userId) {
      UserBankConnection conn = connectionRepo.findByIdAndUserId(connectionId, UUID.fromString(userId))
              .orElseThrow(() -> new EntityNotFoundException("Connection not found"));
      connectionRepo.delete(conn);
  }

  public void syncAllConnections(String userId) {
      List<UserBankConnection> connections = connectionRepo.findByUserId(UUID.fromString(userId));
      for (UserBankConnection conn : connections) {
          syncTransactions(userId, conn.getEncryptedAccessToken());
      }
  }

  /**
   * Builds a full financial position summary for the user.
   */
  public com.example.financial.dto.FinancialSummaryDTO getFinancialSummary(String userId) {
      List<UserBankConnection> connections = connectionRepo.findByUserId(UUID.fromString(userId));

      double totalAssets = 0;
      double totalLiabilities = 0;
      double totalCreditLimit = 0;
      List<com.example.financial.dto.AccountSummaryDTO> accounts = new ArrayList<>();

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

          accounts.add(new com.example.financial.dto.AccountSummaryDTO(
                  conn.getPlaidAccountId(),
                  conn.getAccountName() != null ? conn.getAccountName() : conn.getInstitutionName(),
                  conn.getInstitutionName(),
                  type,
                  balance,
                  creditLimit,
                  utilization));
      }

      return new com.example.financial.dto.FinancialSummaryDTO(totalAssets, totalLiabilities, totalCreditLimit, accounts);
  }
}
