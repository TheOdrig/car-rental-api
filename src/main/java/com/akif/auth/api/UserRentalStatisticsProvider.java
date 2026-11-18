package com.akif.auth.api;

public interface UserRentalStatisticsProvider {

    UserRentalStatisticsDto getUserStatistics(Long userId);
}
