package org.jembi.jempi.em.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.AppConfig;
import org.jembi.jempi.libmpi.LibMPI;

public class GammaMatrixGenerator {
    private static LibMPI libMPI = null;
    private final FieldComparator fieldComparator = new FieldComparator();
    private static final Logger LOGGER = LogManager.getLogger(GammaMatrixGenerator.class);

    public GammaMatrixGenerator() {
        if (libMPI == null) {
            openMPI(true);
        }
        LOGGER.debug(libMPI);
    }
    private static void openMPI(final boolean useDGraph) {
        if (useDGraph) {
            final var host = AppConfig.DGRAPH_ALPHA_HOSTS;
            final var port = AppConfig.DGRAPH_ALPHA_PORTS;
            libMPI = new LibMPI(host, port);
        } else {
            libMPI = new LibMPI(String.format("jdbc:postgresql://%s/notifications", AppConfig.POSTGRES_SERVER),
                    "postgres",
                    null);
        }
    }

//    public final List<Interaction> getRandomPatients(final int scale) {
//
//        try {
//            LOGGER.debug("I just got Patients from libMPI ");
//            return libMPI.getRandomPatients();
//
//        } catch (Exception e) {
//            LOGGER.debug(e.getMessage());
//            return null;
//        }
//    }
//    public final List<List<Boolean>> generateGammaMatrix() {
//        List<Interaction> randomPatients = libMPI.getRandomPatients();
//
//        List<List<Boolean>> gammaMatrix = new ArrayList<>();
//
//        for (Interaction patient : randomPatients) {
//            List<Boolean> agreementVector = new ArrayList<>();
//            List<GoldenRecord> candidateGoldenRecords =   libMPI.getCandidates(patient.demographicData(), true);
//            for (GoldenRecord candidate : candidateGoldenRecords) {
//                boolean agreement = fieldComparator.compareFields(patient, candidate);
//                agreementVector.add(agreement);
//            }
//
//            gammaMatrix.add(agreementVector);
//        }
//
//        return gammaMatrix;
//    }

}