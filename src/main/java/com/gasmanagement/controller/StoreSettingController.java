package com.gasmanagement.controller;

import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.model.StoreSetting;
import com.gasmanagement.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/settings")
@RequiredArgsConstructor
public class StoreSettingController {

    private final StoreSettingService storeSettingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<StoreSetting>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(storeSettingService.getSettings()));
    }

    @PutMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<StoreSetting>> updateSettings(@RequestBody StoreSetting settings) {
        return ResponseEntity.ok(ApiResponse.ok(storeSettingService.updateSettings(settings)));
    }
}
