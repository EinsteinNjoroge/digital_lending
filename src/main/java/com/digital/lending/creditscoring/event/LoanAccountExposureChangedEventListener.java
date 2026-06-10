package com.digital.lending.creditscoring.event;

import com.digital.lending.creditscoring.model.LoanAccountExposureProjection;
import com.digital.lending.creditscoring.repository.LoanAccountExposureProjectionRepository;
import com.digital.lending.events.LoanAccountExposureChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LoanAccountExposureChangedEventListener {

    private final LoanAccountExposureProjectionRepository repository;

    @ApplicationModuleListener
    public void onLoanAccountExposureChanged(LoanAccountExposureChangedEvent event) {
        LoanAccountExposureProjection projection = repository.findById(event.loanAccountId())
                .orElseGet(LoanAccountExposureProjection::new);

        projection.setLoanAccountId(event.loanAccountId());
        projection.setProfileId(event.profileId());
        projection.setAccountReference(event.accountReference());
        projection.setOutstandingPrincipal(event.outstandingPrincipal() == null ? BigDecimal.ZERO : event.outstandingPrincipal());
        projection.setExposureStatus(event.exposureStatus());
        projection.setUpdatedAt(event.occurredAt());

        repository.save(projection);
    }
}
