package com.wallet.transaction.dto;

import java.util.List;

public record TransactionHistoryResponse(
        List<TransactionResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
