package org.eso.ias.cdb.pojos;


import javax.persistence.*;
import java.util.Objects;

/**
 * The pojo with the configuration of a IAS client
 *
 * The configuration is passed as a string.
 */
@Entity
@Table(name = "CLIENT_CONFIG")
public class ClientConfigDao {

    /**
     * Empty constructor
     */
    public ClientConfigDao() {}

    /**
     * Constructor
     *
     * @param id The id of the client
     * @param config the configuration
     */
    public ClientConfigDao(String id, String config) {
        if (id==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid null or empty id of the client");
        }
        this.id = id;
        this.config = config;
    }

    /**
     * The unique identifier of the client
     */
    @Id
    @Column(name = "client_id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    /**
     * The configuration
     *
     * The configuration is passed as a string whose format
     * can be selected by each client
     */
    @Basic(optional=false)
    private String config;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientConfigDao that = (ClientConfigDao) o;
        return id.equals(that.id) &&
                config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, config);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("ClientConfig=[");
        ret.append("id=");
        ret.append(getId());
        ret.append(", config=");
        ret.append(getConfig());

        ret.append("]");
        return ret.toString();
    }
}
