package com.stations.facedetection.Dashboard.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AttendanceSyncProcedureService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void runSyncProcedureSafe() {
        try {
            entityManager.createNativeQuery("CALL public.employee_main_checkincheckout_procedure()")
                    .executeUpdate();
            log.info("Attendance sync procedure executed successfully");
        } catch (Exception ex) {
            // Keep dashboard APIs responsive even if procedure is not stable yet.
            log.warn("Attendance sync procedure failed; continuing with existing data. Reason: {}", ex.getMessage());
        }
    }
}
