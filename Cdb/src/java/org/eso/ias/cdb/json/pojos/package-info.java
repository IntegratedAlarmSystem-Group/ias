/**
 * A package for the pojos used by Jakson2 libraries.
 * They differ from rdb pojos because we do not want to have recursion 
 * at this level that would make JSON files very long and unreadable.
 * 
 * Instead of embedding objecs (for example the ASCEs running in a DASU),
 * we want to have only their IDs in JSON files.
 * 
 * @author acaproni
 *
 */
package org.eso.ias.cdb.json.pojos;