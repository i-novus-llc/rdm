package ru.i_novus.ms.rdm.sync.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
    private LocalDateTime date;
    private String message;
    private String stack;

}
