package com.example.tradedesk.service;

import com.example.tradedesk.domain.PnlSnapshot;
import com.example.tradedesk.dto.PnlSnapshotResponse;
import com.example.tradedesk.repository.PnlSnapshotRepository;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.PositionRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class PnlSnapshotService {

    private final PositionRepository positionRepository;
    private final PnlSnapshotRepository snapshotRepository;

    public PnlSnapshotService(PositionRepository positionRepository,
                              PnlSnapshotRepository snapshotRepository) {
        this.positionRepository = positionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /** Records every active position for the given date. Safe to re-run: replaces that date's rows. */
    @Transactional
    public int takeSnapshot(LocalDate date) {
        OffsetDateTime now = OffsetDateTime.now();
        int written = 0;

        for (PositionRow row : positionRepository.findAllRows()) {
            boolean neverTraded = row.netExposure().signum() == 0 && row.realizedPnl().signum() == 0;
            if (neverTraded) {
                continue;
            }

            PnlSnapshot snapshot = snapshotRepository
                    .findBySnapshotDateAndInstrumentId(date, row.instrumentId())
                    .orElseGet(PnlSnapshot::new);

            snapshot.setSnapshotDate(date);
            snapshot.setInstrumentId(row.instrumentId());
            snapshot.setCurrency(PnlCalculator.quoteCurrency(row.currencyPair()));
            snapshot.setNetExposure(row.netExposure());
            snapshot.setAvgRate(row.avgRate());
            snapshot.setMarkRate(row.markRate());
            snapshot.setRealizedPnl(row.realizedPnl());
            snapshot.setUnrealizedPnl(
                    PnlCalculator.unrealized(row.netExposure(), row.avgRate(), row.markRate()));
            snapshot.setCreatedAt(now);
            snapshotRepository.save(snapshot);
            written++;
        }

        log.info("P&L snapshot for {}: {} positions recorded", date, written);
        return written;
    }

    @Transactional(readOnly = true)
    public List<PnlSnapshotResponse> list(LocalDate from, LocalDate to) {
        return snapshotRepository.findRange(from, to);
    }
}