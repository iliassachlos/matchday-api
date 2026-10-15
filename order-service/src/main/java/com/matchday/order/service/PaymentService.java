package com.matchday.order.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.matchday.order.dto.PaymentResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    public static final String CARD_OK = "pm_card_ok";
    public static final String CARD_DECLINED = "pm_card_declined";

    public PaymentResult charge(UUID orderId, long amountCents, String paymentMethod) {
        if (CARD_DECLINED.equals(paymentMethod)) {
            log.info("Declined {} cents for order {}", amountCents, orderId);

            return PaymentResult.declined("Card declined");
        }

        String reference = "fake_ch_" + UUID.randomUUID();
        log.info("Charged {} cents for order {} as {}", amountCents, orderId, reference);

        return PaymentResult.approved(reference);
    }

    public void refund(String paymentReference, long amountCents) {
        log.info("Refunded {} cents for payment {}", amountCents, paymentReference);
    }
}
