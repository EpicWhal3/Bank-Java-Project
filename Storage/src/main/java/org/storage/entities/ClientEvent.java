package org.storage.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bank.memory.DTO_entities.ClientEventDto;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private LocalDateTime eventTime;

    private Long userId;
    private String name;
    private Integer age;
    private String gender;
    private String hairColor;

    private String accountIds;
    private String friendLogins;
    private String changedField;
    private String oldValue;
    private String newValue;


    public static @NonNull ClientEvent fromDTO(@NonNull ClientEventDto dto) {
        ClientEvent entity = new ClientEvent();
        entity.setEventType(dto.getEventType());
        entity.setEventTime(dto.getEventTime());
        entity.setUserId(dto.getUserId());
        entity.setName(dto.getName());
        entity.setAge(dto.getAge());
        entity.setGender(dto.getGender());
        entity.setHairColor(dto.getHairColor() != null ? dto.getHairColor().toString() : null);
        return entity;
    }
}

