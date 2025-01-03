/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.changes.announcement.mailsender;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Date;
import java.util.Properties;

import org.codehaus.plexus.mailsender.AbstractMailSender;
import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.mailsender.util.DateFormatUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Helper class for sending email.
 */
public class ProjectJavamailMailSender extends AbstractMailSender {
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    private Properties props;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize() {
        if (StringUtils.isEmpty(getSmtpHost())) {
            getLogger().error("Error in configuration: Missing smtpHost.");
        }

        if (getSmtpPort() == 0) {
            setSmtpPort(DEFAULT_SMTP_PORT);
        }

        props = new Properties();

        props.put("mail.smtp.host", getSmtpHost());

        props.put("mail.smtp.port", String.valueOf(getSmtpPort()));

        if (getUsername() != null) {
            props.put("mail.smtp.auth", "true");
        }

        props.put("mail.debug", String.valueOf(getLogger().isDebugEnabled()));

        if (isSslMode()) {
            props.put("mail.smtp.socketFactory.port", String.valueOf(getSmtpPort()));

            props.put("mail.smtp.socketFactory.class", SSL_FACTORY);

            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        if (isTlsEnabled()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
    }

    // ----------------------------------------------------------------------
    // MailSender Implementation
    // ----------------------------------------------------------------------
    @Override
    public void send(MailMessage mail) throws MailSenderException {
        verify(mail);

        try {
            Authenticator auth = null;

            if (getUsername() != null) {
                auth = new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(getUsername(), getPassword());
                    }
                };
            }

            Session session = Session.getDefaultInstance(props, auth);

            session.setDebug(getLogger().isDebugEnabled());

            Message msg = new MimeMessage(session);
            InternetAddress addressFrom = new InternetAddress(mail.getFrom().getRfc2822Address());
            msg.setFrom(addressFrom);

            if (mail.getToAddresses().size() > 0) {
                InternetAddress[] addressTo =
                        new InternetAddress[mail.getToAddresses().size()];
                int count = 0;
                for (Object o : mail.getToAddresses()) {
                    String address = ((MailMessage.Address) o).getRfc2822Address();
                    addressTo[count++] = new InternetAddress(address);
                }
                msg.setRecipients(Message.RecipientType.TO, addressTo);
            }

            if (mail.getCcAddresses().size() > 0) {
                InternetAddress[] addressCc =
                        new InternetAddress[mail.getCcAddresses().size()];
                int count = 0;
                for (Object o : mail.getCcAddresses()) {
                    String address = ((MailMessage.Address) o).getRfc2822Address();
                    addressCc[count++] = new InternetAddress(address);
                }
                msg.setRecipients(Message.RecipientType.CC, addressCc);
            }

            if (mail.getBccAddresses().size() > 0) {
                InternetAddress[] addressBcc =
                        new InternetAddress[mail.getBccAddresses().size()];
                int count = 0;
                for (Object o : mail.getBccAddresses()) {
                    String address = ((MailMessage.Address) o).getRfc2822Address();
                    addressBcc[count++] = new InternetAddress(address);
                }
                msg.setRecipients(Message.RecipientType.BCC, addressBcc);
            }

            // Setting the Subject and Content Type
            msg.setSubject(mail.getSubject());
            msg.setContent(mail.getContent(), mail.getContentType() == null ? "text/plain" : mail.getContentType());

            if (mail.getSendDate() != null) {
                msg.setHeader("Date", DateFormatUtils.getDateHeader(mail.getSendDate()));
            } else {
                msg.setHeader("Date", DateFormatUtils.getDateHeader(new Date()));
            }

            // Send the message
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new MailSenderException("Error while sending mail.", e);
        }
    }
}
