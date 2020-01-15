package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.util.UUID;

public interface AsyncOperationLogEntryRepository extends JpaRepository<AsyncOperationLogEntryEntity, UUID> {}
