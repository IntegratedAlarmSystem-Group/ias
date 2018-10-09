package org.eso.ias.cdb.pojos;

import javax.persistence.*;
import java.util.Objects;

/**
 * The instance of an IASIO.
 *
 * This is the input of an ASCE when the template is not the same as the one
 * of the ASCE
 *
 * @see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#124</A>
 */
@Entity
@Table(name = "TEMPL_INST_IASIO")
public class TemplateInstanceIasioDao {

    /**
     * Empty constructor
     */
    public TemplateInstanceIasioDao() {}

    @Id
    @SequenceGenerator(name="TEMPL_INST_SEQ_GENERATOR", sequenceName="TEMPL_INST_SEQ_GENERATOR", allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="TEMPL_INST_SEQ_GENERATOR")
    @Column(name = "id")
    private Long id;

    /**
     * The template
     */
    @Basic(optional=false)
    @Column(name = "template_id")
    private String templateId;

    /**
     * The number of the instance of the IASIO
     */
    @Basic(optional=false)
    @Column(name = "instance_num")
    private int instance;

    /**
     * The IASIO definition of thie template instance
     */
    @ManyToOne
    @JoinColumn(name = "io_id", foreignKey = @ForeignKey(name = "io_id"))
    private IasioDao iasio;

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public IasioDao getIasio() {
        return iasio;
    }

    public void setIasio(IasioDao iasio) {
        this.iasio = iasio;
    }

    public String getTemplateId() { return templateId; }

    public void setTemplateId(String templateId) { this.templateId = templateId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateInstanceIasioDao that = (TemplateInstanceIasioDao) o;
        return instance == that.instance &&
                Objects.equals(templateId, that.templateId) &&
                Objects.equals(iasio, that.iasio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, instance, iasio);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("Templated input instance = [ IASIO id=");
        ret.append(iasio.getId());
        ret.append(", template id=");
        ret.append(templateId);
        ret.append(", instance #");
        ret.append(instance);
        ret.append(']');
        return ret.toString();
    }
}
