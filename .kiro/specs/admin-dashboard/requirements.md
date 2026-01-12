# Requirements Document

## Introduction

Admin Dashboard & Operations Panel, rent-a-car işletmesinin günlük operasyonlarını tek bir ekrandan yönetmesini sağlayan kapsamlı bir yönetim panelidir. Bu sistem, admin kullanıcılarına anlık durum görünürlüğü, operasyonel metrikler, alert bildirimleri ve hızlı aksiyon alma imkanı sunar.

Dashboard, mevcut modüllerin (rental, car, payment, damage) verilerini aggregate ederek operasyonel verimlilik sağlar. Caching stratejisi ile performans optimize edilir ve real-time alert sistemi ile proaktif problem yönetimi mümkün olur.

## Glossary

- **Dashboard**: Admin kullanıcıların operasyonel metrikleri ve durumları görüntülediği ana panel
- **Daily Summary**: Günlük operasyonel özet (pending pickups, returns, approvals)
- **Fleet Status**: Araç filosunun durumu (available, rented, maintenance)
- **Alert**: Dikkat gerektiren durumlar için otomatik bildirim (late returns, failed payments)
- **Quick Action**: Dashboard üzerinden tek tıkla yapılabilen operasyonlar (approve, pickup, return)
- **KPI**: Key Performance Indicator - Temel performans göstergeleri
- **Occupancy Rate**: Araç kullanım oranı (rented cars / total cars)
- **Revenue**: Toplam gelir (rental + penalty payments)
- **Pending**: Bekleyen işlemler (onay bekleyen, pickup bekleyen, return bekleyen)

## Requirements

### Requirement 1: Daily Operations Summary

**User Story:** As an admin, I want to see a daily operations summary on the dashboard, so that I can quickly understand the current state of operations and prioritize my tasks.

#### Acceptance Criteria

1. WHEN an admin accesses the dashboard THEN the System SHALL display the count of pending rental approvals (status = REQUESTED)
2. WHEN an admin accesses the dashboard THEN the System SHALL display the count of today's scheduled pickups (status = CONFIRMED, startDate = today)
3. WHEN an admin accesses the dashboard THEN the System SHALL display the count of today's scheduled returns (status = IN_USE, endDate = today)
4. WHEN an admin accesses the dashboard THEN the System SHALL display the count of overdue rentals (status = IN_USE, endDate < today)
5. WHEN an admin accesses the dashboard THEN the System SHALL display the count of pending damage assessments (status = REPORTED or UNDER_ASSESSMENT)
6. WHEN the daily summary data is requested THEN the System SHALL return the response within 500 milliseconds using cached data

### Requirement 2: Fleet Status Overview

**User Story:** As an admin, I want to see the current fleet status at a glance, so that I can understand vehicle availability and identify maintenance needs.

#### Acceptance Criteria

1. WHEN an admin views fleet status THEN the System SHALL display the count of available cars (status = AVAILABLE)
2. WHEN an admin views fleet status THEN the System SHALL display the count of currently rented cars (status = RESERVED with active rental)
3. WHEN an admin views fleet status THEN the System SHALL display the count of cars under maintenance (status = MAINTENANCE)
4. WHEN an admin views fleet status THEN the System SHALL display the count of damaged cars (status = DAMAGED)
5. WHEN an admin views fleet status THEN the System SHALL calculate and display the occupancy rate as a percentage (rented / total active cars × 100)
6. WHEN fleet status changes THEN the System SHALL invalidate the cache and refresh data on next request

### Requirement 3: Monthly Performance Metrics

**User Story:** As an admin, I want to see monthly performance metrics, so that I can track business performance and identify trends.

#### Acceptance Criteria

1. WHEN an admin requests monthly metrics THEN the System SHALL display total revenue for the current month (sum of captured payments)
2. WHEN an admin requests monthly metrics THEN the System SHALL display the count of completed rentals for the current month (status = RETURNED)
3. WHEN an admin requests monthly metrics THEN the System SHALL display the count of cancelled rentals for the current month (status = CANCELLED)
4. WHEN an admin requests monthly metrics THEN the System SHALL display total penalty revenue collected for the current month
5. WHEN an admin requests monthly metrics THEN the System SHALL display the average rental duration in days for the current month
6. WHEN an admin requests monthly metrics with a custom date range THEN the System SHALL calculate metrics for the specified period

### Requirement 4: Alert System

**User Story:** As an admin, I want to receive alerts for critical situations, so that I can take immediate action on urgent matters.

#### Acceptance Criteria

1. WHEN there are late returns exceeding 24 hours THEN the System SHALL generate a CRITICAL severity alert
2. WHEN there are failed payment attempts in the last 24 hours THEN the System SHALL generate a HIGH severity alert
3. WHEN fleet availability drops below 20% THEN the System SHALL generate a WARNING severity alert
4. WHEN there are unresolved damage disputes older than 7 days THEN the System SHALL generate a MEDIUM severity alert
5. WHEN an admin requests alerts THEN the System SHALL return alerts sorted by severity (CRITICAL > HIGH > WARNING > MEDIUM > LOW)
6. WHEN an admin acknowledges an alert THEN the System SHALL mark the alert as acknowledged and record the timestamp

### Requirement 5: Quick Actions

**User Story:** As an admin, I want to perform common operations directly from the dashboard, so that I can efficiently manage daily tasks without navigating to different pages.

#### Acceptance Criteria

1. WHEN an admin clicks approve on a pending rental THEN the System SHALL confirm the rental and update the dashboard counts
2. WHEN an admin clicks pickup on a confirmed rental THEN the System SHALL mark the rental as picked up and update the dashboard counts
3. WHEN an admin clicks return on an in-use rental THEN the System SHALL process the return and update the dashboard counts
4. WHEN a quick action is performed THEN the System SHALL return the updated rental status within 2 seconds
5. WHEN a quick action fails THEN the System SHALL display an error message with the failure reason

### Requirement 6: Revenue Analytics

**User Story:** As an admin, I want to see revenue analytics with trend data, so that I can understand financial performance over time.

#### Acceptance Criteria

1. WHEN an admin requests revenue analytics THEN the System SHALL display daily revenue for the last 30 days
2. WHEN an admin requests revenue analytics THEN the System SHALL display monthly revenue for the last 12 months
3. WHEN an admin requests revenue analytics THEN the System SHALL calculate revenue breakdown by source (rental, penalty, damage charges)
4. WHEN an admin requests revenue analytics THEN the System SHALL display comparison with previous period (percentage change)
5. WHEN revenue data is aggregated THEN the System SHALL use database-level aggregation queries for performance

### Requirement 7: Pending Items List

**User Story:** As an admin, I want to see detailed lists of pending items, so that I can review and process them efficiently.

#### Acceptance Criteria

1. WHEN an admin requests pending approvals THEN the System SHALL return a paginated list of rentals with status REQUESTED
2. WHEN an admin requests today's pickups THEN the System SHALL return a paginated list of confirmed rentals scheduled for today
3. WHEN an admin requests today's returns THEN the System SHALL return a paginated list of in-use rentals scheduled to end today
4. WHEN an admin requests overdue rentals THEN the System SHALL return a paginated list of late returns sorted by hours overdue
5. WHEN displaying pending items THEN the System SHALL include customer name, car details, dates, and total amount for each item

### Requirement 8: Dashboard Caching Strategy

**User Story:** As a system, I want to implement efficient caching for dashboard data, so that the dashboard loads quickly and reduces database load.

#### Acceptance Criteria

1. WHEN dashboard data is requested THEN the System SHALL serve cached data if available and not expired
2. WHEN a rental status changes THEN the System SHALL invalidate relevant dashboard caches
3. WHEN a car status changes THEN the System SHALL invalidate fleet status cache
4. WHEN a payment is processed THEN the System SHALL invalidate revenue-related caches
5. WHEN cache is invalidated THEN the System SHALL refresh data on the next request within 200 milliseconds
6. THE System SHALL configure cache TTL of 5 minutes for summary data and 15 minutes for analytics data

### Requirement 9: Dashboard Access Control

**User Story:** As a system administrator, I want dashboard access restricted to admin users, so that sensitive operational data is protected.

#### Acceptance Criteria

1. WHEN a non-admin user attempts to access dashboard endpoints THEN the System SHALL return 403 Forbidden
2. WHEN an admin user accesses dashboard endpoints THEN the System SHALL return the requested data
3. WHEN an unauthenticated user attempts to access dashboard endpoints THEN the System SHALL return 401 Unauthorized
4. THE System SHALL log all dashboard access attempts with user ID and timestamp
