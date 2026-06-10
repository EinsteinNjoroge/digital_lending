package com.digital.lending.loanaccount.event;

import com.digital.lending.events.LoanProductConfigurationChangedEvent;
import com.digital.lending.loanaccount.model.LoanProductConfigurationProjection;
import com.digital.lending.loanaccount.repository.LoanProductConfigurationProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanProductConfigurationChangedEventListener {

    private final LoanProductConfigurationProjectionRepository repository;

    @ApplicationModuleListener
    public void onLoanProductConfigurationChanged(LoanProductConfigurationChangedEvent event) {
        LoanProductConfigurationProjection projection = repository.findById(event.loanProductId())
                .orElseGet(LoanProductConfigurationProjection::new);

        projection.setLoanProductId(event.loanProductId());
        projection.setProductCode(event.productCode());
        projection.setPartnerId(event.partnerId());
        projection.setCurrency(event.currency());
        projection.setActive(event.active());
        projection.setRepaymentDueDays(event.repaymentDueDays());
        projection.setUpdatedAt(event.occurredAt());

        repository.save(projection);
    }
}
