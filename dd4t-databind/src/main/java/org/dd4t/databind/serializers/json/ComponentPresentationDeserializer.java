package org.dd4t.databind.serializers.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.core.databind.BaseViewModel;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.databind.DataBindFactory;
import org.dd4t.databind.builder.json.JsonDataBinder;
import org.dd4t.databind.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * test
 *
 * @author R. Kempees
 */
public class ComponentPresentationDeserializer extends StdDeserializer<ComponentPresentation> {

	private static final Logger LOG = LoggerFactory.getLogger(ComponentPresentationDeserializer.class);
	private Class<? extends ComponentTemplate> concreteComponentTemplateClass = null;
	private Class<? extends Component> concreteComponentClass = null;

	public ComponentPresentationDeserializer (Class<? extends ComponentPresentation> componentPresentation, Class<? extends ComponentTemplate> componentTemplateClass, Class<? extends Component> concreteComponentClass) {
		super(componentPresentation);
		this.concreteComponentTemplateClass = componentTemplateClass;
		this.concreteComponentClass = concreteComponentClass;
	}


	@Override public ComponentPresentation deserialize (final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		final ObjectNode root = mapper.readTree(jsonParser);
		final ComponentPresentation componentPresentation = getConcreteComponentPresentation();

		if (!isConcreteClass(componentPresentation)) {
			return null;
		}

		final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

		JsonNode rawComponentData = null;
		String viewModelName = null;

		while (fields.hasNext()) {
			final Map.Entry<String, JsonNode> element = fields.next();
			final String key = element.getKey();

			LOG.trace(element.getKey() + "  " + element.getValue().toString());

			if (key.equalsIgnoreCase(Constants.COMPONENT_NODE_NAME)) {
				LOG.debug("Fishing out Component Data");
				rawComponentData = element.getValue();
				LOG.trace("Data is: {}", rawComponentData);
			} else if (key.equalsIgnoreCase(Constants.COMPONENT_TEMPLATE_NODE_NAME)) {
				LOG.debug("Deserializing Component Template Data.");
				final JsonParser parser = element.getValue().traverse();
				final ComponentTemplate componentTemplate = JsonDataBinder.getGenericMapper().readValue(parser, this.concreteComponentTemplateClass);
				componentPresentation.setComponentTemplate(componentTemplate);
				viewModelName = DataBindFactory.findComponentTemplateViewName(componentTemplate);
				LOG.debug("Found view model name: " + viewModelName);
			} else if (key.equalsIgnoreCase(Constants.IS_DYNAMIC_NODE)) {
				final String isDynamic = element.getValue().asText().toLowerCase();
				setIsDynamic(componentPresentation, isDynamic);
			} else if (key.equalsIgnoreCase(Constants.ORDER_ON_PAGE_NODE)) {
				componentPresentation.setOrderOnPage(element.getValue().asInt());
			} else if (key.equalsIgnoreCase(Constants.RENDERED_CONTENT_NODE)) {
				componentPresentation.setRenderedContent(element.getValue().asText());
			}
		}

		if (rawComponentData == null) {
			LOG.error("No component data found.");
			return componentPresentation;
		}

		try {
			renderComponentData(componentPresentation, rawComponentData, viewModelName, DataBindFactory.getRootElementName(rawComponentData));
		} catch (SerializationException e) {
			LOG.error(e.getLocalizedMessage(), e);
			throw new IOException(e);
		}
		return componentPresentation;
	}

	private void renderComponentData (final ComponentPresentation componentPresentation, final JsonNode rawComponentData, final String viewModelName, final String rootElementName) throws IOException, SerializationException {
		if (StringUtils.isEmpty(viewModelName) || DataBindFactory.renderGenericComponentsOnly()) {
			LOG.debug("No view name set on Component Template or only rendering to Generic Component");
			try {
				componentPresentation.setComponent(DataBindFactory.buildComponent(rawComponentData, this.concreteComponentClass));
			} catch (SerializationException e) {
				throw new IOException(e.getLocalizedMessage(), e);
			}
		} else {

			final HashSet<String> modelNames = new HashSet<>();
			modelNames.add(viewModelName);
			if (!rootElementName.equals(viewModelName)) {
				modelNames.add(rootElementName);
			}
			// TODO: explanation
			final Hashtable<String, BaseViewModel> models = DataBindFactory.buildModels(rawComponentData, modelNames, componentPresentation.getComponentTemplate().getId());

			if (models == null || models.isEmpty()) {
				if (DataBindFactory.renderDefaultComponentsIfNoModelFound()) {
					componentPresentation.setComponent(DataBindFactory.buildComponent(rawComponentData, this.concreteComponentClass));
				} else {
					LOG.warn("No model found for CT {}, with component: {}. Fall back deserialization is also turned off.", componentPresentation.getComponentTemplate().getId(), componentPresentation.getComponent().getId());
				}

			} else {
				for (BaseViewModel model : models.values()) {
					if (model.setGenericComponentOnComponentPresentation()) {
						LOG.debug("Also setting a Component object on the CP.");
						componentPresentation.setComponent(DataBindFactory.buildComponent(rawComponentData, this.concreteComponentClass));
					}
					if (model.setRawDataOnModel()) {
						LOG.debug("Setting raw string data on model.");
						model.setRawData(rawComponentData.toString());
					}
				}
				componentPresentation.setViewModel(models);
			}
		}
	}

	private boolean isConcreteClass (final ComponentPresentation componentPresentation) {
		// This check should be good enough
		if (componentPresentation == null || componentPresentation.getClass().isInterface()) {
			LOG.error("No concrete ComponentPresentation class found! not proceeding.");
			return false;
		}
		return true;
	}

	private ComponentPresentation getConcreteComponentPresentation () {

		final String handledType = this.handledType().toString();
		LOG.debug("Type for ComponentPresentation injection: {}", handledType);

		if (ComponentPresentation.class.isAssignableFrom(this.handledType())) {
			try {
				return (ComponentPresentation) this.handledType().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				LOG.error(e.getLocalizedMessage(), e);
				return null;
			}
		}
		LOG.error("Concrete type: " + this.handledType().toString() + " does not implement ComponentPresentation");
		return null;
	}

	private void setIsDynamic (final ComponentPresentation componentPresentation, final String isDynamic) {
		if (isDynamic.equalsIgnoreCase(Constants.TRUE_STRING) || isDynamic.equalsIgnoreCase(Constants.FALSE_STRING)) {
			componentPresentation.setIsDynamic(Boolean.parseBoolean(isDynamic));
		} else {
			componentPresentation.setIsDynamic(false);
		}
	}
}