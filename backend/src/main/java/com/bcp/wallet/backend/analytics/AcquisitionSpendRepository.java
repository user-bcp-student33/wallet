package com.bcp.wallet.backend.analytics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcquisitionSpendRepository extends JpaRepository<AcquisitionSpend, UUID> {

    List<AcquisitionSpend> findBySpendMonth(String spendMonth);
}
