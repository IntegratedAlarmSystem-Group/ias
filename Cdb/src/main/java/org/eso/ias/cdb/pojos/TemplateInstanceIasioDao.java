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

    @Id
    @SequenceGenerator(name="TEMPL_INST_SEQ_GENERATOR", sequenceName="TEMPL_INST_SEQ_GENERATOR", allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="TEMPL_INST_SEQ_GENERATOR")
    @Column(name = "id")
    private Long id;

    /**
     * The template
     */
    @Basic(optional=false)
    private TemplateDao template;

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

    public TemplateDao getTemplate() { return template; }

    public void setTemplate(TemplateDao template) { this.template = template; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateInstanceIasioDao that = (TemplateInstanceIasioDao) o;
        return instance == that.instance &&
                Objects.equals(id, that.id) &&
                Objects.equals(template, that.template) &&
                Objects.equals(iasio, that.iasio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, template, instance, iasio);
    }
}
