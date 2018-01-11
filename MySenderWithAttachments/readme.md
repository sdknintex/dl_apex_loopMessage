MySenderWithAttachments sample
==============================

Demonstrates how to use the loopMessage and loopMessageRequest classes to run multiple DocGen Package requests immediately, synchronously and in the foreground, by using a custom button, and include attachments from related objects.

Overview
--------

The sample is a single file, named 4 MySenderWithAttachments (LoopMessage in future).java, that contains the following items:

* MySender

    An Apex class that uses the loopMessage and loopMessageRequest classes to request a DocGen Package run for each specified Salesforce object.

* MySenderTest

    An Apex class you can use to test the MySender class.

Implementing the sample
-----------------------

You need to implement the MySender Apex class in Salesforce by using the Developer Console.

To implement the sample

1. Log into your Salesforce instance, using a Developer Edition account.

1. In a text editor, open the sample file named 4 MySenderWithAttachments (LoopMessage in future).java.

1. From your Salesforce instance, open the Developer Console.

    For more information about the Developer Console in Salesforce, see [Developer Console](https://developer.salesforce.com/page/Developer_Console).

1. From the Developer Console, create a new Apex class named MySender.

1. Select the code for the MySender class from the sample file, copy the selected code, and then paste the code into the Developer Console, overwriting the existing contents.

1. Modify the code for the class as needed, where identified by comments in the code for the class.

1. Save the new Apex class, and then close the Developer Console.

Using the sample
----------------

You can test the sample by creating a custom button that directly invokes the myApexMethod method of the MySender class.