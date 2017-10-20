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
package com.xpn.xwiki.doc;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateDeletedAttachmentContent;
import com.xpn.xwiki.util.AbstractSimpleClass;
import com.xpn.xwiki.web.Utils;

/**
 * Archive of deleted attachment, stored in {@link com.xpn.xwiki.store.AttachmentRecycleBinStore}. Immutable, because
 * deleted attachments should not be modified.
 *
 * @version $Id$
 * @since 1.4M1
 */
public class DeletedAttachment extends AbstractSimpleClass
{
    /** Synthetic id, generated by Hibernate. This is used to address entries in the recycle bin. */
    private long id;

    /** The ID of the document this attachment belonged to. */
    private long docId;

    /** The reference of the document this attachment belonged to. */
    private String docName;

    /** The name of the attachment. */
    private String filename;

    /** Date of delete action. */
    private Date date;

    /** The user who deleted the attachment, in the <tt>XWiki.UserName</tt> format. */
    private String deleter;

    private String contentStore;

    private DeletedAttachmentContent content;

    /** Default constructor. Used only by hibernate when restoring objects from the database. */
    protected DeletedAttachment()
    {
    }

    /**
     * A constructor with all the information about the deleted attachment.
     *
     * @param docId the ID of the document this attachment belonged to
     * @param docName the reference of the document this attachment belonged to
     * @param filename the name of the attachment
     * @param storeType the way to store the document
     * @param attachment Deleted attachment.
     * @param deleter User which deleted the attachment.
     * @param deleteDate Date of delete action.
     */
    private DeletedAttachment(long docId, String docName, String filename, String storeType, String deleter,
        Date deleteDate)
    {
        this.docId = docId;
        this.docName = docName;
        this.filename = filename;
        this.deleter = deleter;
        this.date = deleteDate;
        this.contentStore = storeType;
    }

    /**
     * A constructor with all the information about the deleted attachment.
     *
     * @param attachment Deleted attachment.
     * @param deleter User which deleted the attachment.
     * @param deleteDate Date of delete action.
     * @param context The current context. Used for determining the encoding.
     * @throws XWikiException If the attachment cannot be exported to XML.
     */
    public DeletedAttachment(XWikiAttachment attachment, String deleter, Date deleteDate, XWikiContext context)
        throws XWikiException
    {
        this(attachment.getDocId(), attachment.getDoc().getFullName(), attachment.getFilename(), null, deleter,
            deleteDate);

        setAttachment(attachment, context);
    }

    /**
     * @param docId the ID of the document this attachment belonged to
     * @param docName the reference of the document this attachment belonged to
     * @param filename the name of the attachment
     * @param storeType the way to store the document
     * @param deleter the user who delete document
     * @param deleteDate date of delete action
     * @param content the stored deleted document
     * @since 9.10RC1
     */
    public DeletedAttachment(long docId, String docName, String filename, String storeType, String deleter,
        Date deleteDate, DeletedAttachmentContent content)
    {
        this(docId, docName, filename, storeType, deleter, deleteDate);

        this.content = content;
    }

    /**
     * @param docId the ID of the document this attachment belonged to
     * @param docName the reference of the document this attachment belonged to
     * @param filename the name of the attachment
     * @param storeType the way to store the document
     * @param deleter the user who delete document
     * @param deleteDate date of delete action
     * @param content the stored deleted document
     * @param id the synthetic id of this deleted attachment. Uniquely identifies an entry in the recycle bin.
     * @since 9.10RC1
     */
    public DeletedAttachment(long docId, String docName, String filename, String storeType, String deleter,
        Date deleteDate, DeletedAttachmentContent content, long id)
    {
        this(docId, docName, filename, storeType, deleter, deleteDate, content);

        this.id = id;
    }

    /**
     * Getter for {@link #id}.
     *
     * @return The synthetic id of this deleted attachment. Uniquely identifies an entry in the recycle bin.
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * Setter for {@link #id}.
     *
     * @param id The synthetic id to set. Used only by hibernate.
     */
    protected void setId(long id)
    {
        this.id = id;
    }

    /**
     * Getter for {@link #docId}.
     *
     * @return The id of the document this attachment belonged to.
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * Setter for {@link #docId}.
     *
     * @param docId The id of the document to set. Used only by hibernate.
     */
    protected void setDocId(long docId)
    {
        this.docId = docId;
    }

    /**
     * Getter for {@link #docName}.
     *
     * @return The name of the document this attachment belonged to.
     */
    public String getDocName()
    {
        return this.docName;
    }

    /**
     * Setter for {@link #docName}.
     *
     * @param docName The document name to set. Used only by hibernate.
     */
    protected void setDocName(String docName)
    {
        this.docName = docName;
    }

    /**
     * Getter for {@link #filename}.
     *
     * @return The name of the attachment.
     */
    public String getFilename()
    {
        return this.filename;
    }

    /**
     * Setter for {@link #filename}.
     *
     * @param filename The attachment filename to set. Used only by hibernate.
     */
    protected void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * @return the attachment reference for the deleted attachment
     * @since 9.9RCA
     */
    public AttachmentReference getAttachmentReference()
    {
        DocumentReference documentReference = getDocumentReferenceResolver().resolve(getDocName());

        return new AttachmentReference(getFilename(), documentReference);
    }

    private static DocumentReferenceResolver<String> getDocumentReferenceResolver()
    {
        return Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
    }

    /**
     * Getter for {@link #date}.
     *
     * @return The date of the delete action.
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * Setter for {@link #date}.
     *
     * @param date The date of the delete action to set. Used only by Hibernate.
     */
    protected void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Getter for {@link #deleter}.
     *
     * @return the user who deleted the attachment, as its document name (e.g. {@code XWiki.Admin})
     */
    public String getDeleter()
    {
        return this.deleter;
    }

    /**
     * Setter for {@link #deleter}.
     *
     * @param deleter The user which has removed the document to set. Used only by Hibernate.
     */
    protected void setDeleter(String deleter)
    {
        this.deleter = deleter;
    }

    /**
     * @return the type of the store used for the content
     * @since 9.10RC1
     */
    public String getContentStore()
    {
        return this.contentStore;
    }

    /**
     * @param xmlStore the type of store (supported values are null/"hibernate" and "file")
     * @since 9.10RC1
     */
    public void setContentStore(String xmlStore)
    {
        this.contentStore = xmlStore;
    }

    /**
     * Getter for {@link #content}.
     *
     * @return XML serialization of {@link XWikiAttachment}
     */
    public String getXml()
    {
        if (this.content != null) {
            try {
                return this.content.getContentAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Return empty String instead of null because this field is configured as not null at database level
        return "";
    }

    /**
     * Setter for {@link #content}.
     *
     * @param xml XML serialization of {@link XWikiAttachment}. Used only by Hibernate.
     */
    protected void setXml(String xml)
    {
        if (StringUtils.isNotEmpty(xml)) {
            try {
                this.content = new HibernateDeletedAttachmentContent(xml);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Export {@link XWikiAttachment} to {@link DeletedAttachment}.
     *
     * @param attachment the deleted attachment
     * @param context the current context, used in the XML export
     * @throws XWikiException if an exception occurs during the XML export
     * @deprecated since 9.9RC1, use
     *             {@link #DeletedAttachment(long, String, String, String, String, Date, DeletedAttachmentContent)}
     *             instead
     */
    @Deprecated
    protected void setAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        this.content = new HibernateDeletedAttachmentContent(attachment);
    }

    /**
     * Restore a {@link XWikiAttachment} from a {@link DeletedAttachment}. Note that this method does not actually
     * restore the attachment to its owner document, it simply re-composes an {@link XWikiAttachment} object from the
     * saved data.
     *
     * @param attachment optional object where to put the attachment data, if not <code>null</code>
     * @param context the current {@link XWikiContext context}
     * @return restored attachment
     * @throws XWikiException If an exception occurs while the Attachment is restored from the XML. See
     *             {@link XWikiAttachment#fromXML(String)}.
     * @deprecated since 9.9RC1, use {@link #restoreAttachment(XWikiAttachment)} instead
     */
    @Deprecated
    public XWikiAttachment restoreAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        return restoreAttachment(attachment);
    }

    /**
     * Restore a {@link XWikiAttachment} from a {@link DeletedAttachment}. Note that this method does not actually
     * restore the attachment to its owner document, it simply re-composes an {@link XWikiAttachment} object from the
     * saved data.
     *
     * @param attachment optional object where to put the attachment data, if not <code>null</code>
     * @return restored attachment
     * @throws XWikiException If an exception occurs while the Attachment is restored from the XML. See
     *             {@link XWikiAttachment#fromXML(String)}.
     */
    public XWikiAttachment restoreAttachment(XWikiAttachment attachment) throws XWikiException
    {
        try {
            return this.content.getXWikiAttachment(attachment);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error restoring document", e, null);
        }
    }

    /**
     * @return restored attachment
     * @throws XWikiException if error in {@link XWikiDocument#fromXML(String)}
     * @since 9.10RC1
     */
    public XWikiAttachment restoreAttachment() throws XWikiException
    {
        return restoreAttachment(null);
    }

}
