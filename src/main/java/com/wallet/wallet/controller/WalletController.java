package com.wallet.wallet.controller;

import com.wallet.common.response.ApiResponse;
import com.wallet.wallet.dto.BalanceResponse;
import com.wallet.wallet.dto.WalletRequest;
import com.wallet.wallet.dto.WalletResponse;
import com.wallet.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets",
        description = "Endpoints for wallet creation, wallet retrieval, and current balance inspection.")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a wallet",
            description = "Creates a new wallet for the authenticated user using the requested ISO currency code.")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @Valid @RequestBody WalletRequest request,
            Principal principal
    ) {
        WalletResponse response =
                walletService.createWallet(principal.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.success("Wallet created successfully",
                        response));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Fetch wallet details",
            description = "Returns the wallet profile for a specific wallet owned by the authenticated user.")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @Parameter(
                    description = "Unique identifier of the wallet to fetch.")
            @PathVariable UUID walletId,
            Principal principal) {
        WalletResponse response =
                walletService.getWallet(walletId, principal.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Wallet fetched successfully",
                        response));
    }

    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Fetch wallet balance",
            description = "Returns the current available balance for a specific wallet owned by the authenticated user.")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(
                    description = "Unique identifier of the wallet whose balance should be returned.")
            @PathVariable UUID walletId,
            Principal principal) {
        BalanceResponse response =
                walletService.getBalance(walletId, principal.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Wallet balance fetched successfully",
                        response));
    }
}
