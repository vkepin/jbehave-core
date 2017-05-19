package org.jbehave.examples.core;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;
import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.HTML_TEMPLATE;
import static org.jbehave.core.reporters.Format.TXT;
import static org.jbehave.core.reporters.Format.XML_TEMPLATE;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.context.Context;
import org.jbehave.core.context.ContextView;
import org.jbehave.core.context.JFrameContextView;
import org.jbehave.core.embedder.PropertyBasedEmbedderControls;
import org.jbehave.core.i18n.LocalizedKeywords;
import org.jbehave.core.io.CodeLocations;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.model.TableTransformers;
import org.jbehave.core.parsers.RegexPrefixCapturingPatternParser;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.reporters.ContextOutput;
import org.jbehave.core.reporters.CrossReference;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.ContextStepMonitor;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.core.steps.ParameterConverters;
import org.jbehave.core.steps.ParameterConverters.DateConverter;
import org.jbehave.core.steps.ParameterConverters.ExamplesTableConverter;
import org.jbehave.examples.core.service.TradingService;
import org.jbehave.examples.core.steps.AndSteps;
import org.jbehave.examples.core.steps.BeforeAfterSteps;
import org.jbehave.examples.core.steps.CalendarSteps;
import org.jbehave.examples.core.steps.CompositeSteps;
import org.jbehave.examples.core.steps.ContextSteps;
import org.jbehave.examples.core.steps.ExamplesTableParametersSteps;
import org.jbehave.examples.core.steps.JsonSteps;
import org.jbehave.examples.core.steps.MetaParametrisationSteps;
import org.jbehave.examples.core.steps.MyContext;
import org.jbehave.examples.core.steps.NamedParametersSteps;
import org.jbehave.examples.core.steps.PendingSteps;
import org.jbehave.examples.core.steps.PriorityMatchingSteps;
import org.jbehave.examples.core.steps.RestartingSteps;
import org.jbehave.examples.core.steps.SandpitSteps;
import org.jbehave.examples.core.steps.SearchSteps;
import org.jbehave.examples.core.steps.TableSteps;
import org.jbehave.examples.core.steps.TraderSteps;

/**
 * <p>
 * Example of how multiple stories can be run via JUnit.
 * </p>
 * <p>
 * Stories are specified in classpath and correspondingly the
 * {@link LoadFromClasspath} story loader is configured.
 * </p>
 */
public class CoreStories extends JUnitStories {

    private final CrossReference xref = new CrossReference();
    private Context context = new Context();
    private Format contextFormat = new ContextOutput(context);
    private ContextView contextView = new JFrameContextView().sized(640, 120);
    private ContextStepMonitor contextStepMonitor = new ContextStepMonitor(context, contextView, xref.getStepMonitor());

    public CoreStories() {
        configuredEmbedder().embedderControls().doGenerateViewAfterStories(true).doIgnoreFailureInStories(false)
                .doIgnoreFailureInView(true).doVerboseFailures(true).useThreads(2).useStoryTimeouts("60");
        configuredEmbedder().useEmbedderControls(new PropertyBasedEmbedderControls());
    }

    @Override
    public Configuration configuration() {
        // avoid re-instantiating configuration for the steps factory
        // alternative use #useConfiguration() in the constructor
        if ( super.hasConfiguration() ){
            return super.configuration();
        }
        Class<? extends Embeddable> embeddableClass = this.getClass();
        Properties viewResources = new Properties();
        viewResources.put("decorateNonHtml", "true");
        viewResources.put("reports", "ftl/jbehave-reports.ftl");
        LoadFromClasspath resourceLoader = new LoadFromClasspath(embeddableClass);
        TableTransformers tableTransformers = new TableTransformers();
        // Start from default ParameterConverters instance
        ParameterConverters parameterConverters = new ParameterConverters(resourceLoader, tableTransformers);
        // factory to allow parameter conversion and loading from external
        // resources (used by StoryParser too)
        ExamplesTableFactory examplesTableFactory = new ExamplesTableFactory(new LocalizedKeywords(), resourceLoader,
                parameterConverters, tableTransformers);
        // add custom converters
        parameterConverters.addConverters(new DateConverter(new SimpleDateFormat("yyyy-MM-dd")),
                new ExamplesTableConverter(examplesTableFactory));
        return new MostUsefulConfiguration()                
                .useStoryLoader(resourceLoader)
                .useStoryParser(new RegexStoryParser(examplesTableFactory))
                .useStoryReporterBuilder(
                        new StoryReporterBuilder()
                                .withCodeLocation(CodeLocations.codeLocationFromClass(embeddableClass))
                                .withDefaultFormats().withViewResources(viewResources)
                                .withFormats(contextFormat, CONSOLE, TXT, HTML_TEMPLATE, XML_TEMPLATE).withFailureTrace(true)
                                .withFailureTraceCompression(true).withCrossReference(xref))
                .useParameterConverters(parameterConverters)
                // use '%' instead of '$' to identify parameters
                .useStepPatternParser(new RegexPrefixCapturingPatternParser("%"))
                .useStepMonitor(contextStepMonitor);
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        MyContext context = new MyContext();
        return new InstanceStepsFactory(configuration(), new TraderSteps(new TradingService()), new AndSteps(),
                new MetaParametrisationSteps(), new CalendarSteps(), new PriorityMatchingSteps(), new PendingSteps(),
                new SandpitSteps(), new SearchSteps(), new BeforeAfterSteps(), new CompositeSteps(),
                new NamedParametersSteps(), new ExamplesTableParametersSteps(), new TableSteps(), 
                new ContextSteps(context), new RestartingSteps(), new JsonSteps());
    }

    @Override
    protected List<String> storyPaths() {
        String filter = System.getProperty("story.filter", "**/*.story");
        return new StoryFinder().findPaths(codeLocationFromClass(this.getClass()), filter, "**/failing_before*.story,**/given_relative_path*");
    }
}
