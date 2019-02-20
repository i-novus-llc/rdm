package ru.inovus.ms.rdm.service;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.ws.rs.core.Response;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncRestImpl implements RdmSyncRest {

    private JdbcTemplate jdbcTemplate;

    @Override
    public Response update() {
        return null;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
