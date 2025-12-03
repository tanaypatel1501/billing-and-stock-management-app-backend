package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.dto.AddressLookupDTO;
import com.gst.billingandstockmanagement.entities.IndiaPostalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostalRepository extends JpaRepository<IndiaPostalData, Integer > {

    IndiaPostalData findByPincode(Integer  pincode);

    // 2. States
    @Query(value = "SELECT DISTINCT t.statename FROM india_postal_data t ORDER BY t.statename",
            nativeQuery = true)
    List<String> findAllDistinctStates();

    // 3. Cities
    @Query(value = "SELECT DISTINCT TRIM(t.district) FROM india_postal_data t " +
            "WHERE UPPER(TRIM(t.statename)) = UPPER(TRIM(:state)) ORDER BY TRIM(t.district)",
            nativeQuery = true)
    List<String> findDistinctDistrictsByState(@Param("state") String state);

    // 4. Get Addresses by district/state (map directly into DTO using constructor expression in JPQL)
    @Query("SELECT new com.gst.billingandstockmanagement.dto.AddressLookupDTO(" +
            "p.pincode, p.district, p.statename) " +
            "FROM IndiaPostalData p " +
            "WHERE UPPER(TRIM(p.district)) = UPPER(TRIM(:district)) " +
            "AND UPPER(TRIM(p.statename)) = UPPER(TRIM(:state)) " +
            "ORDER BY p.pincode")
    List<AddressLookupDTO> findAddressesByDistrictAndState(@Param("district") String district,
                                                           @Param("state") String state);

    // 5. Find state by district
    @Query(value = "SELECT DISTINCT TRIM(t.statename) FROM india_postal_data t " +
            "WHERE UPPER(TRIM(t.district)) = UPPER(TRIM(:district)) LIMIT 1",
            nativeQuery = true)
    String findStateByDistrict(@Param("district") String district);

    // 6. Fallback: find by pincode string (handles DBs that store pincodes as CHAR/VARCHAR or with leading zeros)
    // Using MySQL syntax to cast pincode to CHAR. If you use Postgres, change CAST(... AS TEXT) instead.
    @Query(value = "SELECT * FROM india_postal_data p WHERE CAST(p.pincode AS CHAR) = :pincode LIMIT 1",
            nativeQuery = true)
    IndiaPostalData findByPincodeString(@Param("pincode") String pincode);
}
