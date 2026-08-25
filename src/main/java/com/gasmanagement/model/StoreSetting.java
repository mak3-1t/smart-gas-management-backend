package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "store_settings")
public class StoreSetting {
    @Id
    private String id;
    
    @Builder.Default
    private String settingKey = "DEFAULT_CONFIG";

    private Double defaultDeliveryFee;
    private Double defaultCylinderFee;
    
    private Double freeShippingThreshold;
    
    private String storeOperatingHours; // e.g., "07:00-19:00"
    
    private String deliveryTimePolicy;
    private String cancellationPolicy;
}
