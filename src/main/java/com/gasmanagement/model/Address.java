package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "addresses")
public class Address {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String recipientName;
    private String phone;
    private String addressLine;
    private String ward;
    private String district;
    private String city;

    @Builder.Default
    private boolean isDefault = false;
}
