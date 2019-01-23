package jp.kusumotolab.kgenprog.ga.variant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.Counter;
import jp.kusumotolab.kgenprog.OrdinalNumber;
import jp.kusumotolab.kgenprog.Strategies;
import jp.kusumotolab.kgenprog.fl.Suspiciousness;
import jp.kusumotolab.kgenprog.ga.validation.Fitness;
import jp.kusumotolab.kgenprog.project.GeneratedSourceCode;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class VariantStore {

  private final Configuration config;
  private final Strategies strategies;
  private final Variant initialVariant;
  private List<Variant> currentVariants;
  private final List<Variant> allVariants;
  private List<Variant> generatedVariants;
  private final List<Variant> foundSolutions;
  private final OrdinalNumber generation;
  private final Counter variantCounter;

  public VariantStore(final Configuration config, final Strategies strategies) {
    this.config = config;
    this.strategies = strategies;

    variantCounter = new Counter();
    generation = new OrdinalNumber(0);
    initialVariant = createInitialVariant();
    currentVariants = Collections.singletonList(initialVariant);
    allVariants = new LinkedList<>();
    allVariants.add(initialVariant);
    generatedVariants = new ArrayList<>();
    foundSolutions = new ArrayList<>();
    generation.incrementAndGet();
  }

  /**
   * テスト用
   */
  @Deprecated
  public VariantStore(final Variant initialVariant) {
    this.config = null;
    this.strategies = null;
    this.initialVariant = initialVariant;

    currentVariants = Collections.singletonList(initialVariant);
    allVariants = new ArrayList<>();
    allVariants.add(initialVariant);
    generatedVariants = new ArrayList<>();
    foundSolutions = new ArrayList<>();
    generation = new OrdinalNumber(1);
    variantCounter = new Counter(1);
  }

  public Variant createVariant(final Gene gene, final HistoricalElement element) {
    final GeneratedSourceCode sourceCode = strategies.execSourceCodeGeneration(this, gene);
    return createVariant(gene, sourceCode, element);
  }

  public Variant getInitialVariant() {
    return initialVariant;
  }

  public OrdinalNumber getGenerationNumber() {
    return generation;
  }

  public OrdinalNumber getFoundSolutionsNumber() {
    return new OrdinalNumber(foundSolutions.size());
  }

  public List<Variant> getCurrentVariants() {
    return currentVariants;
  }

  public List<Variant> getGeneratedVariants() {
    return generatedVariants;
  }

  public List<Variant> getAllVariants() {
    return allVariants;
  }

  public List<Variant> getFoundSolutions() {
    return foundSolutions;
  }

  public List<Variant> getFoundSolutions(final int maxNumber) {
    final int length = Math.min(maxNumber, foundSolutions.size());
    return foundSolutions.subList(0, length);
  }

  /**
   * 引数の要素すべてを次世代のVariantとして追加する
   *
   * @param variants 追加対象
   * @see addNextGenerationVariant(Variant)
   */
  public void addGeneratedVariants(final Variant... variants) {
    addGeneratedVariants(Arrays.asList(variants));
  }

  /**
   * リストの要素すべてを次世代のVariantとして追加する
   *
   * @param variants 追加対象
   * @see addNextGenerationVariant(Variant)
   */
  public void addGeneratedVariants(final Collection<? extends Variant> variants) {
    variants.forEach(this::addGeneratedVariant);
  }

  /**
   * 引数を次世代のVariantとして追加する {@code variant.isCompleted() == true} の場合，foundSolutionとして追加され次世代のVariantには追加されない
   *
   * @param variant
   */
  public void addGeneratedVariant(final Variant variant) {

    allVariants.add(variant);
    if (variant.isCompleted()) {
      foundSolutions.add(variant);
    } else {
      generatedVariants.add(variant);
    }
  }

  /**
   * VariantSelectionを実行し世代交代を行う
   *
   * currentVariantsおよびgeneratedVariantsから次世代のVariantsを選択し，それらを次のcurrentVariantsとする
   * また，generatedVariantsをclearする
   */
  public void proceedNextGeneration() {

    final List<Variant> nextVariants =
        strategies.execVariantSelection(currentVariants, generatedVariants);
    nextVariants.forEach(Variant::incrementSelectionCount);
    generation.incrementAndGet();

    currentVariants = nextVariants;
    generatedVariants = new ArrayList<>();
  }

  private Variant createInitialVariant() {
    final GeneratedSourceCode sourceCode =
        strategies.execASTConstruction(config.getTargetProject());
    return createVariant(new Gene(Collections.emptyList()), sourceCode,
        new OriginalHistoricalElement());
  }

  private Variant createVariant(final Gene gene, final GeneratedSourceCode sourceCode,
      final HistoricalElement element) {
    final LazyVariant variant = new LazyVariant(variantCounter.getAndIncrement(), generation.get(),
        gene, sourceCode, element);
    final Single<Variant> variantSingle = Single.just(variant)
        .cast(Variant.class)
        .cache();

    final Single<TestResults> resultsSingle = sourceCode.isGenerationSuccess()
        ? strategies.execAsyncTestExecutor(variantSingle)
        .cache()
        : Single.just(EmptyTestResults.instance);
    variant.setTestResultsSingle(resultsSingle);

    final Single<Fitness> fitnessSingle = Single
        .zip(variantSingle, resultsSingle,
            (v, r) -> strategies.execSourceCodeValidation(sourceCode, r))
        .cache();
    variant.setFitnessSingle(fitnessSingle);

    final Single<List<Suspiciousness>> suspiciousnessListSingle = Single
        .zip(variantSingle, resultsSingle,
            (v, r) -> strategies.execFaultLocalization(sourceCode, r))
        .cache();
    variant.setSuspiciousnessListSingle(suspiciousnessListSingle);

    variant.subscribe();

    return variant;
  }
}
