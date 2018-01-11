/* This sample shows how to use the Loop.LoopMessage class in the context of
 * an Apex Trigger. The Trigger, Class, and Test Class below are defined separately.
 * All queries, objects, and fields are samples and can be replaced with the appropriate references.
 */

trigger SObjectTrigger on SObject__c (after update) {
    // CHANGE ANY FILTER (WHERE) CRITERIA NECESSARY
    Map<Id, SObject__c> mySObjects = new Map<Id, SObject__c>([
        SELECT Id, Contact__c, Contact__r.AccountId
        FROM SObject__c
        WHERE Id IN :Trigger.newMap.keySet() AND OtherCriteria = 'othervalue'
    ]);
    
    // ALTERNATIVELY, LOOP THROUGH Trigger.new TO DETERMINE WHICH RECORDS TO RUN THE DDP FOR
    
    // SEND DDP REQUESTS IF NECESSARY
    if (mySObjects.size() > 0) {
        MySender.sendRequests(JSON.serialize(mySObjects), userInfo.getSessionId());
    }
}


global class MySender {
    public class NoDDPInfoException extends Exception { }
    
    @future(callout=true)
    private static void sendRequests(string encodedObjects, string sessionId) {
        // CHANGE ANY FILTER (WHERE) CRITERIA NECESSARY
        List<Loop__DDP__c> ddps = [SELECT Id, (SELECT Id FROM Loop__Custom_Integration_Options__r WHERE Name='My Delivery Option') FROM Loop__DDP__c WHERE Name='My DDP'];
        
        // ALTERNATIVELY, DETERMINE WHICH DDP TO RUN FOR EACH RECORD
        
        if (ddps == null || ddps.size() < 1 || ddps[0].Loop__Custom_Integration_Options__r == null || ddps[0].Loop__Custom_Integration_Options__r.size() < 1) {
            // CHANGE THE EXCEPTION MESSAGE IF DESIRED
            throw new NoDDPInfoException('The DDP or Delivery Option specified was not found.');
        }
        
        Map<Id, SObject__c> mySObjects = (Map<Id, SObject__c>)JSON.deserialize(encodedObjects, Map<Id, SObject__c>.class);
        
        Loop.loopMessage lm = new Loop.loopMessage();
        
        // SESSION ID NEEDED IF IT CANNOT BE DETERMINED FROM UserInfo.getSessionId()
        lm.sessionId = sessionId;
        
        for (Id sObjectId : mySObjects.keySet()) {
            SObject__c mySObject = mySObjects.get(sObjectId);
            // ADD A DDP RUN REQUEST
            lm.requests.add(new Loop.loopMessage.loopMessageRequest(
                sObjectId, // MAIN RECORD ID - SAME OBJECT AS THE DDP RECORD TYPE SPECIFIES
                ddps[0].Id,
                new Map<string, string>{
                    'deploy' => ddps[0].Loop__Custom_Integration_Options__r[0].Id,
                    'SFAccount' => mySObject.Contact__r.AccountId,
                    'SFContact' => mySObject.Contact__c
                    // THESE PARAMETERS ARE THE SAME AS THOSE FOUND IN OUR OUTBOUND MESSAGE DOCUMENTATION
                    // PLEASE REFERENCE THAT DOCUMENTATION FOR ADDITIONAL OPTIONS
                }
            ));
        }
        // SEND ALL DDP RUN REQUESTS IN A SINGLE CALL OUT
        lm.sendAllRequests();
    }
}


@isTest
private class SObjectTriggerTest {
    private static testMethod void testSObjectTrigger() {
        // CREATE TEST DATA
        SObject__c so = new SObject__c();
        insert so;
        
        // UPDATE RECORD TO FIRE TRIGGER WITHOUT DDP INFO EXISTING
        so.OtherCriteria = 'othervalue';
        update so;
        
        // CREATE DDP TEST DATA
        Loop__DDP__c ddp = new Loop__DDP__c(Name='My DDP');
        insert ddp;
        
        Loop__DDP_Integration_Option__c delivOpt = new Loop__DDP_Integration_Option__c(Name='My Delivery Option', Loop__DDP__c=ddp.Id);
        insert delivOpt;
        
        // UPDATE RECORD TO FIRE TRIGGER WITH DDP INFO EXISTING
        update so;
    }
}