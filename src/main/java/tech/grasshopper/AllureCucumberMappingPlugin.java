package tech.grasshopper;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

public class AllureCucumberMappingPlugin implements ConcurrentEventListener {

	private Map<String, String> mapping = new ConcurrentHashMap<>();

	private String reportFile;

	private final static Logger logger = Logger.getLogger(AllureCucumberMappingPlugin.class.getName());

	public AllureCucumberMappingPlugin(String reportFile) {
		this.reportFile = reportFile;
	}

	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
		publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
	}

	public void handleTestCaseFinished(TestCaseFinished event) {
		AllureLifecycle lifecycle = Allure.getLifecycle();

		if (lifecycle.getCurrentTestCase().isPresent())
			mapping.put(relativize(event.getTestCase().getUri()) + ":" + event.getTestCase().getLocation().getLine(),
					lifecycle.getCurrentTestCase().get());
	}

	public void handleTestRunFinished(TestRunFinished event) {
		ObjectMapper mapper = new ObjectMapper();

		try {
			String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapping);

			try (FileWriter file = new FileWriter(reportFile)) {
				file.write(jsonResult);
				file.flush();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to create mapping file. - " + e.getMessage());
		}
	}

	private URI relativize(URI uri) {
		if (!"file".equals(uri.getScheme())) {
			return uri;
		}
		if (!uri.isAbsolute()) {
			return uri;
		}

		try {
			URI root = new File("").toURI();
			URI relative = root.relativize(uri);
			// Scheme is lost by relativize
			return new URI("file", relative.getSchemeSpecificPart(), relative.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}
