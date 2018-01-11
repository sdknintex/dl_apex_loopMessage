/* This sample shows how to use the Loop.LoopMessage class in the context of
 * a JavaScript button and List View or Related List.
 * The Trigger, Class, and Test Class below are defined separately.
 * All queries, objects, and fields are samples and can be replaced with the appropriate references.
 */

global class MySender {
    public class MissingDDPInfoException extends Exception { }
    
    public webService static string myApexMethod(string stringIds) {
        List<Id> ids = stringIds.split(',');
        
        // CHANGE ANY FILTER (the WHERE clause) CRITERIA NECESSARY
        List<Loop__DDP__c> ddps = [SELECT Id, (SELECT Id FROM Loop__Custom_Integration_Options__r WHERE Name = 'My Delivery Option') FROM Loop__DDP__c WHERE Name = 'My DocGen Package'];
        
        if (ddps == null || ddps.size() < 1) {
            // CHANGE THE EXCEPTION MESSAGE IF DESIRED
            throw new MissingDDPInfoException('A DDP specified was not found.');
        }
        
        Loop__DDP__c ddp = ddps[0];
        
        if (ddp.Loop__Custom_Integration_Options__r == null || ddp.Loop__Custom_Integration_Options__r.size() < 1) {
            // CHANGE THE EXCEPTION MESSAGE IF DESIRED
            throw new MissingDDPInfoException('A Delivery Option specified was not found.');
        }
        
        Id deliveryId = ddp.Loop__Custom_Integration_Options__r[0].Id;
        
        Loop.loopMessage lm = new Loop.loopMessage();
        
        // SESSION ID NEEDED IF IT CANNOT BE DETERMINED FROM UserInfo.getSessionId()
        lm.sessionId = sessionId;
        
        // SET DESIRED BATCH NOTIFICATION. IF THIS IS NOT DONE, THE DEFAULT IS 'NONE'
        // THIS IS AVAILABLE IN LOOP 6.7 / 9.56 AND ABOVE
        lm.batchNotification = Loop.loopMessage.Notification.BEGIN_AND_COMPLETE;
        //lm.batchNotification = Loop.loopMessage.Notification.ON_COMPLETE;
        //lm.batchNotification = Loop.loopMessage.Notification.ON_ERROR;
        
        // LOOP THROUGH WHATEVER COLLECTIONS NECESSARY FOR THE DDP REQUESTS
        for (Contact c : [SELECT Id FROM Contact WHERE Id IN :ids]) {
            // ADD A DDP RUN REQUEST
            lm.requests.add(new Loop.loopMessage.loopMessageRequest(
                c.Id, // MAIN RECORD ID - SAME OBJECT AS THE DDP RECORD TYPE SPECIFIES
                ddp.Id, // DDP ID
                new Map<string, string>{
                    'deploy' => deliveryId,
                    'SFAccount' => c.AccountId
                    // THESE PARAMETERS ARE THE SAME AS THOSE FOUND IN OUR OUTBOUND MESSAGE DOCUMENTATION
                    // PLEASE REFERENCE THAT DOCUMENTATION FOR ADDITIONAL OPTIONS
                    // 'SFObject_Name__c' => recordId
                    // 'attachIds' => attachmentIdsVariable
                    // 'deploytype' => 'autodocusign' // IF deploy IS A DOCUSIGN DELIVERY OPTION ID
                    // 'deploytype' => 'autoemail' // IF deploy IS AN EMAIL DELIVERY OPTION ID
                }
            ));
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
        Account a = new Account(Name = 'test');
        insert a;
        
        Contact c = new Contact(LastName = 'test', AccountId = a.Id);
        insert c;
        
        // SEND DDPS WITHOUT DDP DATA
        try {
            MySender.myApexMethod(string.valueOf(c.Id));
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
