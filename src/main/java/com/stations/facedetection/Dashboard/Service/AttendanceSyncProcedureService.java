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

        log.info("Starting attendance sync procedure execution");

        try {

            entityManager
                    .createNativeQuery("CALL public.employee_main_checkincheckout_procedure()")
                    .executeUpdate();

            log.info("Attendance sync procedure executed successfully");

        } catch (Exception ex) {

            // Do not break dashboard APIs
            log.warn(
                "Attendance sync procedure failed. Dashboard will continue using existing data",
                ex
            );
        }
    }
}