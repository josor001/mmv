package de.fhdo.hmmm.utility.visualizer

import org.apache.commons.lang3.StringUtils

/**
 * This file holds package level utility functions used by the diagram builders.
 *
 * @author Jonas Sorgalla
 */


/**
 * Extension method of [String] to build trimmed aliases without whitespaces,
 * e.g., for names of vertices or UML components.
 */
fun String.toAlias(): String {
    return StringUtils.remove(this.trim(), " ")
}