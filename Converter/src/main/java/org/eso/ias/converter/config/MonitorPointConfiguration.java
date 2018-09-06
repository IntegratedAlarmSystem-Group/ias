package org.eso.ias.converter.config;

import org.eso.ias.types.IASTypes;

import java.util.Objects;
import java.util.Optional;

/**
 * The configuration of the monitor point as read from the configuration database.
 * <P>
 * Objects of this class do not contain all the configuration read
 * from the configuration database but only the part needed 
 * by the conversion.
 * 
 * @author acaproni
 *
 */
public class MonitorPointConfiguration {
	
	/**
	 * The type of the monitor point.
	 */
	public  final IASTypes mpType;

	/**
     * The min index of the template, if templated
     */
	public final Optional<Integer> minTemplateIndex;

    /**
     * The max index of the template, if templated
     */
    public final Optional<Integer> maxTemplateIndex;

	/**
	 * The type of the monitor point
	 * 
	 * @param mpType The type of the monitor point as read from the CDB
	 */
	public MonitorPointConfiguration(IASTypes mpType, Optional<Integer> minTemplate, Optional<Integer>maxTemplate) {
		super();
        Objects.requireNonNull(mpType);
        Objects.requireNonNull(minTemplate);
        Objects.requireNonNull(maxTemplate);

        int min = (minTemplate.isPresent())?1:0;
        int max = (maxTemplate.isPresent())?1:0;
        if ((min+max)==1) {
            throw new IllegalArgumentException("Invalid min/max indexes");
        }

		this.mpType = mpType;
		this.minTemplateIndex=minTemplate;
		this.maxTemplateIndex=maxTemplate;
	}

}
