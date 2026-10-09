package com.example.tradedesk.service;

import com.example.tradedesk.dto.PositionResponse;
import com.example.tradedesk.repository.PositionRepository;
import com.example.tradedesk.repository.PositionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.tradedesk.service.PnlCalculator.quoteCurrency;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return positionRepository.findAllRows().stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionResponse toResponse(PositionRow row) {
        return new PositionResponse(
                row.instrumentId(),
                row.ticker(),
                quoteCurrency(row.currencyPair()),
                row.netExposure(),
                row.maxExposure(),
                row.avgRate(),
                row.markRate(),
                row.realizedPnl(),
                PnlCalculator.unrealized(row.netExposure(), row.avgRate(), row.markRate()),
                row.lastUpdated()
        );
    }

}