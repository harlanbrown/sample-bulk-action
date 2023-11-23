# sample-bulk-action
Example bulk action with a computation triggered by done stream

To trigger the bulk action:
`curl -u Administrator:Administrator localhost:8080/nuxeo/api/v1/search/bulk/sample?query=SELECT%20%2A%20FROM%20File -d '{"operationId":"Document.Update","parameters":{"properties":"dc:description=foo"},"parentId":"91bb432e-24b0-4135-a904-cf9391d4f28e"}' -H content-type:application/json`

Parameters to action are the same as for the AutomationBulkAction with the addition of the id of the document to be updated (parentId) when the automation bulk action completes.

