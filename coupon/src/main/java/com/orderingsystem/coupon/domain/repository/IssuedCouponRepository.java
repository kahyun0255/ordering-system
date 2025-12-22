package com.orderingsystem.coupon.domain.repository;

import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    List<IssuedCoupon> findByUserIdAndStatusIn(UUID userId, Collection<IssuedCouponStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IssuedCoupon ic SET ic.status = 'EXPIRED' WHERE ic.expiredAt < :now AND ic.status = 'ISSUED'")
    int bulkExpireIssuedCoupons(LocalDateTime now);

}
