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
@Document(collection = "brands")
public class Brand {
    @Id
    private String id;
    private String name;
    private String description;
    @Builder.Default
    private boolean active = true;
}
