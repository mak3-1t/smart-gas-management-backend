package com.gasmanagement.service.impl;

import com.gasmanagement.model.StoreSetting;
import com.gasmanagement.repository.StoreSettingRepository;
import com.gasmanagement.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreSettingServiceImpl implements StoreSettingService {

    private final StoreSettingRepository storeSettingRepository;
    private static final String DEFAULT_KEY = "DEFAULT_CONFIG";

    @Override
    public StoreSetting getSettings() {
        return storeSettingRepository.findBySettingKey(DEFAULT_KEY)
                .orElseGet(() -> {
                    StoreSetting defaultSetting = StoreSetting.builder()
                            .settingKey(DEFAULT_KEY)
                            .defaultDeliveryFee(30000.0)
                            .defaultCylinderFee(500000.0)
                            .freeShippingThreshold(1000000.0)
                            .storeOperatingHours("07:00-19:00")
                            .build();
                    return storeSettingRepository.save(defaultSetting);
                });
    }

    @Override
    public StoreSetting updateSettings(StoreSetting newSettings) {
        StoreSetting existing = getSettings();
        
        existing.setDefaultDeliveryFee(newSettings.getDefaultDeliveryFee());
        existing.setDefaultCylinderFee(newSettings.getDefaultCylinderFee());
        existing.setFreeShippingThreshold(newSettings.getFreeShippingThreshold());
        existing.setStoreOperatingHours(newSettings.getStoreOperatingHours());
        existing.setDeliveryTimePolicy(newSettings.getDeliveryTimePolicy());
        existing.setCancellationPolicy(newSettings.getCancellationPolicy());
        
        return storeSettingRepository.save(existing);
    }
}
