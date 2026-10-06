package com.example.tradedesk.service;

import com.example.tradedesk.dto.PositionResponse;
import com.example.tradedesk.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return positionRepository.findAllWithTicker();
    }
}