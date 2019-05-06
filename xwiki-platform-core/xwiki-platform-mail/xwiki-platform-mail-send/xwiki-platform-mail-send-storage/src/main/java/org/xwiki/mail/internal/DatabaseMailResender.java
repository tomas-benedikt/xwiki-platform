/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.mail.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.SessionFactory;

/**
 * Implements mail resending by loading mail status from the database and messages from the filesystem.
 *
 * @version $Id$
 * @since 9.3RC1
 */
@Component
@Named("database")
@Singleton
public class DatabaseMailResender implements MailResender
{
    private static final String SESSION_BATCHID_KEY = "xwiki.batchId";

    @Inject
    private Logger logger;

    @Inject
    @Named("database")
    private Provider<MailListener> databaseMailListenerProvider;

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    @Named("database")
    private MailStatusStore store;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private MailSender mailSender;

    @Override
    public MailStatusResult resendAsynchronously(String batchId, String uniqueMessageId)
        throws MailStoreException
    {
        // Note: We don't need to check permissions since the caller already needs to know the batch id and mail id
        // to be able to call this method and for it to have any effect.

        // Set the batch id so that no new batch id is generated when re-sending the mail
        Session session = this.sessionFactory.create(Collections.singletonMap(SESSION_BATCHID_KEY, batchId));

        MimeMessage message = loadMessage(session, batchId, uniqueMessageId);

        MailListener databaseMailListener = this.databaseMailListenerProvider.get();

        this.mailSender.sendAsynchronously(Arrays.asList(message), session, databaseMailListener);

        return databaseMailListener.getMailStatusResult();
    }

    @Override
    public List<Pair<MailStatus, MailStatusResult>> resendAsynchronously(Map<String, Object> filterMap, int offset,
        int count) throws MailStoreException
    {
        List<Pair<MailStatus, MailStatusResult>> results = new ArrayList<>();

        List<MailStatus> statuses = this.store.load(filterMap, offset, count, null, true);

        for (MailStatus status : statuses) {
            try {
                results.add(new ImmutablePair<>(status,
                    resendAsynchronously(status.getBatchId(), status.getMessageId())));
            } catch (MailStoreException e) {
                // Failed to load the message from the content store and thus the mail couldn't be resent
                // Log a warning but continue to try to send the other mails...
                this.logger.warn("Failed to load mail content for batchId [{}], messageId [{}]. Root cause [{}]",
                    status.getBatchId(), status.getMessageId(), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return results;
    }

    private MimeMessage loadMessage(Session session, String batchId, String mailId) throws MailStoreException
    {
        MimeMessage message = this.mailContentStore.load(session, batchId, mailId);
        return message;
    }
}
