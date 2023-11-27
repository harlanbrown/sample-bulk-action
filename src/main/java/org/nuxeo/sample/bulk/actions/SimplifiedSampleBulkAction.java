package org.nuxeo.sample.bulk.actions;

import static java.util.Objects.requireNonNullElse;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.LoginException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

import org.nuxeo.ecm.automation.core.operations.services.bulk.AbstractAutomationBulkAction.AutomationComputation;

public class SimplifiedSampleBulkAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "sample2";
    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;
    
    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new AutomationComputation(ACTION_FULL_NAME, false),
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .addComputation(() -> new SimplifiedSampleDoneComputation("bulk/sample2Done"), 
                               Collections.singletonList(INPUT_1 + ":" + DONE_STREAM))
                       .build();
    }

    public static class SimplifiedSampleDoneComputation extends AbstractComputation {

        protected Codec<BulkStatus> codec;

        public SimplifiedSampleDoneComputation(String name) {
            super("Sample2Done", 1, 1);
        }

        @Override
        public void init(ComputationContext context) {
            super.init(context);
            this.codec = BulkCodecs.getStatusCodec();
        }

        @Override
        public void processRecord(ComputationContext context, String inputStream, Record record) {
            BulkStatus status = codec.decode(record.getData());
            if (ACTION_NAME.equals(status.getAction())
                    && BulkStatus.State.COMPLETED.equals(status.getState())) {

                BulkService bulkService = Framework.getService(BulkService.class);
                BulkCommand command = bulkService.getCommand(status.getId());
                String parentId = (String) command.getParam("parentId");
                String message = status.toString();

                if (parentId != null) {

                    TransactionHelper.runInTransaction(300, () -> {
                        String repository = command.getRepository();
                        CoreSession session = CoreInstance.getCoreSession(repository);
                        updateParent(session, parentId, message); 
                    });

                }
            }

            context.askForCheckpoint();
        }


        public void updateParent(CoreSession session, String parentId, String message) {
            DocumentModel doc = session.getDocument(new IdRef(parentId)); 
            doc.setPropertyValue("dc:description", message);
    		session.saveDocument(doc);
        }
    }
}
