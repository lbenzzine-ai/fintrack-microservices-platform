package com.fintrack.transaction.mapper;

import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    TransactionResponse toResponse(Transaction t);
}
