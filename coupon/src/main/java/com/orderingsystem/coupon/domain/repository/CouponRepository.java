package com.orderingsystem.coupon.domain.repository;

import com.orderingsystem.coupon.domain.model.Coupon;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
}
