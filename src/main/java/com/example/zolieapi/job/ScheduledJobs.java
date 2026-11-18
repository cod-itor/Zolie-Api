package com.example.zolieapi.job;

import com.example.zolieapi.service.MaintenanceService;
import com.example.zolieapi.service.ReelProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    private static final int MAX_REELS_PER_RUN = 2;

    private final MaintenanceService maintenanceService;
    private final ReelProcessingService reelProcessingService;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void expireStories() {
        run("expire stories", maintenanceService::expireStories);
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void sendCountdownReminders() {
        run("countdown reminders", maintenanceService::sendCountdownReminders);
    }

    @Scheduled(fixedDelayString = "PT10S", initialDelayString = "PT10S")
    public void processReels() {
        run("reel processing", () -> {
            int processed = 0;
            while (processed < MAX_REELS_PER_RUN && reelProcessingService.processNext()) {
                processed++;
            }
        });
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT2M")
    public void cleanStorage() {
        run("storage cleanup", maintenanceService::cleanStorage);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void removeAbandonedUploads() {
        run("abandoned uploads", maintenanceService::removeAbandonedUploads);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT3M")
    public void liftEndedSuspensions() {
        run("suspension end", maintenanceService::liftEndedSuspensions);
    }

    @Scheduled(fixedDelayString = "P1D", initialDelayString = "PT10M")
    public void purgeIdempotencyKeys() {
        run("idempotency purge", maintenanceService::purgeIdempotencyKeys);
    }

    private void run(String name, Runnable job) {
        try {
            job.run();
        } catch (RuntimeException e) {
            log.error("Scheduled job failed: {}", name, e);
        }
    }
}
