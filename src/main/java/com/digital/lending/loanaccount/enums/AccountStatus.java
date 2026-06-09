package com.digital.lending.loanaccount.enums;

public enum AccountStatus {
    ACTIVE,      // Performing clean, up-to-date settlements
    WATCH,       // 30+ Days Past Due (DPD) late trace flags
    DOUBTFUL,    // 180-360 DPD. Severe delinquency threat layer (CRB reporting ready)
    SETTLED      // Fully cleared obligation line
}