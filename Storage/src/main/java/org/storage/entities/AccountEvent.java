package org.storage.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bank.memory.DTO_entities.AccountEventDto;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private LocalDateTime eventTime;

    private Long accountId;
    private Double balance;
    private String ownerLogin;

    private String changedField;
    private String oldValue;
    private String newValue;

    private Long transactionId;
    private String transactionType;
    private Double transactionAmount;

    public static @NonNull AccountEvent fromDTO(@NonNull AccountEventDto dto) {
        AccountEvent entity = new AccountEvent();
        entity.setEventType(dto.getEventType());
        entity.setEventTime(dto.getEventTime());
        entity.setAccountId(dto.getAccountId());
        entity.setOwnerLogin(dto.getOwnerLogin());
        entity.setBalance(dto.getBalance());

        if (dto.getChanges() != null) {
            entity.setChangedField(dto.getChanges().getChangedField());
            entity.setOldValue(dto.getChanges().getOldValue().toString());
            entity.setNewValue(dto.getChanges().getNewValue().toString());
        }

        if (dto.getLastTransaction() != null) {
            entity.setTransactionId(dto.getLastTransaction().getId());
            entity.setTransactionType(dto.getLastTransaction().getType().toString());
            entity.setTransactionAmount(dto.getLastTransaction().getAmount());
        }

        return entity;
    }
}
