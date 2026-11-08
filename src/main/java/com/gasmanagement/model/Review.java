package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String productId;

    @Indexed
    private String customerId;

    private String customerName;

    /** Rating for product (1 - 5) */
    private int rating;

    private String comment;

    /** Rating for delivery / staff (1 - 5) */
    private int deliveryRating;

    private String deliveryComment;

    private LocalDateTime createdAt;
}
