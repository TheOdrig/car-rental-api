package com.akif.service.penalty;

import com.akif.model.PenaltyWaiver;

import java.math.BigDecimal;
import java.util.List;

public interface IPenaltyWaiverService {

    PenaltyWaiver waivePenalty(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId);

    PenaltyWaiver waiveFullPenalty(Long rentalId, String reason, Long adminId);

    List<PenaltyWaiver> getPenaltyHistory(Long rentalId);

    void processRefundForWaiver(PenaltyWaiver waiver);
}
