package com.library.loan_service.service;


import com.library.loan_service.dto.FineResponse;
import com.library.loan_service.exception.BusinessException;
import com.library.loan_service.exception.ResourceNotFoundException;
import com.library.loan_service.model.Fine;
import com.library.loan_service.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;

    public List<FineResponse> findByUser(Long userId){
        return fineRepository.findByUserId(userId).stream()
                .map(FineResponse::from)
                .toList();
    }

    public FineResponse pay(Long fineId){
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        if(fine.isPaid()){
            throw new BusinessException("Fine already paid");
        }
        fine.setPaid(true);
        return FineResponse.from(fineRepository.save(fine));
    }
}
