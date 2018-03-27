/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.pass.grant.cli;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Properties;

/**
 * Test classs for email service
 * @author jrm@jhu.edu
 */
public class EmailServiceTest {

    private Properties mailProperties = System.getProperties();
    private EmailService underTest;
    private GreenMail testServer;

    @Before
    public void setup() throws InterruptedException {
        mailProperties.setProperty("mail.transport.protocol","SMTP");
        mailProperties.put("mail.smtp.starttls.enable", "false");
        mailProperties.setProperty("mail.smtp.host","localhost");
        mailProperties.setProperty("mail.smtp.port","3025");
        mailProperties.setProperty("mail.smtp.user","testUser");
        mailProperties.setProperty("mail.smtp.password","na");
        mailProperties.put("mail.smtp.auth","false");
        mailProperties.setProperty("mail.from","no-reply@dataconservancy.org");
        mailProperties.setProperty("mail.to","luser@luser.com");

        underTest = new EmailService(mailProperties);

        boolean started = false;
        testServer = new GreenMail(ServerSetupTest.SMTP);
        try {
            testServer.start();
            started = true;
        } catch (RuntimeException e) {
            // ignore,
        }

        if (!started) {
            // try one more time
            Thread.sleep(5000l);
            testServer.start();
        }

    }

    /**
     *  Test for sending a message
     * @throws IOException if there was a problem getting the content of the message
     * @throws MessagingException if there was a problem  sending the message
     */
    @Test
    public void testSendMessage() throws MessagingException, IOException {
        String messageBody = "MOO";
        String messageSubject = "TEST";
        underTest.sendEmailMessage(messageSubject, messageBody);
        // Check that only one message was sent
        Integer numMessages = testServer.getReceivedMessages().length;
        Assert.assertTrue("Expected only one message, got " + numMessages, numMessages == 1);

        // Check that the message is just a plaintext message
        MimeMessage message = testServer.getReceivedMessages()[0];
        Assert.assertTrue("Subject of message was not correct", message.getSubject().equals(messageSubject));
        Assert.assertTrue("Content of message was not a string as expected", message.getContent() instanceof String);
        Assert.assertTrue(message.getContent().toString().contains(messageBody));
    }

    @After
    public void tearDown() {
        testServer.stop();
    }

}
