package ru.inovus.ms.rdm.sync.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.inovus.ms.rdm.util.JsonLocalDateTimeDeserializer;

import java.time.LocalDateTime;

/**
 * @author lgalimova
 * @since 28.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
public class Log {
    private Long id;
    private String refbookCode;
    private String currentVersion;
    private String newVersion;
    private String status;
    @JsonDeserialize(using = JsonLocalDateTimeDeserializer.class)
    private LocalDateTime date;
    private String message;
    private String stack;

}
