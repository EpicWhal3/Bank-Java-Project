package org.bank.core.services;

import lombok.RequiredArgsConstructor;
import org.bank.memory.DTO_entities.AccountEventDto;
import org.bank.memory.DTO_entities.AccountDTO;
import org.bank.memory.DTO_entities.TransactionDTO;
import org.bank.memory.entities.transactions.Transaction;
import org.bank.memory.entities.transactions.TransactionStatus;
import org.bank.memory.entities.transactions.TransactionTypes;
import org.bank.memory.entities.accounts.Account;
import org.bank.memory.entities.users.User;
import org.bank.memory.exceptions.AccountExceptions;
import org.bank.memory.exceptions.UserExceptions;
import org.bank.core.mappers.AccountMapper;
import org.bank.core.mappers.TransactionMapper;
import org.bank.memory.repos.AccountRepository;
import org.bank.memory.repos.TransactionRepository;
import org.bank.memory.repos.UserRepository;
import org.bank.memory.requestEntites.TransferRequestBody;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Класс для работы со счётами пользователей.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final KafkaEventProducer kafkaProducer;

    /**
     * Метод для создания счёта.
     *
     * @param login логин пользователя
     */
    @Transactional
    public AccountDTO createAccount(String login) throws Exception {
        User user = userRepository.findByLogin(login);
        if (user == null) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден");
        }
        Account account = new Account(user);
        accountRepository.save(account);

        AccountEventDto event = AccountEventDto.fromAccount(account, "CREATED");
        kafkaProducer.sendAccountEvent(event);
        return accountMapper.toAccountDTO(account);
    }

    /**
     * Метод для пополнения счёта.
     *
     * @param accountId идентификатор счёта
     * @param amount    сумма для пополнения
     * @throws AccountExceptions исключение, если счёт не найден
     */
    @Transactional
    public AccountDTO deposit(@NonNull Long accountId, double amount) throws Exception {
        Optional<Account> account = accountRepository.findById(accountId);
        if (account.isEmpty()) {
            throw AccountExceptions.AccountNotFoundException("Счет не найден");
        }
        double oldBalance = account.get().getBalance();
        account.get().deposit(amount);
        Transaction tx = new Transaction(TransactionTypes.DEPOSIT,
                TransactionStatus.SUCCESS, amount, "Пополнение счета", account.get());
        account.get().addTransaction(tx);
        transactionRepository.save(tx);

        AccountEventDto event = AccountEventDto.fromAccount(account.get(), "UPDATED");
        event.setChanges(new AccountEventDto.FieldChanges(
                "balance",
                oldBalance,
                account.get().getBalance()));
        event.setLastTransaction(new AccountEventDto.TransactionSummary(
                tx.getId(),
                tx.getType(),
                tx.getAmount()));
        kafkaProducer.sendAccountEvent(event);
        return accountMapper.toAccountDTO(account.get());
    }

    /**
     * Метод для снятия средств со счёта.
     *
     * @param accountId идентификатор счёта
     * @param amount    сумма для снятия
     * @throws AccountExceptions исключение, если счёт не найден
     */
    @Transactional
    public AccountDTO withdraw(@NonNull Long accountId, double amount) throws Exception {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            throw AccountExceptions.AccountNotFoundException("Счет не найден");
        }
        double oldBalance = account.getBalance();
        account.withdraw(amount);
        Transaction tx = new Transaction(TransactionTypes.WITHDRAW,
                TransactionStatus.SUCCESS, amount, "Снятие со счета", account);
        account.addTransaction(tx);
        transactionRepository.save(tx);

        AccountEventDto event = AccountEventDto.fromAccount(account, "UPDATED");
        event.setChanges(new AccountEventDto.FieldChanges(
                "balance",
                oldBalance,
                account.getBalance()));
        event.setLastTransaction(new AccountEventDto.TransactionSummary(
                tx.getId(),
                tx.getType(),
                tx.getAmount()));
        kafkaProducer.sendAccountEvent(event);

        return accountMapper.toAccountDTO(account);
    }

    /**
     * Метод для перевода средств между счетами.
     *
     * @param transferRequest тело запроса на перевод средств
     * @throws AccountExceptions исключение, если счёт не найден
     */
    @Transactional
    public ArrayList<AccountDTO> transfer(TransferRequestBody transferRequest)
            throws Exception {
        Long fromId = transferRequest.getFromAccountId();
        Long toId = transferRequest.getToAccountId();
        double amount = transferRequest.getAmount();

        Account fromAccount = accountRepository.findById(fromId).orElse(null);
        Account toAccount = accountRepository.findById(toId).orElse(null);

        if (fromAccount == null || toAccount == null) {
            throw AccountExceptions.AccountNotFoundException("Счет не найден");
        }

        User sender = userRepository.findByLogin(fromAccount.getOwner().getLogin());
        User recipient = userRepository.findByLogin(toAccount.getOwner().getLogin());
        if (sender == null || recipient == null) {
            throw UserExceptions.UserNotFoundException("Пользователь не найден");
        }

        double commissionRate = CommissionCalc(sender, recipient);
        double totalAmount = amount + (amount * commissionRate);

        double fromOldBalance = fromAccount.getBalance();
        double toOldBalance = toAccount.getBalance();

        fromAccount.withdraw(totalAmount);
        toAccount.deposit(amount);

        Transaction txFrom = new Transaction(TransactionTypes.TRANSFER,
                TransactionStatus.SUCCESS, totalAmount, "Перевод средств на счёт " + toAccount.getId(),
                fromAccount);
        Transaction txTo = new Transaction(TransactionTypes.TRANSFER,
                TransactionStatus.SUCCESS, amount, "Получение средств со счёта " + fromAccount.getId(),
                toAccount);

        transactionRepository.save(txFrom);
        transactionRepository.save(txTo);
        AccountEventDto fromEvent = AccountEventDto.fromAccount(fromAccount, "UPDATED");
        fromEvent.setChanges(new AccountEventDto.FieldChanges(
                "balance",
                fromOldBalance,
                fromAccount.getBalance()));
        fromEvent.setLastTransaction(new AccountEventDto.TransactionSummary(
                txFrom.getId(),
                txFrom.getType(),
                txFrom.getAmount()));

        AccountEventDto toEvent = AccountEventDto.fromAccount(toAccount, "UPDATED");
        toEvent.setChanges(new AccountEventDto.FieldChanges(
                "balance",
                toOldBalance,
                toAccount.getBalance()));
        toEvent.setLastTransaction(new AccountEventDto.TransactionSummary(
                txTo.getId(),
                txTo.getType(),
                txTo.getAmount()));

        kafkaProducer.sendAccountEvent(fromEvent);
        kafkaProducer.sendAccountEvent(toEvent);

        return new ArrayList<>(List.of(accountMapper.toAccountDTO(fromAccount),
                accountMapper.toAccountDTO(toAccount)));
    }

    /**
     * Метод для проверки баланса счёта.
     *
     * @param id идентификатор счёта
     */
    @Transactional(readOnly = true)
    public AccountDTO checkBalance(@NonNull Long id) throws AccountExceptions {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty()) {
            throw AccountExceptions.AccountNotFoundException("Счет не найден");
        }
        return new AccountDTO(account.get().getId(), account.get().getBalance());
    }

    private double CommissionCalc(User sender, User recipient) {
        double commissionRate = 0.0;
        if (!sender.getLogin().equals(recipient.getLogin())) {
            commissionRate = sender.getFriends().contains(recipient) ? 0.03 : 0.10;
        }
        return commissionRate;
    }

    /**
     * Метод для получения всех счетов.
     *
     * @return список счетов пользователя
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() throws AccountExceptions {
        List<Account> accounts = accountRepository.findAll();
        if (accounts.isEmpty()) {
            throw AccountExceptions.NoAccounts("Счета не найдены");
        }
        return accountMapper.toAccountDTOs(accounts);
    }

    /**
     * Метод для получения всех операций.
     *
     * @return список операций
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() throws AccountExceptions {
        List<Transaction> transactions = transactionRepository.findAll();
        if (transactions.isEmpty()) {
            throw AccountExceptions.NoTransactionsException("Нет операций по счету");
        }
        return transactionMapper.toTransactionDTOs(transactions);
    }

    /**
     * Метод для получения всех операций по идентификатору счета.
     *
     * @param id идентификатор счета
     * @return список операций
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactionsById(Long id) throws AccountExceptions {
        List<Transaction> transactions = transactionRepository.findByAccountId(id);
        if (transactions.isEmpty()) {
            throw AccountExceptions.NoTransactionsException("Нет операций по счету");
        }
        return transactionMapper.toTransactionDTOs(transactions);
    }

    /**
     * Метод для получения всех операций по типу операции.
     *
     * @param transactionTypes тип операции
     * @return список операций
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactionsByType(TransactionTypes transactionTypes) throws AccountExceptions {
        List<Transaction> transactions = transactionRepository.findTransactionByType(transactionTypes);
        if (transactions.isEmpty()) {
            throw AccountExceptions.NoTransactionsException("Нет операций по счету");
        }
        return transactionMapper.toTransactionDTOs(transactions);
    }

    /**
     * Метод для получения всех операций по идентификатору счета и типу операции.
     *
     * @param id               идентификатор счета
     * @param transactionTypes тип операции
     * @return список операций
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactionsByIdAndType(Long id, TransactionTypes transactionTypes)
            throws AccountExceptions {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndType(id, transactionTypes);
        if (transactions.isEmpty()) {
            throw AccountExceptions.NoTransactionsException("Нет операций по счету");
        }
        return transactionMapper.toTransactionDTOs(transactions);
    }
}
