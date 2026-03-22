package com.wealthix.plaid.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import com.wealthix.ai.model.dto.AITransactionDTO;
import com.wealthix.ai.model.dto.JassHybridResponseDTO;
import com.wealthix.ai.model.entity.WealthInsight;
import com.wealthix.ai.repository.WealthInsightRepository;
import com.wealthix.autopay.service.WealthixAiClient;
import com.wealthix.entity.UserBankConnection;
import com.wealthix.repository.TransactionRepository;
import com.wealthix.repository.UserBankConnectionRepository;
import com.wealthix.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class PlaidGhostSubscriptionTest {

    @Mock
    private PlaidApi plaidApi;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private UserBankConnectionRepository connectionRepo;

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private WealthixAiClient aiClient;

    @Mock
    private WealthInsightRepository insightRepo;

    @InjectMocks
    private PlaidService plaidService;

    private String userId = UUID.randomUUID().toString();
    private String encryptedToken = "encrypted-token";
    private String plainToken = "plain-token";

    @Test
    @SuppressWarnings("unchecked")
    void test90DaySyncAndGhostLogic() throws IOException {
        // Arrange
        UserBankConnection conn = new UserBankConnection();
        conn.setUserId(UUID.fromString(userId));
        conn.setEncryptedAccessToken(encryptedToken);
        conn.setPlaidAccountId("acc_123");
        conn.setAccountName("Checking");
        conn.setAccountSubtype("checking");
        
        when(connectionRepo.findByUserId(UUID.fromString(userId)))
            .thenReturn(Collections.singletonList(conn));
        when(encryptionService.decrypt(encryptedToken)).thenReturn(plainToken);

        // Mock Plaid API Response (TransactionsGet)
        TransactionsGetResponse plaidRes = new TransactionsGetResponse();
        plaidRes.setTransactions(Arrays.asList(
            createPlaidTx("tx_1", 14.99, "FitStream Plus", LocalDate.now().minusDays(5)),
            createPlaidTx("tx_2", 14.99, "FitStream Plus", LocalDate.now().minusDays(35)),
            createPlaidTx("tx_3", 14.99, "FitStream Plus", LocalDate.now().minusDays(65))
        ));
        plaidRes.setAccounts(Collections.emptyList());
        
        Call<TransactionsGetResponse> call = mock(Call.class);
        when(plaidApi.transactionsGet(any(TransactionsGetRequest.class))).thenReturn(call);
        when(call.execute()).thenReturn(Response.success(plaidRes));

        // Mock AI Client Response (Using JassHybridResponseDTO)
        UUID analysisId = UUID.randomUUID();
        JassHybridResponseDTO hybridResponse = JassHybridResponseDTO.builder()
            .analysisId(analysisId.toString())
            .standardReport("Standard Gemini Analysis")
            .healthScore(85)
            .ghostSubscriptions(List.of("FitStream Plus"))
            .build();

        when(aiClient.analyzeTransactions(eq(userId), anyList(), any()))
            .thenReturn(Optional.of(hybridResponse));

        // Act
        plaidService.syncTransactions(userId, encryptedToken);

        // Assert 1: Data Window (180 days)
        ArgumentCaptor<TransactionsGetRequest> requestCaptor = ArgumentCaptor.forClass(TransactionsGetRequest.class);
        verify(plaidApi, atLeast(2)).transactionsGet(requestCaptor.capture());
        
        LocalDate expectedStartDate = LocalDate.now().minusDays(180);
        assertTrue(requestCaptor.getAllValues().stream().allMatch(r -> r.getStartDate().equals(expectedStartDate)));

        // Assert 2: Mapping to AITransactionDTO
        ArgumentCaptor<List<AITransactionDTO>> aiTxsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiClient).analyzeTransactions(eq(userId), aiTxsCaptor.capture(), any());
        
        List<AITransactionDTO> aiTxs = aiTxsCaptor.getValue();
        assertEquals(3, aiTxs.size());

        // Assert 3: Correlation ID and Health Score
        ArgumentCaptor<WealthInsight> insightCaptor = ArgumentCaptor.forClass(WealthInsight.class);
        verify(insightRepo).save(insightCaptor.capture());
        
        WealthInsight savedInsight = insightCaptor.getValue();
        assertEquals(analysisId, savedInsight.getAnalysisId());
        assertEquals(85, savedInsight.getFinancialHealthScore());
        assertEquals(1, savedInsight.getGhostSubscriptions());
        
        System.out.println("[JUnit] Hybrid Ghost Test Passed: Standard + Jass 2.0 metrics saved.");
    }

    private com.plaid.client.model.Transaction createPlaidTx(String id, double amount, String name, LocalDate date) {
        com.plaid.client.model.Transaction tx = new com.plaid.client.model.Transaction();
        tx.setTransactionId(id);
        tx.setAmount(amount);
        tx.setName(name);
        tx.setDate(date);
        tx.setPending(false);
        tx.setAccountId("acc_123");
        
        PersonalFinanceCategory pfc = new PersonalFinanceCategory();
        pfc.setPrimary("ENTERTAINMENT");
        tx.setPersonalFinanceCategory(pfc);
        
        return tx;
    }
}
