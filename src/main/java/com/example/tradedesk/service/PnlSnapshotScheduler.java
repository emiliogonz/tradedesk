package com.example.tradedesk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class PnlSnapshotScheduler {

    private final PnlSnapshotService snapshotService;
    private final ZoneId zone;

    public PnlSnapshotScheduler(PnlSnapshotService snapshotService,
                                @Value("${tradedesk.snapshot.zone}") String zone) {
        this.snapshotService = snapshotService;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${tradedesk.snapshot.cron}", zone = "${tradedesk.snapshot.zone}")
    public void runEndOfDaySnapshot() {
        snapshotService.takeSnapshot(LocalDate.now(zone));
    }
}