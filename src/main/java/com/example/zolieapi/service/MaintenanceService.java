package com.example.zolieapi.service;

public interface MaintenanceService {

    void expireStories();

    void sendCountdownReminders();

    void cleanStorage();

    void removeAbandonedUploads();

    void liftEndedSuspensions();

    void purgeIdempotencyKeys();
}
