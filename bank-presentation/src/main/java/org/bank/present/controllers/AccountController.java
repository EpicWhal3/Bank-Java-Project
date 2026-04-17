package org.bank.present.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.bank.core.services.AccountService;
import org.bank.memory.DTO_entities.AccountDTO;
import org.bank.memory.DTO_entities.TransactionDTO;
import org.bank.memory.entities.transactions.TransactionTypes;
import org.bank.memory.exceptions.AccountExceptions;
import org.bank.memory.requestEntites.TransferRequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

        private final AccountService accountService;

        @Operation(summary = "Создание нового счёта")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Счёт успешно создан\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при создании счёта\n")
        })
        @PostMapping("/create")
        public ResponseEntity<AccountDTO> createAccount(@RequestBody String login) throws Exception {
                AccountDTO accountDTO = accountService.createAccount(login);
                return new ResponseEntity<>(accountDTO, HttpStatus.CREATED);
        }

        @Operation(summary = "Пополнение счета")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Счёт успешно пополнен\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при пополнении счета\n")
        })
        @PostMapping("/{id}/deposit")
        public ResponseEntity<AccountDTO> deposit(@PathVariable("id") @NonNull Long id,
                        @RequestParam("amount") double amount)
                        throws Exception {
                AccountDTO accountDTO = accountService.deposit(id, amount);
                return new ResponseEntity<>(accountDTO, HttpStatus.OK);

        }

        @Operation(summary = "Снятие средств с счёта")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Средства сняты\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при снятии средств\n")
        })
        @PostMapping("/{accountId}/withdraw")
        public ResponseEntity<AccountDTO> withdraw(@PathVariable("accountId") @NonNull Long accountId,
                        @RequestParam("amount") double amount)
                        throws Exception {
                AccountDTO accountDTO = accountService.withdraw(accountId, amount);
                return new ResponseEntity<>(accountDTO, HttpStatus.OK);
        }

        @Operation(summary = "Перевод средств между счетами")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен\n"),
                        @ApiResponse(responseCode = "400", description = "Ошибка при переводе средств\n")
        })
        @PostMapping("/transfer")
        public ResponseEntity<ArrayList<AccountDTO>> transfer(TransferRequestBody transferRequestBody)
                        throws Exception {
                ArrayList<AccountDTO> accountDTOS = accountService.transfer(transferRequestBody);
                return new ResponseEntity<>(accountDTOS, HttpStatus.OK);

        }

        @Operation(summary = "Получить баланс счета")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Баланс успешно получен\n"),
                        @ApiResponse(responseCode = "404", description = "Счёт не найден\n")
        })
        @GetMapping("/{accountId}/balance")
        public ResponseEntity<AccountDTO> getBalance(@PathVariable("accountId") @NonNull Long accountId)
                        throws AccountExceptions {
                AccountDTO accountDTO = accountService.checkBalance(accountId);
                return new ResponseEntity<>(accountDTO, HttpStatus.OK);

        }

        /**
         * Получение всех счетов системы.
         *
         * @return ResponseEntity со списком всех счетов
         */
        @Operation(summary = "Получить все счета системы")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Все счета успешно получены\n"),
                        @ApiResponse(responseCode = "404", description = "Счета не найдены\n")
        })
        @GetMapping
        public ResponseEntity<List<AccountDTO>> getAllAccounts() throws AccountExceptions {
                List<AccountDTO> accountDTOs = accountService.getAllAccounts();
                return new ResponseEntity<>(accountDTOs, HttpStatus.OK);
        }

        /**
         * Получение всех операций.
         */
        @Operation(summary = "Получить все операции")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Операции успешно получены\n"),
                        @ApiResponse(responseCode = "404", description = "Операции не найдены\n")
        })
        @GetMapping("/transactions")
        public ResponseEntity<List<TransactionDTO>> getAllTransactions() throws AccountExceptions {
                List<TransactionDTO> transactionDTOs = accountService.getAllTransactions();
                return new ResponseEntity<>(transactionDTOs, HttpStatus.OK);
        }

        /**
         * Получение всех операций с фильтрацией по типу и accountId.
         *
         * @param accountId идентификатор счета
         * @return ResponseEntity со списком операций
         */
        @Operation(summary = "Получить все операции с фильтрацией по accountId")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Операции успешно получены\n"),
                        @ApiResponse(responseCode = "404", description = "Операции не найдены\n")
        })
        @GetMapping("/{account_id}/transactions")
        public ResponseEntity<List<TransactionDTO>> getAllTransactionsById(@PathVariable("account_id") Long accountId)
                        throws AccountExceptions {
                List<TransactionDTO> transactionDTOs = accountService.getAllTransactionsById(accountId);
                return new ResponseEntity<>(transactionDTOs, HttpStatus.OK);
        }

        /**
         * Получение всех операций с фильтрацией по типу.
         * \
         *
         * @param transactionTypes тип операции
         * @return ResponseEntity со списком операций
         */
        @Operation(summary = "Получить все операции с фильтрацией по типу")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Операции успешно получены\n"),
                        @ApiResponse(responseCode = "404", description = "Операции не найдены\n")
        })
        @GetMapping("/transactions/{type}")
        public ResponseEntity<List<TransactionDTO>> getAllTransactionsByType(
                        @PathVariable("type") TransactionTypes transactionTypes)
                        throws AccountExceptions {
                List<TransactionDTO> transactionDTOs = accountService.getAllTransactionsByType(transactionTypes);
                return new ResponseEntity<>(transactionDTOs, HttpStatus.OK);
        }

        @GetMapping("/{account_id}/transactions/{type}")
        public ResponseEntity<List<TransactionDTO>> getAllTransactionsByIdAndType(
                        @PathVariable("account_id") Long accountId,
                        @PathVariable("type") TransactionTypes transactionTypes)
                        throws AccountExceptions {
                List<TransactionDTO> transactionDTOs = accountService.getAllTransactionsByIdAndType(accountId,
                                transactionTypes);
                return new ResponseEntity<>(transactionDTOs, HttpStatus.OK);
        }
}
