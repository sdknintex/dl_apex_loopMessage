/* This sample shows how to use the Loop.LoopMessage class in the context of
 * a JavaScript button and List View or Related List.
 * The Trigger, Class, and Test Class below are defined separately.
 * All queries, objects, and fields are samples and can be replaced with the appropriate references.
 */

global class MySender {
    public class MissingDDPInfoException extends Exception { }
    
    public webService static string myApexMethod(string stringIds) {
        List<Id> ids = stringIds.split(',');
        
        // CHANGE ANY FILTER (WHERE) CRITERIA NECESSARY
        List<Loop__DDP__c> ddps = [SELECT Id, (SELECT Id FROM Loop__Custom_Integration_Options__r WHERE Name IN ('My Delivery Option','Document Queue')) FROM Loop__DDP__c WHERE Name IN ('My DDP','My Other DDP')];
        
        if (ddps == null || ddps.size() < 1) {
            // CHANGE THE EXCEPTION MESSAGE IF DESIRED
            throw new MissingDDPInfoException('A DDP specified was not found.');
        }
        else {
            for (Loop__DDP__c ddp : ddps) {
                if (ddp.Loop__Custom_Integration_Options__r == null || ddp.Loop__Custom_Integration_Options__r.size() < 1)
                    // CHANGE THE EXCEPTION MESSAGE IF DESIRED
                    throw new MissingDDPInfoException('A Delivery Option specified was not found.');
            }
        }
        
        // GET RECORDS TO RUN DDPS FOR
        Map<Id, List<SObject__c>> ddpIdsAndObjects = new Map<Id, List<SObject__c>>();
        for (SObject__c so : [SELECT Id, Fields FROM SObject__c WHERE Id IN :ids]) {
            for (Loop__DDP__c ddp : ddps) {
                if (/*CRITERIA THAT DETERMINES WHICH RECORD NEEDS WHICH DDP*/) {
                    if (!ddpIdsAndObjects.containsKey(ddp.Id))
                        ddpIdsAndObjects.put(ddp.Id, new List<SObject__c>());
                    ddpIdsAndObjects.get(ddp.Id).add(so);
                    break;
                }
            }
        }
        
        Loop.loopMessage lm = new Loop.loopMessage();
        
        // SESSION ID NEEDED IF IT CANNOT BE DETERMINED FROM UserInfo.getSessionId()
        lm.sessionId = sessionId;
        
        // SET DESIRED BATCH NOTIFICATION. IF THIS IS NOT DONE, THE DEFAULT IS 'NONE'
        // THIS IS AVAILABLE IN LOOP 6.7 / 9.56 AND ABOVE
        lm.batchNotification = Loop.loopMessage.Notification.BEGIN_AND_COMPLETE;
        //lm.batchNotification = Loop.loopMessage.Notification.ON_COMPLETE;
        //lm.batchNotification = Loop.loopMessage.Notification.ON_ERROR;
        
        // LOOP THROUGH WHATEVER COLLECTIONS NECESSARY FOR THE DDP REQUESTS
        for (Id ddpId : ddpIdsAndObjects.keySet()) {
            for (SObject__c myObject : ddpIdsAndObjects.get(ddpId)) {
                
                // ADD A DDP RUN REQUEST
                lm.requests.add(new Loop.loopMessage.loopMessageRequest(
                    myObject.Id, // MAIN RECORD ID - SAME OBJECT AS THE DDP RECORD TYPE SPECIFIES
                    ddpId, // DDP ID
                    new Map<string, string>{
                        'deploy' => 'autoemail', // THIS COULD BE A DELIVERY OPTION ID INSTEAD
                        'SFAccount' => myObject.Account__c,
                        'SFContact' => myObject.Contact__c
                        // THESE PARAMETERS ARE THE SAME AS THOSE FOUND IN OUR OUTBOUND MESSAGE DOCUMENTATION
                        // PLEASE REFERENCE THAT DOCUMENTATION FOR ADDITIONAL OPTIONS
                        // 'attachIds' => attachmentIdsVariable
                        // 'deploytype' => 'autodocusign' // IF deploy IS A DOCUSIGN DELIVERY OPTION ID
                        // 'deploytype' => 'autoemail' // IF deploy IS AN EMAIL DELIVERY OPTION ID
                    }
                ));
            }
        }
        // SEND ALL DDP RUN REQUESTS IN A SINGLE CALL OUT
        lm.sendAllRequests();
        
        return 'Your requests are being processed.';
    }
}


@isTest
private class MySenderTest {
    private static testMethod void testMySender() {
        // CREATE TEST DATA
        SObject__c so = new SObject__c();
        insert so;
        
        // SEND DDPS WITHOUT DDP DATA
        try {
            MySender.myApexMethod(string.valueOf(so.Id));
            system.assert(false);
        } catch (Exception ex) {
            system.assert(ex instanceOf MissingDDPInfoException);
        }
        
        // CREATE DDP TEST DATA
        Loop__DDP__c ddp = new Loop__DDP__c(Name='My DDP');
        insert ddp;
        
        Loop__DDP_Integration_Option__c delivOpt = new Loop__DDP_Integration_Option__c(Name='My Delivery Option', Loop__DDP__c=ddp.Id);
        insert delivOpt;
        
        // SEND DDPS WITH DDP DATA
        MySender.myApexMethod(string.valueOf(so.Id));
    }
}