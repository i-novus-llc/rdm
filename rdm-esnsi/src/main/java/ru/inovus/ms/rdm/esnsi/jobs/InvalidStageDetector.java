package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.EsnsiLoadService;

import java.util.List;
import java.util.Set;

public class InvalidStageDetector implements Job {

    private static final Logger logger = LoggerFactory.getLogger(InvalidStageDetector.class);

    @Autowired
    private EsnsiLoadService esnsiLoadService;

    @Autowired
    private Scheduler scheduler;

    @Value("${esnsi.classifier.codes}")
    private List<String> codes;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        for (String classifierCode : codes) {
            ClassifierProcessingStage stage = esnsiLoadService.getClassifierProcessingStage(classifierCode);
            if (stage != ClassifierProcessingStage.NONE) {
                try {
                    Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(classifierCode));
                    if (jobKeys.isEmpty()) {
                        logger.warn("Detected invalid classifier processing stage. No jobs currently running for classifier {}, although it's stage is not NONE. Stage will be explicitly set to NONE.", classifierCode);
                        esnsiLoadService.setClassifierProcessingStageAtomically(classifierCode, stage, ClassifierProcessingStage.NONE, () -> {});
                    }
                } catch (SchedulerException e) {
                    logger.warn("Can't get job keys with group matching {}", classifierCode, e);
                }
            }
        }
    }

}
