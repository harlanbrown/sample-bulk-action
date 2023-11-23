package org.nuxeo.sample.bulk.actions.test;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.sample.bulk.actions.SampleBulkAction;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.sample.sample-bulk-action")
public class TestSampleBulkAction {

    @Inject
    protected BulkService bulkService;

    @Inject
    protected CoreSession session;
    
    @Test
    public void shouldLaunchSampleBulkAction() {
    
    	String query = "SELECT * FROM Domain";
        BulkCommand bulkCommand = new BulkCommand.Builder(SampleBulkAction.ACTION_NAME, query)
                .repository(session.getRepositoryName())
                .param(AutomationBulkAction.OPERATION_ID, "Document.Update")
                .param("properties", "dc:description=hi")
                .build();

        String commandId = bulkService.submit(bulkCommand);
        assertNotNull(commandId);
    }
	
}
