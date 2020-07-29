package ru.i_novus.ms.rdm.esnsi.smev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;


@Repository
class MsgBuffer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public String get(String msgId) {
        return jdbcTemplate.query("SELECT msg FROM esnsi_sync.msg_buffer WHERE msg_id = ?",
            (rs, rowNum) -> rs.getString(1), msgId).stream().findFirst().orElse(null);
    }

    @Transactional
    public boolean put(String msgId, String msg, LocalDateTime utcDeliveryTimestamp) {
        int n = jdbcTemplate.update("INSERT INTO esnsi_sync.msg_buffer (msg_id, msg, delivery_timestamp) VALUES (?, ?, ?) ON CONFLICT (msg_id) DO NOTHING", msgId, msg, Timestamp.valueOf(utcDeliveryTimestamp));
        return n != 0;
    }

    @Transactional
    public void remove(String messageId) {
        jdbcTemplate.update("DELETE FROM esnsi_sync.msg_buffer WHERE msg_id = ?", messageId);
    }

    @Transactional
    public int removeExpiredMessages(LocalDateTime utcBound) {
        return jdbcTemplate.update("DELETE FROM esnsi_sync.msg_buffer WHERE delivery_timestamp < ?", Timestamp.valueOf(utcBound));
    }

}
