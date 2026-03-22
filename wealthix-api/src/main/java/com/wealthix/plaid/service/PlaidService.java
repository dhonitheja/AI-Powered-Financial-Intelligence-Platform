package com.wealthix.plaid.service;

import com.wealthix.entity.Transaction;
import com.wealthix.entity.UserBankConnection;
import com.wealthix.repository.TransactionRepository;
import com.wealthix.repository.UserBankConnectionRepository;
import com.wealthix.service.EncryptionService;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.*;
import com.plaid.client.model.TransactionsGetRequestOptions;
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
import java.util.concurrent.CompletableFuture;
import com.wealthix.plaid.model.UserBankConnectionResponse;
import com.wealthix.ai.model.dto.JassHybridResponseDTO;
import com.wealthix.ai.model.dto.AITransactionDTO;
import com.wealthix.ai.model.entity.WealthInsight;
import com.wealthix.ai.repository.WealthInsightRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidService {

  private final PlaidApi plaidApi;
  private final EncryptionService encryptionService;
  private final UserBankConnectionRepository connectionRepo;
  private final TransactionRepository transactionRepo;
  private final com.wealthix.autopay.service.WealthixAiClient aiClient;
  private final WealthInsightRepository insightRepo;

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
        String requestId = response.headers().get("plaid-request-id");
        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
        log.error("[Wealthix] Link token creation failed: code={} requestId={} error={}",
            response.code(), requestId, errorMsg);
        throw new RuntimeException(
            "Plaid link token creation failed (ID: " + requestId + "): " + errorMsg);
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
  public List<UserBankConnectionResponse> exchangePublicToken(
      String publicToken, String userId) {
    try {
      // 1. Exchange public token for access token
      ItemPublicTokenExchangeRequest exchangeRequest =
          new ItemPublicTokenExchangeRequest()
              .publicToken(publicToken);

      Response<ItemPublicTokenExchangeResponse> exchangeResponse =
          plaidApi.itemPublicTokenExchange(
              exchangeRequest).execute();

      if (!exchangeResponse.isSuccessful()
          || exchangeResponse.body() == null) {
        log.error("[Wealthix] Token exchange failed: code={} raw={}", 
            exchangeResponse.code(), exchangeResponse.errorBody());
        throw new RuntimeException("Failed to exchange Plaid token");
      }

      String accessToken = exchangeResponse.body().getAccessToken();
      String itemId = exchangeResponse.body().getItemId();

      // 2. Encrypt access token BEFORE saving
      String encryptedToken = encryptionService.encrypt(accessToken);

      // 3. Fetch account details (inst name, mask, subtype) and save
      List<UserBankConnection> savedEntities = fetchAndSaveAccounts(
          encryptedToken, itemId, userId, accessToken);

      // 4. Async trigger for initial AI analysis (First-Sync)
      String plaintextToken = accessToken; // alias for clarity
      CompletableFuture.runAsync(() -> {
          log.info("[Wealthix] Triggering async first-sync for user: {} itemId: {}", userId, itemId);
          syncTransactions(userId, encryptedToken);
      }).exceptionally(ex -> {
          log.warn("[Wealthix] First-sync failed for user {}: {}", userId, ex.getMessage());
          return null;
      });

      log.info("[Wealthix] Bank connection established: userId={} itemId={} connections={}",
          userId, itemId, savedEntities.size());

      // 5. Convert to Response DTOs
      return savedEntities.stream()
          .map(UserBankConnectionResponse::fromEntity)
          .collect(Collectors.toList());

    } catch (IOException e) {
      log.error("[Wealthix] Plaid API error during exchange: {}", e.getMessage());
      throw new RuntimeException("Plaid service error during token exchange");
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

      // Date range: last 180 days for deeper historical analysis
      LocalDate startDate =
          LocalDate.now().minusDays(180);
      LocalDate endDate = LocalDate.now();

      // Paginate: Plaid defaults to 100 per page; loop until all fetched
      List<com.plaid.client.model.Transaction> plaidTxs = new ArrayList<>();
      List<AccountBase> plaidAccounts = null;
      int offset = 0;
      int totalTransactions = Integer.MAX_VALUE;

      while (plaidTxs.size() < totalTransactions) {
        TransactionsGetRequest txRequest =
            new TransactionsGetRequest()
                .accessToken(accessToken)
                .startDate(startDate)
                .endDate(endDate)
                .options(new TransactionsGetRequestOptions()
                    .count(500)
                    .offset(offset));

        Response<TransactionsGetResponse> txResponse =
            plaidApi.transactionsGet(txRequest).execute();

        if (!txResponse.isSuccessful() || txResponse.body() == null) {
          log.warn("[Wealthix] Transaction sync failed at offset {}: {}", offset, txResponse.code());
          break;
        }

        if (plaidAccounts == null) {
          plaidAccounts = txResponse.body().getAccounts();
          totalTransactions = txResponse.body().getTotalTransactions();
        }

        List<com.plaid.client.model.Transaction> page = txResponse.body().getTransactions();
        if (page.isEmpty()) break;
        plaidTxs.addAll(page);
        offset += page.size();
      }

      if (plaidAccounts == null) {
        log.warn("[Wealthix] Transaction sync returned no data for user {}", userId);
        return;
      }

      // Update balances for all accounts in this item
      for (AccountBase plaidAccount : plaidAccounts) {
          connectionRepo.findByPlaidAccountIdAndUserId(
              plaidAccount.getAccountId(), UUID.fromString(userId))
              .ifPresent(conn -> {
                  conn.setCurrentBalance(plaidAccount.getBalances().getCurrent());
                  conn.setCreditLimit(plaidAccount.getBalances().getLimit());
                  connectionRepo.save(conn);
              });
      }

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
            .transactionDate(plaidTx.getDate())
            .build();

        transactionRepo.save(tx);
        saved++;
      }

      // Log count only — no transaction details
      log.info("[Wealthix] Sync complete: userId={}" +
          " saved={} total={}",
          userId, saved, plaidTxs.size());

      // Push to AI for RAG indexing
      List<com.wealthix.ai.model.dto.AITransactionDTO> aiTxs = getTransactionsForAI(userId);
      // ingestTransactions still uses Map for now as it's a generic ingest, but we can update it too if needed
      // For now focusing on analyzeTransactions
      
      // Trigger full Jass Analysis and store report
      aiClient.analyzeTransactions(userId, aiTxs, null).ifPresent(report -> {
          try {
              WealthInsight insight = new WealthInsight();
              insight.setUserId(UUID.fromString(userId));
              insight.setWealthTip(report.getStandardReport()); // Use standardReport field
              insight.setAnalysisId(UUID.fromString(report.getAnalysisId()));
              
              // Jass 2.0 fields
              insight.setFinancialHealthScore(report.getHealthScore());
              // For Legacy compatibility or future expansion, we can map spending_velocity
              // but for now focusing on the health score and ghost count
              insight.setGhostSubscriptions(report.getGhostSubscriptions() != null ? report.getGhostSubscriptions().size() : 0);
              
              // Record which model was used and the confidence score for transparency
              insight.setModelUsed(report.getModelUsed());
              insight.setRouterConfidenceScore(report.getComplexityScore());

              insightRepo.save(insight);
              log.info("[Wealthix] New financial insight stored (analysisId={}) for user={}", 
                  report.getAnalysisId(), userId);
          } catch (Exception e) {
              log.warn("[Wealthix] Failed to save AI insight: {}", e.getMessage());
          }
      });

    } catch (Exception e) {
      log.warn("[Wealthix] Sync error for user {}: {}",
          userId, e.getMessage());
    }
  }

  /**
   * Triggers sync for all connections sharing a Plaid item_id
   * Triggered by webhook SYNC_UPDATES_AVAILABLE
   */
  public void syncByItemId(String itemId) {
    var connections = connectionRepo.findByItemId(itemId);
    if (connections.isEmpty()) {
       log.warn("[Wealthix Webhook] No connections found for item_id: {}", itemId);
       return;
    }

    String encryptedToken = connections.get(0).getEncryptedAccessToken();
    String userId = connections.get(0).getUserId().toString();

    log.info("[Wealthix Webhook] Triggering sync for item={} user={}", itemId, userId);
    syncTransactions(userId, encryptedToken);
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
      return connectionRepo.findByUserId(UUID.fromString(userId))
          .stream()
          .filter(UserBankConnection::isActive)
          .collect(Collectors.toList());
  }

  public void disconnectBank(UUID connectionId, String userId) {
      UserBankConnection conn = connectionRepo.findByIdAndUserId(connectionId, UUID.fromString(userId))
              .orElseThrow(() -> new EntityNotFoundException("Connection not found"));
      connectionRepo.delete(conn);
  }

  public void syncAllConnections(String userId) {
      List<UserBankConnection> connections = connectionRepo.findByUserId(UUID.fromString(userId));
      Set<String> uniqueTokens = connections.stream()
              .map(UserBankConnection::getEncryptedAccessToken)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
              
      log.info("[Wealthix] Syncing {} unique bank items for user {}", uniqueTokens.size(), userId);
      for (String token : uniqueTokens) {
          syncTransactions(userId, token);
      }
  }

  /**
   * Builds a full financial position summary for the user.
   */
    public com.wealthix.dto.FinancialSummaryDTO getFinancialSummary(String userId) {
        List<UserBankConnection> connections = connectionRepo.findByUserId(UUID.fromString(userId));

        double totalAssets = 0;
        double totalLiabilities = 0;
        double totalCreditLimit = 0;
        List<com.wealthix.dto.AccountSummaryDTO> accounts = new ArrayList<>();

        for (UserBankConnection conn : connections) {
            double balance = conn.getCurrentBalance() != null ? conn.getCurrentBalance() : 0.0;
            String type = conn.getAccountType() != null ? conn.getAccountType() : "CHECKING";

            Double utilization = null;
            Double creditLimit = conn.getCreditLimit();

            if ("CREDIT".equals(type)) {
                double absBalance = Math.abs(balance);
                totalLiabilities += absBalance;
                if (creditLimit != null && creditLimit > 0) {
                    totalCreditLimit += creditLimit;
                    utilization = Math.round((absBalance / creditLimit) * 1000.0) / 10.0;
                }
            } else {
                totalAssets += balance;
            }

            accounts.add(new com.wealthix.dto.AccountSummaryDTO(
                    conn.getPlaidAccountId(),
                    conn.getAccountName() != null ? conn.getAccountName() : conn.getInstitutionName(),
                    conn.getInstitutionName(),
                    type,
                    balance,
                    creditLimit,
                    utilization));
        }

        return new com.wealthix.dto.FinancialSummaryDTO(totalAssets, totalLiabilities, totalCreditLimit, accounts);
    }

    /**
     * Fetches a 90-day window of transactions, maps them to AITransactionDTO,
     * and prepares the payload for the Python Hybrid AI Service.
     * Supports both userId or itemId for flexibility in chat context.
     */
    public List<com.wealthix.ai.model.dto.AITransactionDTO> getTransactionsForAI(String identifier) {
        List<UserBankConnection> connections;
        try {
            // 1. Try resolving by userId first (UUID format)
            connections = connectionRepo.findByUserId(UUID.fromString(identifier));
        } catch (IllegalArgumentException e) {
            // 2. Fallback: try by itemId if it's not a UUID
            connections = connectionRepo.findByItemId(identifier);
        }

        if (connections.isEmpty()) {
            log.warn("[Wealthix] No bank connections found for AI request (id={})", identifier);
            return List.of();
        }

        List<com.wealthix.ai.model.dto.AITransactionDTO> aiTransactions = new ArrayList<>();

        for (UserBankConnection conn : connections) {
            try {
                String accessToken = encryptionService.decrypt(conn.getEncryptedAccessToken());
                
                // Fetch last 180 days for better historical context
                LocalDate startDate = LocalDate.now().minusDays(180);
                LocalDate endDate = LocalDate.now();

                TransactionsGetRequest request = new TransactionsGetRequest()
                        .accessToken(accessToken)
                        .startDate(startDate)
                        .endDate(endDate);

                Response<TransactionsGetResponse> response = plaidApi.transactionsGet(request).execute();

                if (response.isSuccessful()) {
                    TransactionsGetResponse body = response.body();
                    if (body != null) {
                        for (com.plaid.client.model.Transaction tx : body.getTransactions()) {
                            String category = "UNCATEGORIZED";
                            PersonalFinanceCategory pfc = tx.getPersonalFinanceCategory();
                            if (pfc != null) {
                                category = pfc.getPrimary();
                            }

                            Map<String, String> location = new HashMap<>();
                            if (tx.getLocation() != null) {
                                if (tx.getLocation().getCity() != null) location.put("city", tx.getLocation().getCity());
                                if (tx.getLocation().getRegion() != null) location.put("region", tx.getLocation().getRegion());
                                if (tx.getLocation().getCountry() != null) location.put("country", tx.getLocation().getCountry());
                            }

                            aiTransactions.add(com.wealthix.ai.model.dto.AITransactionDTO.builder()
                                .id(tx.getTransactionId())
                                .amount(tx.getAmount())
                                .date(tx.getDate().toString())
                                .description(tx.getName())
                                .category(category)
                                .merchant(tx.getMerchantName())
                                .pending(tx.getPending())
                                .accountSubtype(conn.getAccountSubtype())
                                .location(location)
                                .build());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[Wealthix] Failed to fetch transactions for AI: id={}, account={}", identifier, conn.getAccountName());
            }
        }
        return aiTransactions;
    }
}
