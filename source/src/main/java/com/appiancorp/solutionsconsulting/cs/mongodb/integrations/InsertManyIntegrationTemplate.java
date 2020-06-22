package com.appiancorp.solutionsconsulting.cs.mongodb.integrations;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.*;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.appiancorp.solutionsconsulting.cs.mongodb.exceptions.InvalidJsonException;
import com.appiancorp.solutionsconsulting.cs.mongodb.exceptions.MissingCollectionException;
import com.appiancorp.solutionsconsulting.cs.mongodb.exceptions.MissingDatabaseException;
import com.appiancorp.solutionsconsulting.cs.mongodb.operations.InsertManyOperation;
import com.mongodb.MongoException;
import com.mongodb.MongoExecutionTimeoutException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.appiancorp.solutionsconsulting.cs.mongodb.MongoDbConnectedSystemConstants.*;

@TemplateId(name = "InsertManyIntegrationTemplate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class InsertManyIntegrationTemplate extends MongoDbIntegrationTemplate {

    @Override
    protected SimpleConfiguration getConfiguration(
            SimpleConfiguration integrationConfiguration,
            SimpleConfiguration connectedSystemConfiguration,
            PropertyPath propertyPath,
            ExecutionContext executionContext
    ) {
        this.setupConfiguration(integrationConfiguration, connectedSystemConfiguration, propertyPath, executionContext);

        // Set defaults
        String insertSource = integrationConfiguration.getValue(INSERT_SOURCE);
        if (insertSource == null || StringUtils.isEmpty(insertSource)) {
            insertSource = INSERT_SOURCE_JSON;
            integrationConfiguration.setValue(INSERT_SOURCE, insertSource);
        }
        if (integrationConfiguration.getValue(INSERT_FILE_IS_ARRAY) == null) {
            integrationConfiguration.setValue(INSERT_FILE_IS_ARRAY, true);
        }

        propertyDescriptorsUtil.buildOutputTypeProperty();
        propertyDescriptorsUtil.buildDatabaseProperty();
        propertyDescriptorsUtil.buildCollectionsProperty();

        if (integrationConfiguration.getValue(COLLECTION) != null) {
            propertyDescriptors.add(TextPropertyDescriptor.builder()
                    .key(INSERT_SOURCE)
                    .label("JSON Source")
                    .instructionText("Whether to insert a JSON string representing an array of MongoDB Documents, or import from an Appian Document")
                    .choices(
                            Choice.builder().name(INSERT_SOURCE_JSON).value(INSERT_SOURCE_JSON).build(),
                            Choice.builder().name(INSERT_SOURCE_DOCUMENT).value(INSERT_SOURCE_DOCUMENT).build()
                    )
                    .refresh(RefreshPolicy.ALWAYS)
                    .isExpressionable(true)
                    .isRequired(true)
                    .build()
            );

            if (insertSource.equals(INSERT_SOURCE_JSON))
                propertyDescriptors.add(TextPropertyDescriptor.builder()
                        .key(INSERT_MANY_JSON)
                        .label("Insert Many JSON Array")
                        .description("A JSON string representing an array of Documents to be inserted in the form of: [{...},{...}].")
                        .isExpressionable(true)
                        .displayHint(DisplayHint.EXPRESSION)
                        .isRequired(true)
                        .build()
                );
            else
                propertyDescriptorsUtil.buildFileInputProperty();

            propertyDescriptorsUtil.buildInsertOptionsProperties();
        }

        return integrationConfiguration.setProperties(propertyDescriptors.toArray(new PropertyDescriptor[0]));
    }


    @Override
    protected IntegrationResponse execute(
            SimpleConfiguration integrationConfiguration,
            SimpleConfiguration connectedSystemConfiguration,
            ExecutionContext executionContext
    ) {
        this.setupExecute("MongoCollection.insertMany()", integrationConfiguration, connectedSystemConfiguration, executionContext);

        InsertManyOperation insertManyOperation;
        try {
            insertManyOperation = new InsertManyOperation(
                    integrationConfiguration.getValue(DATABASE),
                    integrationConfiguration.getValue(DATABASE_EXISTS),
                    integrationConfiguration.getValue(COLLECTION),
                    integrationConfiguration.getValue(COLLECTION_EXISTS),

                    integrationConfiguration.getValue(OUTPUT_TYPE),
                    integrationConfiguration.getValue(INSERT_SOURCE),
                    integrationConfiguration.getValue(INSERT_FILE_ID),
                    integrationConfiguration.getValue(INSERT_FILE_IS_ARRAY),
                    integrationConfiguration.getValue(INSERT_MANY_JSON),

                    integrationConfiguration.getValue(INSERT_SKIP_DATETIME_CONVERSION)
            );
        } catch (InvalidJsonException e) {
            return csUtil.buildApiExceptionError(
                    e.getMessage(),
                    "Invalid JSON string: \"" + e.jsonString + "\"");
        }

        csUtil.addAllRequestDiagnostic(insertManyOperation.getRequestDiagnostic());

        Map<String, Object> output = new HashMap<>();

        csUtil.startTiming();

        output.put("database", insertManyOperation.getDatabaseName());
        output.put("collection", insertManyOperation.getCollectionName());

        try {
            output.put("documents", mongoDbUtility.insertMany(insertManyOperation));

        } catch (MongoExecutionTimeoutException ex) {
            return csUtil.buildApiExceptionError(
                    "Max Processing Time Exceeded",
                    ex.getMessage());
        } catch (MissingCollectionException e) {
            return csUtil.buildApiExceptionError(
                    "Missing Collection Exception",
                    e.getMessage());
        } catch (MissingDatabaseException e) {
            return csUtil.buildApiExceptionError(
                    "Missing Database Exception",
                    e.getMessage());
        } catch (UnsupportedOperationException e) {
            return csUtil.buildApiExceptionError(
                    "Unsupported Operation Exception",
                    e.getMessage());
        } catch (MongoException e) {
            return csUtil.buildApiExceptionError(
                    "Mongo Exception",
                    e.getMessage());
        } catch (Exception e) {
            return csUtil.buildApiExceptionError(
                    "Exception",
                    e.getMessage());
        }

        csUtil.stopTiming();

        csUtil.addAllResponse(output);

        return csUtil.buildSuccess();
    }
}
