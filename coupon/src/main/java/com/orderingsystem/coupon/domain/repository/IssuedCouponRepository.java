package com.orderingsystem.coupon.domain.repository;

import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
}
