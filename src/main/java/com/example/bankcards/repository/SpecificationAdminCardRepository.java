package com.example.bankcards.repository;

import com.example.bankcards.dto.request.FindAllCardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpecificationAdminCardRepository {

    Page<FindAllCardRequest> findAll(FindAllCardRequest findAllCardRequest, Pageable pageable);
}
