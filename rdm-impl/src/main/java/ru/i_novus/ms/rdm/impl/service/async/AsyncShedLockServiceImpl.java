package ru.i_novus.ms.rdm.impl.service.async;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AsyncShedLockServiceImpl implements AsyncShedLockService {

    private final LockProvider lockProvider;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public AsyncShedLockServiceImpl(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    @Override
    public void run(
            LockConfiguration lockConfiguration,
            Runnable runnable,
            Supplier<UserException> onLockError
    ) {
        final SimpleLock lock = lockProvider.lock(lockConfiguration)
                .orElseThrow(onLockError);

        CompletableFuture.runAsync(runnable)
                .whenComplete((result, exception) -> lock.unlock());
    }
}