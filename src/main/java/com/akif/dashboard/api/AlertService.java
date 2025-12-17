package com.akif.dashboard.api;

import com.akif.dashboard.api.dto.AlertDto;
import com.akif.dashboard.domain.enums.AlertType;

import java.util.List;

public interface AlertService {

    void generateAlerts();

    AlertDto acknowledgeAlert(Long alertId, String adminUsername);

    List<AlertDto> getActiveAlerts();

    List<AlertDto> getAlertsByType(AlertType type);
}
