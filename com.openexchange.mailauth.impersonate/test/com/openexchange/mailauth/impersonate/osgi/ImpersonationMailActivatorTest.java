
package com.openexchange.mailauth.impersonate.osgi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.test.osgi.AbstractHousekeepingActivatorTest;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

public class ImpersonationMailActivatorTest extends AbstractHousekeepingActivatorTest {

    @Mock
    ThreadPoolService threadPool;

    @Mock
    TimerService timer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(threadPool.getExecutor()).thenReturn(Mockito.mock(ExecutorService.class));
        Mockito.when(timer.getExecutor()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
    }

    @SuppressWarnings("null")
    @Override
    public ActivatorExtender getActivator() {
        return ActivatorExtender
            .builder(new ImpersonationMailActivator())
            .withPreparedService(ThreadPoolService.class, threadPool)
            .withPreparedService(TimerService.class, timer)
            .withServices(MailAuthenticator.class)
            .build();
    }

}
