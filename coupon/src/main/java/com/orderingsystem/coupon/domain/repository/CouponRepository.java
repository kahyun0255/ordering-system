package com.orderingsystem.coupon.domain.repository;

import com.orderingsystem.coupon.domain.model.Coupon;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    @Modifying
    @Query("UPDATE Coupon  c SET c.issuedCount = c.issuedCount + :count WHERE c.couponId = :couponId")
    void increaseIssuedCount(@Param("couponId") UUID couponId, @Param("count") Long count);

    List<Coupon> findAllByCouponIdIn(List<UUID> couponId);

}
