package com.bcp.wallet.backend.account;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedCardRepository extends JpaRepository<LinkedCard, UUID> {
}
