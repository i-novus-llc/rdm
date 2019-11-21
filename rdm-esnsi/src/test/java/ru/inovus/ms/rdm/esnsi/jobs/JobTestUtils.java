package ru.inovus.ms.rdm.esnsi.jobs;

import org.mockito.ArgumentCaptor;
import org.quartz.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JobTestUtils {

    static final InputStream EMPTY_IN = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    static final int NUM_REVISIONS = 432543423;
    static final String CLASSIFIER_CODE = "ОКАТО";

    static JobExecutionContext ctx(Class<? extends AbstractEsnsiDictionaryProcessingJob> jobClass, Map<String, Object> jobData) {
        return new ExecContext(CLASSIFIER_CODE, jobClass, jobData);
    }

    static void assertJobExecutedWithArgs(AbstractEsnsiDictionaryProcessingJob spy, Map<String, Object> args) {
        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);
        verify(spy, times(1)).execSmevResponseResponseReadingJob(captor.capture());
        JobDataMap jobDataMap = captor.getValue().getJobDataMap();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            assertEquals(jobDataMap.get(entry.getKey()), entry.getValue());
        }
    }

    private static class ExecContext implements JobExecutionContext {

        private final JobDetail jobDetail;

        ExecContext(String classifierCode, Class<? extends AbstractEsnsiDictionaryProcessingJob> jobClass, Map<String, Object> jobData) {
            JobKey jobKey = JobKey.jobKey(jobClass.getSimpleName(), classifierCode);
            JobDataMap jobDataMap = new JobDataMap(jobData);
            this.jobDetail = new JobDetail() {
                @Override public JobKey getKey() {return jobKey;}
                @Override public JobDataMap getJobDataMap() {return jobDataMap;}

                @Override public String getDescription() {throw new UnsupportedOperationException();}
                @Override public Class<? extends Job> getJobClass() {throw new UnsupportedOperationException();}
                @Override public boolean isDurable() {throw new UnsupportedOperationException();}
                @Override public boolean isPersistJobDataAfterExecution() {throw new UnsupportedOperationException();}
                @Override public boolean isConcurrentExectionDisallowed() {throw new UnsupportedOperationException();}
                @Override public boolean requestsRecovery() {throw new UnsupportedOperationException();}
                @Override public JobBuilder getJobBuilder() {throw new UnsupportedOperationException();}
                @Override public Object clone() {throw new UnsupportedOperationException();}
            };
        }

        @Override public JobDetail getJobDetail() {
            return jobDetail;
        }

        @Override public Scheduler getScheduler() {throw new UnsupportedOperationException();}
        @Override public Trigger getTrigger() {throw new UnsupportedOperationException();}
        @Override public Calendar getCalendar() {throw new UnsupportedOperationException();}
        @Override public boolean isRecovering() {throw new UnsupportedOperationException();}
        @Override public TriggerKey getRecoveringTriggerKey() throws IllegalStateException {throw new UnsupportedOperationException();}
        @Override public int getRefireCount() {throw new UnsupportedOperationException();}
        @Override public JobDataMap getMergedJobDataMap() {throw new UnsupportedOperationException();}
        @Override public Job getJobInstance() {throw new UnsupportedOperationException();}
        @Override public Date getFireTime() {throw new UnsupportedOperationException();}
        @Override public Date getScheduledFireTime() {throw new UnsupportedOperationException();}
        @Override public Date getPreviousFireTime() {throw new UnsupportedOperationException();}
        @Override public Date getNextFireTime() {throw new UnsupportedOperationException();}
        @Override public String getFireInstanceId() {throw new UnsupportedOperationException();}
        @Override public Object getResult() {throw new UnsupportedOperationException();}
        @Override public void setResult(Object result) {throw new UnsupportedOperationException();}
        @Override public long getJobRunTime() {throw new UnsupportedOperationException();}
        @Override public void put(Object key, Object value) {throw new UnsupportedOperationException();}
        @Override public Object get(Object key) {throw new UnsupportedOperationException();}

    }

}
