package jp.kusumotolab.kgenprog.ga.selection;

import java.util.List;
import java.util.stream.Collectors;
import jp.kusumotolab.kgenprog.ga.variant.Variant;

public class GenerationalVariantSelection extends DefaultVariantSelection {

  public GenerationalVariantSelection(final int maxVariantPerGeneration) {
    super(maxVariantPerGeneration);
  }

  @Override
  public List<Variant> exec(final List<Variant> current, final List<Variant> generated) {

    // 最後の世代のうち，Fitness が 1.0でないものを variantsForSelection に追加
    final List<Variant> variantsForSelection = current.stream()
        .filter(variant -> !variant.isCompleted())
        .collect(Collectors.toList());

    return super.exec(variantsForSelection, generated);
  }
}
