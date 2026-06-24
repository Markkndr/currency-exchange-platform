package com.currencyexchange.service;

import com.currencyexchange.dto.transactions.CreateTransactionRequestDTO;
import com.currencyexchange.dto.transactions.TransactionDTO;
import com.currencyexchange.entity.Transaction;
import com.currencyexchange.entity.User;
import com.currencyexchange.entity.Wallet;
import com.currencyexchange.exception.InvalidTransactionException;
import com.currencyexchange.exception.TransactionNotFoundException;
import com.currencyexchange.repository.TransactionRepository;
import com.currencyexchange.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("bob@example.com");
    }

    private Transaction txOwnedBy(Long id, User owner) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setUser(owner);
        t.setTransactionType("EXCHANGE");
        t.setFromAmount(new BigDecimal("100.00"));
        t.setToAmount(new BigDecimal("90.00"));
        t.setStatus("PENDING");
        return t;
    }

    @Test
    @DisplayName("getUserTransactions maps the user's transactions to DTOs")
    void listsUserTransactions() {
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(txOwnedBy(1L, user), txOwnedBy(2L, user)));

        List<TransactionDTO> result = transactionService.getUserTransactions(7L);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.getUserId()).isEqualTo(7L));
    }

    @Test
    @DisplayName("getUserTransactionsByType delegates to the typed query")
    void listsUserTransactionsByType() {
        when(transactionRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(7L, "EXCHANGE"))
                .thenReturn(List.of(txOwnedBy(1L, user)));

        List<TransactionDTO> result = transactionService.getUserTransactionsByType(7L, "EXCHANGE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("EXCHANGE");
    }

    @Test
    @DisplayName("getUserTransaction returns the transaction when owned by the user")
    void getsOwnedTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(txOwnedBy(1L, user)));

        TransactionDTO dto = transactionService.getUserTransaction(7L, 1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("getUserTransaction hides a transaction owned by another user")
    void getForeignTransactionThrowsNotFound() {
        User other = new User();
        other.setId(99L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(txOwnedBy(1L, other)));

        assertThatThrownBy(() -> transactionService.getUserTransaction(7L, 1L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("getUserTransaction throws when the transaction is missing")
    void getMissingTransactionThrowsNotFound() {
        when(transactionRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getUserTransaction(7L, 123L))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("createTransaction persists a PENDING transaction with a reference for the user")
    void createsTransaction() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setTransactionType("exchange");
        request.setFromAmount(new BigDecimal("100.00"));
        request.setToAmount(new BigDecimal("90.00"));
        request.setExchangeRateUsed(new BigDecimal("0.90"));

        when(userService.getUserById(7L)).thenReturn(user);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO dto = transactionService.createTransaction(7L, request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTransactionType()).isEqualTo("EXCHANGE");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getTransactionReference()).startsWith("TXN-");
        assertThat(saved.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTransactionType()).isEqualTo("EXCHANGE");
    }

    @Test
    @DisplayName("createTransaction attaches a wallet the user owns")
    void createsTransactionWithOwnedWallet() {
        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setUser(user);

        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setTransactionType("WITHDRAWAL");
        request.setFromWalletId(5L);
        request.setFromAmount(new BigDecimal("50.00"));
        request.setToAmount(new BigDecimal("50.00"));

        when(userService.getUserById(7L)).thenReturn(user);
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO dto = transactionService.createTransaction(7L, request);

        assertThat(dto.getFromWalletId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("createTransaction rejects a wallet owned by another user")
    void createTransactionRejectsForeignWallet() {
        User other = new User();
        other.setId(99L);
        Wallet foreignWallet = new Wallet();
        foreignWallet.setId(5L);
        foreignWallet.setUser(other);

        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setTransactionType("WITHDRAWAL");
        request.setFromWalletId(5L);
        request.setFromAmount(new BigDecimal("50.00"));
        request.setToAmount(new BigDecimal("50.00"));

        when(userService.getUserById(7L)).thenReturn(user);
        when(walletRepository.findById(5L)).thenReturn(Optional.of(foreignWallet));

        assertThatThrownBy(() -> transactionService.createTransaction(7L, request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("5");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction rejects a wallet that does not exist")
    void createTransactionRejectsMissingWallet() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setTransactionType("DEPOSIT");
        request.setToWalletId(404L);
        request.setFromAmount(new BigDecimal("50.00"));
        request.setToAmount(new BigDecimal("50.00"));

        when(userService.getUserById(7L)).thenReturn(user);
        when(walletRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(7L, request))
                .isInstanceOf(InvalidTransactionException.class);

        verify(transactionRepository, never()).save(any());
    }
}
