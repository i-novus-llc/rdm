package ru.inovus.ms.rdm.esnsi.smev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static ru.inovus.ms.rdm.esnsi.smev.Utils.EMPTY_INPUT_STREAM;


@Repository
class MsgBuffer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public Map.Entry<String, InputStream> get(String msgId) {
        return jdbcTemplate.query("SELECT msg, attachment FROM esnsi_sync.msg_buffer WHERE msg_id = ?",
            (rs, rowNum) -> Map.entry(rs.getString(1), rs.getBytes(2) == null ? EMPTY_INPUT_STREAM : new ByteArrayInputStream(rs.getBytes(2))),
        msgId).stream().findFirst().orElse(null);
    }

    @Transactional
    public boolean put(String msgId, String msg, LocalDateTime utcDeliveryTimestamp, byte[] attachment) {
        int n = jdbcTemplate.update("INSERT INTO esnsi_sync.msg_buffer (msg_id, msg, attachment, delivery_timestamp) VALUES (?, ?, ?, ?) ON CONFLICT (msg_id) DO NOTHING", msgId, msg, attachment, Timestamp.valueOf(utcDeliveryTimestamp));
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
