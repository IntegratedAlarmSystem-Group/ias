/**
 * A package for the pojos used by Jakson2 libraries.
 * They differ from rdb pojos (org.eso.ias.cdb.pojos) 
 * because they use the IDs instead of including objects directly
 * that would make JSON files very long and unreadable.
 * 
 * In short, instead of embedding objects (for example the ASCEs running in a DASU),
 * we want to have only their IDs in JSON files.
 * 
 * @author acaproni
 *
 */
package org.eso.ias.cdb.json.pojos;