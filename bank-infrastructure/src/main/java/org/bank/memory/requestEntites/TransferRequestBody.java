package org.bank.memory.requestEntites;

import org.springframework.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TransferRequestBody {
    private @NonNull Long fromAccountId;
    private @NonNull Long toAccountId;
    private double amount;
}
