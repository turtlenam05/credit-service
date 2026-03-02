package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditReservation;
import com.dathq.swd302.creditservice.entity.ReservationStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditReservationRepository  extends CrudRepository<CreditReservation, Long> {
    Optional<CreditReservation> findByReferenceIdAndStatus(String referenceId, ReservationStatus status);


}
