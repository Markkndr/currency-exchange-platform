package com.currencyexchange.repository;

import com.currencyexchange.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Wallet findByUserIdAndCurrency(Long userId, String currency);

    List<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByWalletAddress(String address);
}
