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

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {
    private static Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private static String ERR_ADDRESS = "Could not send email: an addressing exception occurred";
    private static String ERR_MESSAGING = "Could not send email: a messaging exception occurred";
    private static String ERR_ENCODING = "ould not send email; an encoding exception occurred";
    private Properties mailProperties;

    EmailService(Properties mailProperties){
        this.mailProperties = mailProperties;
    }

    public void sendEmailMessage(String message){
        try {
           // Session session = Session.getInstance(mailProperties, pwAuth);
            Session session = Session.getInstance(mailProperties,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(mailProperties
                                    .getProperty("mail.smtp.user"), mailProperties
                                    .getProperty("mail.smtp.password"));
                        }
                    });
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress((String) mailProperties.get("mail.from"), "COEUS Data Loader"));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(mailProperties.getProperty("mail.to"), "COEUS Client"));
            msg.setSubject("COEUS Data Loader Message");
            msg.setText(message);
            Transport.send(msg);
        } catch (AddressException e) {
            e.printStackTrace();
            LOG.error(ERR_ADDRESS ,e);
        } catch (MessagingException e) {
            e.printStackTrace();
            LOG.error(ERR_MESSAGING, e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LOG.error(ERR_ENCODING, e);
        }

    }
}
