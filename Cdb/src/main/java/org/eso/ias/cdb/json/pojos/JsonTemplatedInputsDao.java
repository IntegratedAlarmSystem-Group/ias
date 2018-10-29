package org.eso.ias.cdb.json.pojos;

import org.eso.ias.cdb.pojos.TemplateInstanceIasioDao;

import javax.persistence.Basic;
import java.util.Objects;

/**
 * Templated inputs of the ASCE
 *
 * These are the inputs that belongs to a different template
 * of that of the ASCE for which we need to specify the instance
 *
 * (@see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#124</A>)
 */
public class JsonTemplatedInputsDao {

    /**
     * The ID of the IASIO
     */
    @Basic(optional=false)
    private String iasioId;

    /**
     * The ID of the Template
     */
    @Basic(optional=false)
    private String templateId;

    /**
     * The number of the instance
     */
    @Basic(optional=false)
    private int instanceNum;

    /**
     * Empty constructor
     */
    public JsonTemplatedInputsDao() {}
    /**
     * Constructor
     *
     * @param iasioId The ID of the IASIO
     * @param templateId the ID of the template
     * @param instanceNum the number of the instance
     */
    public JsonTemplatedInputsDao(String iasioId, String templateId, int instanceNum) {
        super();
        if (iasioId==null || iasioId.isEmpty()) {
            throw new IllegalArgumentException("The ID of the IASIO can't be numm nor empty");
        }
        if (templateId==null || templateId.isEmpty()) {
            throw new IllegalArgumentException("The ID of the template can't be numm nor empty");
        }
        this.iasioId=iasioId;
        this.templateId=templateId;
        this.instanceNum=instanceNum;
    }

    public JsonTemplatedInputsDao(TemplateInstanceIasioDao tempInstanceDao) {
        Objects.requireNonNull(tempInstanceDao,"Invalid null TemplateInstanceIasioDao");
        this.iasioId =  tempInstanceDao.getIasio().getId();
        this.templateId=tempInstanceDao.getTemplateId();
        this.instanceNum=tempInstanceDao.getInstance();
    }

                                  /** Getter */
    public String getIasioId() {
        return iasioId;
    }

    /** Setter */
    public void setIasioId(String iasioId) {
        this.iasioId = iasioId;
    }

    /** Getter */
    public String getTemplateId() {
        return templateId;
    }

    /** Setter */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /** Getter */
    public int getInstanceNum() {
        return instanceNum;
    }

    /** Setter */
    public void setInstanceNum(int instanceNum) {
        this.instanceNum = instanceNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonTemplatedInputsDao that = (JsonTemplatedInputsDao) o;
        return instanceNum == that.instanceNum &&
                Objects.equals(iasioId, that.iasioId) &&
                Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iasioId, templateId, instanceNum);
    }
}
