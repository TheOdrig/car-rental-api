package com.akif.model;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "car",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_car_license_plate", columnNames = "license_plate"),
            @UniqueConstraint(name = "uk_car_vin", columnNames = "vin_number")
        },
        indexes = {
            @Index(name = "idx_car_brand", columnList = "brand"),
            @Index(name = "idx_car_status", columnList = "car_status_type"),
            @Index(name = "idx_car_price", columnList = "price"),
            @Index(name = "index_car_year", columnList = "production_year"),
            @Index(name = "index_car_create_time", columnList = "create_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Car extends BaseEntity {

    @Column(name = "license_plate", nullable = false, length = 11,  unique = true)
    private String licensePlate;

    @Column(name = "vin_number", length = 17, unique = true)
    private String vinNumber;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name= "model", nullable = false, length = 50)
    private String model;

    @Column(name = "production_year",  nullable = false)
    private Integer productionYear;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type",  nullable = false, length = 10)
    private CurrencyType currencyType;

    @Column(name = "damage_price", precision = 12, scale = 2)
    private BigDecimal damagePrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_status_type",  nullable = false, length = 10)
    private CarStatusType carStatusType;

    @Column(name = "engine_type", length = 20)
    private String engineType;

    @Column(name = "engine_displacement", precision = 4, scale = 2)
    private BigDecimal engineDisplacement;

    @Column(name = "fuel_type", length = 20)
    private String fuelType;

    @Column(name = "transmission_type", length = 20)
    private String transmissionType;

    @Column(name = "body_type", length = 20)
    private String bodyType;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "kilometer")
    private Long kilometer;

    @Column(name = "doors")
    private Integer doors;

    @Column(name = "seats")
    private Integer seats;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Column(name = "inspection_expiry_date")
    private LocalDate inspectionExpiryDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "is_test_drive_available", nullable = false)
    private Boolean isTestDriveAvailable = true;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;


    public boolean isAvailable() {
        return carStatusType != null && carStatusType.isAvailable();
    }

    public boolean canBeSold() {
        return carStatusType != null && carStatusType.canBeSold();
    }

    public boolean canBeReserved() {
        return carStatusType != null && carStatusType.canBeReserved();
    }

    public boolean requiresAttention() {
        return carStatusType != null && carStatusType.requiresAttention();
    }

    public String getFormattedPrice() {
        if (price == null || currencyType == null) {
            return "Price not set";
        }
        return currencyType.formatAmount(price);
    }

    public String getFormattedPriceWithSeparators() {
        if (price == null || currencyType == null) {
            return "Price not set";
        }
        return currencyType.formatAmountWithSeparators(price);
    }

    public int getAge() {
        if (productionYear == null) {
            return 0;
        }
        return LocalDate.now().getYear() - productionYear;
    }

    public boolean isNew() {
        return getAge() < 1;
    }

    public boolean isOld() {
        return getAge() > 10;
    }

    public String getFullName() {
        if (brand == null || model == null) {
            return "Unknown Car";
        }
        return brand + " " + model;
    }

    public String getDisplayName() {
        if (productionYear == null) {
            return getFullName();
        }
        return getFullName() + " (" + productionYear + ")";
    }

    public boolean hasDamage() {
        return damagePrice != null && damagePrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getTotalPrice() {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (damagePrice == null) {
            return price;
        }
        return price.add(damagePrice);
    }

    public boolean needsService() {
        if (nextServiceDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(nextServiceDate);
    }

    public boolean isInsuranceExpired() {
        if (insuranceExpiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(insuranceExpiryDate);
    }

    public boolean isInspectionExpired() {
        if (inspectionExpiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(inspectionExpiryDate);
    }

    public boolean hasExpiredDocuments() {
        return isInsuranceExpired() || isInspectionExpired();
    }


    public void incrementViewCount() {
        if (viewCount == null) {
            viewCount = 0L;
        }
        viewCount++;
    }

    public void incrementLikeCount() {
        if (likeCount == null) {
            likeCount = 0L;
        }
        likeCount++;
    }

    public void decrementLikeCount() {
        if (likeCount == null || likeCount <= 0) {
            likeCount = 0L;
        } else {
            likeCount--;
        }
    }

    public void markAsSold() {
        this.carStatusType = CarStatusType.SOLD;
    }

    public void markAsAvailable() {
        this.carStatusType = CarStatusType.AVAILABLE;
    }

    public void markAsReserved() {
        this.carStatusType = CarStatusType.RESERVED;
    }

    public void markAsMaintenance() {
        this.carStatusType = CarStatusType.MAINTENANCE;
    }

    public void markAsDamaged() {
        this.carStatusType = CarStatusType.DAMAGED;
    }

    public void markAsInspection() {
        this.carStatusType = CarStatusType.INSPECTION;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(this.vinNumber, car.vinNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vinNumber);
    }


    @Override
    public String toString() {
        return "Car{" +
                "id=" + getId() +
                ", licensePlate='" + licensePlate + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", productionYear=" + productionYear +
                ", price=" + price +
                ", currencyType=" + currencyType +
                ", carStatusType=" + carStatusType +
                '}';
    }
}
