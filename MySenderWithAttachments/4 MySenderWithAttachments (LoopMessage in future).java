global class MySender {
    WebService static string myApexMethod(string stringIds) {
        List<Id> ids = stringIds.split(',');
        
        Map<Id, SObject__c> mySObjects = new Map<Id, SObject__c>([SELECT Id, Contact__c, Contact__r.AccountId, (SELECT Id FROM Attachments) FROM SObject__c WHERE Id IN :ids]);
        MySender.sendRequests(JSON.serialize(mySObjects), userInfo.getSessionId());
        return 'Your requests are being processed.';
    }
    
    @future(callout=true)
    private static void sendRequests(string encodedObjects, string sessionId) {
        Map<Id, SObject__c> mySObjects = (Map<Id, SObject__c>)JSON.deserialize(encodedObjects, Map<Id, SObject__c>.class);
        
        Loop.loopMessage lm = new Loop.loopMessage();
        
        // SESSION ID NEEDED IF IT CANNOT BE DETERMINED FROM UserInfo.getSessionId()
        lm.sessionId = sessionId;
        
        for (Id sObjectId : mySObjects.keySet()) {
            SObject__c mySObject = mySObjects.get(sObjectId);
            // GET ATTACHMENT IDS
            string attachIds = '';
            if (mySObject.Attachments != null) {
                List<Id> attachmentIds = new List<Id>();
                attachmentIds.addAll((new Map<Id, sObject>(mySObject.Attachments)).keySet());
                attachIds = string.join(attachmentIds, '|');
            }
            // ADD A DDP RUN REQUEST
            lm.requests.add(new Loop.loopMessage.loopMessageRequest(
                sObjectId, // MAIN RECORD ID - SAME OBJECT AS THE DDP RECORD TYPE SPECIFIES
                'DDP_ID', // HARD CODED DDP ID - LOGIC CAN BE ADDED TO MAKE THIS DYNAMIC
                new Map<string, string>{
                    'deploy' => 'queue', // HARD CODED DELIVERY - LOGIC CAN BE ADDED TO MAKE THIS DYNAMIC
                    'SFAccount' => mySObject.Contact__r.AccountId,
                    'SFContact' => mySObject.Contact__c,
                    'attachIds' => attachIds
                    // THESE PARAMETERS ARE THE SAME AS THOSE FOUND IN OUR OUTBOUND MESSAGE DOCUMENTATION
                    // PLEASE REFERENCE THAT DOCUMENTATION FOR ADDITIONAL OPTIONS
                }
            ));
        }
        // SEND ALL DDP RUN REQUESTS IN A SINGLE CALL OUT
        lm.sendAllRequests();
    }
}