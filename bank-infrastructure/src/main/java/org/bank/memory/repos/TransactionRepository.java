package org.bank.memory.repos;

import org.bank.memory.entities.transactions.Transaction;
import org.bank.memory.entities.transactions.TransactionTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findTransactionByType(TransactionTypes type);

    List<Transaction> findByAccountIdAndType(Long accountId, TransactionTypes type);
}