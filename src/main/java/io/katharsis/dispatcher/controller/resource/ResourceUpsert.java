package io.katharsis.dispatcher.controller.resource;

import io.katharsis.dispatcher.controller.BaseController;
import io.katharsis.repository.RelationshipRepository;
import io.katharsis.request.dto.DataBody;
import io.katharsis.request.dto.Linkage;
import io.katharsis.request.dto.RequestBody;
import io.katharsis.resource.ResourceInformation;
import io.katharsis.resource.exception.ResourceException;
import io.katharsis.resource.exception.ResourceNotFoundException;
import io.katharsis.resource.registry.RegistryEntry;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.utils.parser.TypeParser;
import org.apache.commons.beanutils.PropertyUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class ResourceUpsert implements BaseController {
    protected ResourceRegistry resourceRegistry;
    protected TypeParser typeParser;

    public ResourceUpsert(ResourceRegistry resourceRegistry, TypeParser typeParser) {
        this.resourceRegistry = resourceRegistry;
        this.typeParser = typeParser;
    }

    protected void setAttributes(RequestBody requestBody, Object instance, ResourceInformation resourceInformation)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        if (requestBody.getData().getAttributes() != null) {
            for (Map.Entry<String, Object> property : requestBody.getData().getAttributes().getAttributes().entrySet()) {
                Field attributeField = resourceInformation.findAttributeFieldByName(property.getKey());
                PropertyUtils.setProperty(instance, attributeField.getName(), property.getValue());
            }
        }
    }

    protected void saveRelations(Object savedResource, RegistryEntry registryEntry, RequestBody requestBody)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (requestBody.getData().getRelationships() != null) {
            Map<String, Object> additionalProperties = requestBody.getData().getRelationships().getAdditionalProperties();
            for (Map.Entry<String, Object> property : additionalProperties.entrySet()) {
                if (Iterable.class.isAssignableFrom(property.getValue().getClass())) {
                    saveRelationsField(savedResource, registryEntry, (Map.Entry) property, registryEntry.getResourceInformation());
                } else {
                    saveRelationField(savedResource, registryEntry, (Map.Entry) property, registryEntry.getResourceInformation());
                }

            }
        }
    }

    private void saveRelationsField(Object savedResource, RegistryEntry registryEntry, Map.Entry<String,
            Iterable<Linkage>> property, ResourceInformation resourceInformation)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (!allTypesTheSame(property.getValue())) {
            throw new ResourceException("Not all types are the same for linkage: " + property.getKey());
        }

        String type = getLinkageType(property.getValue());
        RegistryEntry relationRegistryEntry = getRelationRegistryEntry(type);
        Class<? extends Serializable> relationshipIdClass = (Class<? extends Serializable>) relationRegistryEntry
                .getResourceInformation()
                .getIdField()
                .getType();
        List<Serializable> castedRelationIds = new LinkedList<>();

        for (Linkage linkage : property.getValue()) {
            Serializable castedRelationshipId = typeParser.parse(linkage.getId(), relationshipIdClass);
            castedRelationIds.add(castedRelationshipId);
        }

        Class<?> relationshipClass = relationRegistryEntry.getResourceInformation().getResourceClass();
        RelationshipRepository relationshipRepository = registryEntry.getRelationshipRepositoryForClass(relationshipClass);
        Field relationshipField = resourceInformation.findRelationshipFieldByName(property.getKey());
        relationshipRepository.setRelations(savedResource, castedRelationIds, relationshipField.getName());
    }

    private boolean allTypesTheSame(Iterable<Linkage> linkages) {
        String type = linkages.iterator().hasNext() ? linkages.iterator().next().getType() : null;
        for (Linkage linkage : linkages) {
            if (!Objects.equals(type, linkage.getType())) {
                return false;
            }
        }
        return true;
    }

    private String getLinkageType(Iterable<Linkage> linkages) {
        return linkages.iterator().hasNext() ? linkages.iterator().next().getType() : null;
    }

    protected void saveRelationField(Object savedResource, RegistryEntry registryEntry, Map.Entry<String, Linkage>
            property,
                                   ResourceInformation resourceInformation)
            throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        RegistryEntry relationRegistryEntry = getRelationRegistryEntry(property.getValue().getType());

        Class<? extends Serializable> relationshipIdClass = (Class<? extends Serializable>) relationRegistryEntry
                .getResourceInformation()
                .getIdField()
                .getType();
        Serializable castedRelationshipId = typeParser.parse(property.getValue().getId(), relationshipIdClass);

        Class<?> relationshipClass = relationRegistryEntry.getResourceInformation().getResourceClass();
        RelationshipRepository relationshipRepository = registryEntry.getRelationshipRepositoryForClass(relationshipClass);
        Field relationshipField = resourceInformation.findRelationshipFieldByName(property.getKey());
        relationshipRepository.setRelation(savedResource, castedRelationshipId, relationshipField.getName());
    }

    private RegistryEntry getRelationRegistryEntry(String type) {
        RegistryEntry relationRegistryEntry = resourceRegistry.getEntry(type);
        if (relationRegistryEntry == null) {
            throw new ResourceNotFoundException(type);
        }
        return relationRegistryEntry;
    }

    protected Object buildNewResource(RegistryEntry registryEntry, RequestBody requestBody, String resourceName)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        DataBody data = requestBody.getData();
        if (data == null) {
            throw new RuntimeException("No data field in the body.");
        }
        if (!resourceName.equals(data.getType())) {
            throw new RuntimeException(String.format("Inconsistent type definition between path and body: body type: " +
                    "%s, request type: %s", data.getType(), resourceName));
        }
        Object resource = registryEntry.getResourceInformation().getResourceClass().newInstance();
        return resource;
    }
}
