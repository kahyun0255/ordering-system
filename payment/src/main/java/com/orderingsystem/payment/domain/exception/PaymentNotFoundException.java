package com.orderingsystem.payment.domain.exception;

import com.orderingsystem.common.exception.NotFoundException;

public class PaymentNotFoundException extends NotFoundException {
  public PaymentNotFoundException(String message) {
    super(message);
  }
}
