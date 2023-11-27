package org.nuxeo.sample.bulk.actions.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.lang.InterruptedException;
import java.util.HashMap;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.sample.bulk.actions.SampleBulkAction;
import org.nuxeo.sample.bulk.actions.SimplifiedSampleBulkAction;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.sample.sample-bulk-action")
public class TestSampleBulkAction {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected CoreSession session;

    @Test
    public void shouldLaunchSampleBulkAction() {

        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        String parentId = domain.getId();

        DocumentModel fileDoc = session.createDocumentModel("/","fileDoc","File");
        fileDoc = session.createDocument(fileDoc);
        session.saveDocument(fileDoc);
        
        var automationParams = new HashMap<>();
        automationParams.put("properties","dc:description=hi");

        String query = "SELECT * FROM File";
        BulkCommand bulkCommand = new BulkCommand.Builder(SampleBulkAction.ACTION_NAME, query, "system")
                .repository(session.getRepositoryName())
                .param("operationId", "Document.Update")
                .param("parameters", automationParams)
                .param("parentId", parentId)
                .build();

        String commandId = bulkService.submit(bulkCommand);

        txFeature.nextTransaction();

        BulkStatus status = bulkService.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());

        domain = session.getDocument(new PathRef("/default-domain"));
        assertNotNull(domain.getPropertyValue("dc:description"));

        fileDoc = session.getDocument(new PathRef("/fileDoc"));
        assertEquals("hi",fileDoc.getPropertyValue("dc:description"));

    }

    @Test
    public void shouldLaunchSimplifiedSampleBulkAction() {

        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        String parentId = domain.getId();

        DocumentModel fileDoc = session.createDocumentModel("/","fileDoc","File");
        fileDoc = session.createDocument(fileDoc);
        session.saveDocument(fileDoc);
        
        var automationParams = new HashMap<>();
        automationParams.put("properties","dc:description=hi");

        String query = "SELECT * FROM File";
        BulkCommand bulkCommand = new BulkCommand.Builder(SimplifiedSampleBulkAction.ACTION_NAME, query, "system")
                .repository(session.getRepositoryName())
                .param("operationId", "Document.Update")
                .param("parameters", automationParams)
                .param("parentId", parentId)
                .build();

        String commandId = bulkService.submit(bulkCommand);

        txFeature.nextTransaction();

        BulkStatus status = bulkService.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());

        domain = session.getDocument(new PathRef("/default-domain"));
        assertNotNull(domain.getPropertyValue("dc:description"));

        fileDoc = session.getDocument(new PathRef("/fileDoc"));
        assertEquals("hi",fileDoc.getPropertyValue("dc:description"));

    }
}
