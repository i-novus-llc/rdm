package ru.i_novus.ms.rdm.impl.service.async;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.n2oapp.platform.i18n.UserException;

import java.util.function.Supplier;

/**
 * Сервис для асинхронных операций с использованием shedlock.
 */
public interface AsyncShedLockService {

    void run(
            LockConfiguration lockConfiguration,
            Runnable runnable,
            Supplier<UserException> onLockError
    );
}
