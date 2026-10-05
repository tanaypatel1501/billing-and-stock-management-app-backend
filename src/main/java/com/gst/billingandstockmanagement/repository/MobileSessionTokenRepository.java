package com.gst.billingandstockmanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gst.billingandstockmanagement.entities.MobileSessionToken;

@Repository
public interface MobileSessionTokenRepository extends JpaRepository<MobileSessionToken, Long> {

    Optional<MobileSessionToken> findByToken(String token);

    @Modifying
    @Query("UPDATE MobileSessionToken t SET t.revoked = true WHERE t.token = :token")
    void revokeByToken(@Param("token") String token);

    @Modifying
    @Query("UPDATE MobileSessionToken t SET t.revoked = true WHERE t.user.id = :userId")
    void revokeAllForUser(@Param("userId") Long userId);
}