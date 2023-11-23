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

public class SampleBulkAction implements StreamProcessorTopology {

    private static final Logger logger = LogManager.getLogger(SampleBulkAction.class);

    public static final String ACTION_NAME = "sample";
    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;
    
    @Override
    public Topology getTopology(Map<String, String> options) {
        boolean failOnError = false;
        return Topology.builder()
                       .addComputation(() -> new AutomationComputation(ACTION_FULL_NAME, failOnError),
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .addComputation(() -> new SampleDoneComputation("bulk/sampleDone",failOnError), 
                               Collections.singletonList(INPUT_1 + ":" + DONE_STREAM))
                       .build();
    }

    public static class SampleDoneComputation extends AbstractComputation {

        protected final boolean failOnError;
        protected Codec<BulkStatus> codec;

        String OPERATION_NAME = "Document.SetProperty";

        public SampleDoneComputation(String name, boolean failOnError) {
            super("SampleDone", 1, 1);
            this.failOnError = false;
        }

        @Override
        public void init(ComputationContext context) {
            super.init(context);
            this.codec = BulkCodecs.getStatusCodec();
        }

        @Override
        public void processRecord(ComputationContext context, String inputStream, Record record) {
            BulkStatus status = codec.decode(record.getData());
            BulkService bulkService = Framework.getService(BulkService.class);
            BulkCommand command = bulkService.getCommand(status.getId());
            String parentId = (String) command.getParam("parentId");
            String message = status.toString();

            if (parentId != null) {

                int timeout = (int) requireNonNullElse(command.getBatchTransactionTimeout(), Duration.ZERO).toSeconds();

                TransactionHelper.runInTransaction(timeout, () -> {
                    try {
                        String username = command.getUsername();
                        String repository = command.getRepository();
                        try (NuxeoLoginContext ignored = loginSystemOrUser(username)) {
                            CoreSession session = CoreInstance.getCoreSession(repository);
                            updateParent(session, parentId, message); 
                        }
                    } catch (LoginException e) {
                        throw new NuxeoException(e);
                    }
                });

            }

            context.askForCheckpoint();
        }

        protected NuxeoLoginContext loginSystemOrUser(String username) throws LoginException {
            return SYSTEM_USERNAME.equals(username) ? Framework.loginSystem() : Framework.loginUser(username);
        }

        public void updateParent(CoreSession session, String parentId, String message) {
            AutomationService as = Framework.getService(AutomationService.class);
            OperationContext ctx = new OperationContext(session);
            Map<String, Object> params = new HashMap<String, Object>();
            DocumentModel doc = session.getDocument(new IdRef(parentId)); 
            ctx.setInput(doc);
            params.put("xpath", "dc:description");
            params.put("value", message);
            params.put("save", true);

            try {
                Object result = as.run(ctx, OPERATION_NAME, params);
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
        }
    }
}
