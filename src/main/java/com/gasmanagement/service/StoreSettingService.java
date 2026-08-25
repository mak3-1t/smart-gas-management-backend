package com.gasmanagement.service;

import com.gasmanagement.model.StoreSetting;

public interface StoreSettingService {
    StoreSetting getSettings();
    StoreSetting updateSettings(StoreSetting newSettings);
}
