package com.digital.lending.loanaccount;

import com.digital.lending.events.LoanAccountOverdueEvent;
import com.digital.lending.loanaccount.dto.LoanServicingRunResponseDto;
import com.digital.lending.loanaccount.enums.IssuanceStatus;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.model.LoanAccount;
import com.digital.lending.loanaccount.model.LoanAccountAuditLog;
import com.digital.lending.loanaccount.repository.LoanAccountAuditLogRepository;
import com.digital.lending.loanaccount.repository.LoanAccountRepository;
import com.digital.lending.loanaccount.service.LoanServicingService;
import com.digital.lending.loanproduct.model.LoanProductConfiguration;
import com.digital.lending.loanproduct.repository.LoanProductConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServicingServiceTest {

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private LoanAccountAuditLogRepository loanAccountAuditLogRepository;

    @Mock
    private LoanProductConfigurationRepository loanProductConfigurationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LoanServicingService loanServicingService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        when(loanAccountAuditLogRepository.save(any(LoanAccountAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should update overdue accounts and publish one overdue event per loan")
    void shouldUpdateOverdueAccountsAndPublishEvents() {
        LoanAccount watchLoan = overdueLoan("acc_burgundy_001", "prod_001", 5);
        LoanAccount doubtfulLoan = overdueLoan("acc_burgundy_003", "prod_003", 190);

        when(loanAccountRepository.findServicingCandidates(anyCollection(), anyCollection(), any(ZonedDateTime.class)))
                .thenReturn(List.of(watchLoan, doubtfulLoan));
        when(loanAccountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanProductConfigurationRepository.findById("prod_001")).thenReturn(Optional.of(product("prod_001", "KES")));
        when(loanProductConfigurationRepository.findById("prod_003")).thenReturn(Optional.of(product("prod_003", "KES")));

        LoanServicingRunResponseDto response = loanServicingService.runServicing("test");

        assertEquals(2, response.accountsChecked());
        assertEquals(2, response.accountsUpdated());
        assertEquals(PerformanceStatus.WATCH, watchLoan.getPerformanceStatus());
        assertEquals(PerformanceStatus.DOUBTFUL, doubtfulLoan.getPerformanceStatus());

        ArgumentCaptor<LoanAccountOverdueEvent> eventCaptor = ArgumentCaptor.forClass(LoanAccountOverdueEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertEquals("WATCH", eventCaptor.getAllValues().get(0).performanceStatus());
        assertEquals("DOUBTFUL", eventCaptor.getAllValues().get(1).performanceStatus());
    }

    private LoanAccount overdueLoan(String id, String productId, int daysPastDue) {
        LoanAccount loan = new LoanAccount();
        loan.setId(id);
        loan.setAccountNumber("LN-" + id);
        loan.setProfileId("prof_burgundy_001");
        loan.setLoanProductId(productId);
        loan.setIssuanceStatus(IssuanceStatus.ACTIVE);
        loan.setPerformanceStatus(PerformanceStatus.ACTIVE);
        loan.setOutstandingPrincipal(new BigDecimal("1000.00"));
        loan.setRepaymentDueAt(ZonedDateTime.now().minusDays(daysPastDue));
        loan.setDaysPastDue(0);
        return loan;
    }

    private LoanProductConfiguration product(String id, String currency) {
        LoanProductConfiguration product = new LoanProductConfiguration();
        product.setId(id);
        product.setCurrency(currency);
        return product;
    }
}
