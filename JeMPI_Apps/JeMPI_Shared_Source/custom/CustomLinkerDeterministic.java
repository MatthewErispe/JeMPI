package org.jembi.jempi.linker;

import org.apache.commons.lang3.StringUtils;

import org.jembi.jempi.shared.models.CustomEntity;
import org.jembi.jempi.shared.models.CustomGoldenRecord;

class CustomLinkerDeterministic {

   private CustomLinkerDeterministic() {}

   private static boolean isMatch(final String left, final String right) {
      return StringUtils.isNotBlank(left) && StringUtils.equals(left, right);
   }

   static boolean deterministicMatch(final CustomGoldenRecord goldenRecord,
                                     final CustomEntity customEntity) {
      final var natFingerprintCode_l = goldenRecord.natFingerprintCode();
      final var natFingerprintCode_r = customEntity.natFingerprintCode();
      return isMatch(natFingerprintCode_l, natFingerprintCode_r);
   }

}
