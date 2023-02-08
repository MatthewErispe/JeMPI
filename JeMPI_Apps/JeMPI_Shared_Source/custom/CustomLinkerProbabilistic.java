package org.jembi.jempi.linker;


import org.jembi.jempi.shared.models.CustomMU;
import org.jembi.jempi.shared.models.CustomEntity;
import org.jembi.jempi.shared.models.CustomGoldenRecord;

public class CustomLinkerProbabilistic {

  public static float probabilisticScore(final CustomGoldenRecord goldenRecord,
                                         final CustomEntity customEntity) {
    return 0.0F;
  }

  public static void updateMU(final CustomMU mu) {
  }

  public static void checkUpdatedMU() {
  }

  static CustomMU getMU() {
    return new CustomMU(null);
  }

}
