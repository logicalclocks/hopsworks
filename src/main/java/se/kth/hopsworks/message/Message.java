package se.kth.hopsworks.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;

@Entity
@Table(name = "hopsworks.message")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Message.findAll",
          query
          = "SELECT m FROM Message m"),
  @NamedQuery(name = "Message.findById",
          query
          = "SELECT m FROM Message m WHERE m.id = :id"),
  @NamedQuery(name = "Message.findByDateSent",
          query
          = "SELECT m FROM Message m WHERE m.dateSent = :dateSent"),
  @NamedQuery(name = "Message.findByFrom",
          query
          = "SELECT m FROM Message m WHERE m.from = :from"),
  @NamedQuery(name = "Message.findMessagesByToAndUnread",
          query
          = "SELECT m FROM Message m WHERE m.unread = :unread AND m.to = :to"),
  @NamedQuery(name = "Message.countByToAndUnread",
          query
          = "SELECT COUNT(m.id) FROM Message m WHERE m.to = :to AND m.unread = :unread"),
  @NamedQuery(name = "Message.findByToAndDeleted",
          query
          = "SELECT m FROM Message m WHERE m.to = :to AND m.deleted = :deleted  ORDER BY m.unread DESC, m.dateSent DESC"),
  @NamedQuery(name = "Message.emptyToAndDeleted",
          query
          = "DELETE FROM Message m WHERE m.to = :to AND m.deleted = :deleted"),
  @NamedQuery(name = "Message.countByToAndDeleted",
          query
          = "SELECT COUNT(m.id) FROM Message m WHERE m.to = :to AND m.deleted = :deleted")})
public class Message implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "date_sent")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateSent;
  @Size(max = 128)
  @Column(name = "subject")
  private String subject;
  @Size(max = 128)
  @Column(name = "preview")
  private String preview;
  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
          max = 65535)
  @Column(name = "content")
  private String content;
  @Basic(optional = false)
  @NotNull
  @Column(name = "unread")
  private boolean unread;
  @Basic(optional = false)
  @NotNull
  @Column(name = "deleted")
  private boolean deleted;
  @Size(max = 600)
  @Column(name = "path")
  private String path;
  @JoinTable(name = "hopsworks.message_to_user",
          joinColumns
          = {
            @JoinColumn(name = "message",
                    referencedColumnName = "id")},
          inverseJoinColumns
          = {
            @JoinColumn(name = "user_email",
                    referencedColumnName = "email")})
  @ManyToMany
  private Collection<Users> usersCollection;
  @JoinColumn(name = "user_from",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users from;
  @JoinColumn(name = "user_to",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users to;

  public Message() {
  }

  public Message(Integer id) {
    this.id = id;
  }

  public Message(Users from, Users to, Date dateSent, String content, boolean unread,
          boolean deleted) {
    this.from = from;
    this.to = to;
    this.dateSent = dateSent;
    this.content = content;
    this.unread = unread;
    this.deleted = deleted;
  }
  
  public Message(Users from, Users to, Collection<Users> recipients, Date dateSent, String content, boolean unread,
          boolean deleted) {
    this.from = from;
    this.to = to;
    this.usersCollection = recipients;
    this.dateSent = dateSent;
    this.content = content;
    this.unread = unread;
    this.deleted = deleted;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getDateSent() {
    return dateSent;
  }

  public void setDateSent(Date dateSent) {
    this.dateSent = dateSent;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getPreview() {
    return preview;
  }

  public void setPreview(String preview) {
    this.preview = preview;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public boolean getUnread() {
    return unread;
  }

  public void setUnread(boolean unread) {
    this.unread = unread;
  }

  public boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Collection<Users> getUsersCollection() {
    return usersCollection;
  }

  public void setUsersCollection(Collection<Users> usersCollection) {
    this.usersCollection = usersCollection;
  }

  public Users getFrom() {
    return from;
  }

  public void setFrom(Users from) {
    this.from = from;
  }

  public Users getTo() {
    return to;
  }

  public void setTo(Users to) {
    this.to = to;
  }

  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Message)) {
      return false;
    }
    Message other = (Message) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.message.Message[ id=" + id + " ]";
  }

}
