package com.wallet.transaction.controller;

import com.wallet.common.response.ApiResponse;
import com.wallet.transaction.dto.FundRequest;
import com.wallet.transaction.dto.TransactionHistoryResponse;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.dto.WithdrawRequest;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.model.TransactionType;
import com.wallet.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Transactions",
        description = "Endpoints for wallet funding, withdrawals, transfers, transaction history lookup, and reversals.")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/wallets/{walletId}/fund")
    @Operation(summary = "Fund a wallet",
            description = "Credits the target wallet and records a successful credit transaction. Reuse the same Idempotency-Key header to safely retry the request.")
    public ResponseEntity<ApiResponse<TransactionResponse>> fundWallet(
            @Parameter(
                    description = "Unique identifier of the wallet to credit.")
            @PathVariable UUID walletId,
            @Valid @RequestBody FundRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(
                transactionService.fundWallet(walletId, request,
                        principal.getName()));
    }

    @PostMapping("/wallets/{walletId}/withdraw")
    @Operation(summary = "Withdraw from a wallet",
            description = "Debits the target wallet if enough balance is available and records the resulting debit transaction. Reuse the same Idempotency-Key header to safely retry the request.")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdrawWallet(
            @Parameter(
                    description = "Unique identifier of the wallet to debit.")
            @PathVariable UUID walletId,
            @Valid @RequestBody WithdrawRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(
                transactionService.withdrawFunds(walletId, request,
                        principal.getName()));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer between wallets",
            description = "Moves funds from a source wallet to a destination wallet and records matching debit and credit ledger entries. Reuse the same Idempotency-Key header to safely retry the request.")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(transactionService.transferFunds(request,
                principal.getName()));
    }

    @GetMapping("/wallets/{walletId}/transactions")
    @Operation(summary = "Fetch transaction history",
            description = "Returns paginated transaction history for a wallet with optional filters for type, status, date range, and amount range.")
    public ResponseEntity<ApiResponse<TransactionHistoryResponse>> history(
            @Parameter(
                    description = "Unique identifier of the wallet whose transaction history should be returned.")
            @PathVariable UUID walletId,
            @Parameter(description = "Zero-based page index.",
                    example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page.",
                    example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional transaction type filter.",
                    example = "CREDIT")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Optional transaction status filter.",
                    example = "SUCCESS")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(
                    description = "Optional start date in ISO-8601 format.",
                    example = "2026-03-01")
            @RequestParam(required = false) LocalDate from,
            @Parameter(
                    description = "Optional end date in ISO-8601 format.",
                    example = "2026-03-20")
            @RequestParam(required = false) LocalDate to,
            @Parameter(
                    description = "Optional minimum transaction amount.",
                    example = "100.00")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(
                    description = "Optional maximum transaction amount.",
                    example = "5000.00")
            @RequestParam(required = false) BigDecimal maxAmount,
            Principal principal
    ) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(
                walletId, page, size, type, status, from, to, minAmount,
                maxAmount, principal.getName()
        ));
    }

    @PostMapping("/transactions/{transactionId}/reverse")
    @Operation(summary = "Reverse a transaction",
            description = "Reverses a previously successful transaction by applying the opposite balance change and creating a new reversal ledger entry.")
    public ResponseEntity<ApiResponse<TransactionResponse>> reverse(
            @Parameter(
                    description = "Unique identifier of the successful transaction to reverse.")
            @PathVariable UUID transactionId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                transactionService.reverseTransaction(transactionId,
                        principal.getName()));
    }
}
