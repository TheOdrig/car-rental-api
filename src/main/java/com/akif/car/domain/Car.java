package com.akif.car.domain;

import com.akif.car.api.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.domain.BaseEntity;
import com.akif.shared.exception.InvalidStatusTransitionException;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "car_status_type",  nullable = false, length = 15)
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

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    public boolean isAvailable() {
        return carStatusType != null && carStatusType.isAvailable();
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

    public int ageAt(LocalDate today) {
        if (productionYear == null) {
            return 0;
        }
        return today.getYear() - productionYear;
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

    public void changeFleetStatus(CarStatusType target) {
        if (target == null) {
            throw new InvalidStatusTransitionException("Target car status is required");
        }
        if (target == carStatusType) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition from %s to %s", carStatusType, target));
        }
        this.carStatusType = target;
    }

    public void markAsAvailable() {
        if (carStatusType != CarStatusType.AVAILABLE) {
            changeFleetStatus(CarStatusType.AVAILABLE);
        }
    }

    public void markAsMaintenance() {
        if (carStatusType != CarStatusType.MAINTENANCE) {
            changeFleetStatus(CarStatusType.MAINTENANCE);
        }
    }

    public void markAsDamaged() {
        if (carStatusType != CarStatusType.DAMAGED) {
            changeFleetStatus(CarStatusType.DAMAGED);
        }
    }

    public void markAsInspection() {
        if (carStatusType != CarStatusType.INSPECTION) {
            changeFleetStatus(CarStatusType.INSPECTION);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return getId() != null && Objects.equals(getId(), car.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
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
