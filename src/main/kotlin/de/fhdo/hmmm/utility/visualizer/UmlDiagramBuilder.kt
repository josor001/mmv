package de.fhdo.hmmm.utility.visualizer

import de.fhdo.hmmm.utility.model.*
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Enum which holds all valid UML diagram types for the visualization.
 * Currently, only the component diagram type is supported.
 *
 * @author Jonas Sorgalla
 */
enum class UmlDiagramType {
    COMPONENT,
}

/**
 * This class is responsible to create a **UML component diagram** based on a given [System].
 * For each [Microservice] in the [System] the textual **PlantUML** notation is additively generated.
 * The complete string is transformed to a diagram graphic with the mechanism provided by PlantUML.
 * *PlantUML requires GraphViz installed on the executing system.*
 *
 * The class can be initialized like this:
 * ```
 * umlDiagram = de.fhdo.hmmm.utility.visualizer.UmlDiagram.Builder()
 * .system(yourSystem)
 * .outputFormat(de.fhdo.hmmm.utility.visualizer.EOutputFormat.SVG)
 * .type(de.fhdo.hmmm.utility.visualizer.UmlDiagramType.COMPONENT)
 * .build()
 * ```
 *
 * @author Jonas Sorgalla
 */
class UmlDiagram private constructor(
    val system: System?,
    val type: UmlDiagramType?,
    val format: EOutputFormat?
) : IDiagram {
    data class Builder(
        var system: System? = null,
        var type: UmlDiagramType? = null,
        var format: EOutputFormat? = null
    ) {
        fun system(system: System) = apply { this.system = system }
        fun type(type: UmlDiagramType) = apply { this.type = type }
        fun outputFormat(EOutputFormat: EOutputFormat) = apply { this.format = EOutputFormat }
        fun build(): UmlDiagram = UmlDiagram(system, type, format)
    }

    val logger = LoggerFactory.getLogger(UmlDiagram::class.java)

    /**
     * Builds a PlantUML component diagram calling the corresponding *stringify* methods for all objects
     * in the given [System]. If [system], [type], or [format] is not set it throws an [Exception].
     *
     * @param file - the file where the generated image is persisted.
     */
    override fun visualize(file: File) {
        if (this.type == null || this.format == null)
            throw Exception("Need to set type and output format!")
        if (this.type != UmlDiagramType.COMPONENT)
            throw Exception("Unsupported de.fhdo.hmmm.utility.visualizer.UmlDiagramType!")
        if (this.format != EOutputFormat.SVG)
            throw Exception("Unsupported output format for UML diagrams!")

        logger.debug(
            "It is heavily recommended to have the 'dot' command from GraphViz installed. " +
                    "Might timeout if command is not available."
        )

        logger.debug("Starting to assemble PlantUML string...")
        var diagram = "@startuml\n"
        diagram += "allow_mixing\n"

        if (system == null)
            throw Exception("No system found to visualize.")

        logger.debug("Populating with microservices...")
        this.system.microservices.forEach { diagram += stringifyMicroservice(it) }
        logger.debug("Microservices added!")
        logger.debug("Populating with contracts...")
        this.system.contracts.forEach { c ->
            // check whether any of system's microservice provides the interface
            var validOwner: Boolean = false
            var validConsumer: Boolean = false
            this.system.microservices.forEach { m ->
                if (m.interfaces.contains(c.owner))
                    validOwner = true
            }
            if (this.system.microservices.contains(c.consumer))
                validConsumer = true

            if (validOwner && validConsumer) {
                diagram += stringifyContract(c)
            } else {
                throw Exception("Owner or consumer of a contract were not previously defined.")
            }
        }
        logger.debug("Contracts added!")
        diagram += "@enduml\n"
        logger.debug("Writing PlantUML string to file...")
        val reader = SourceStringReader(diagram)
        val os = file.outputStream()
        reader.outputImage(os, FileFormatOption(FileFormat.SVG))
        os.close()
        logger.info(file.absolutePath)
    }

    /**
     * Transforms a [Microservice] to its [String] representation in PlantUML notation.
     * @param m the [Microservice] to transform.
     * @return [String] in PlantUML notation.
     */
    private fun stringifyMicroservice(m: Microservice): String {
        var ret: String = ""
        ret += "component [${m.name}] << Microservice >> as ${m.name.toAlias()}\n"
        m.interfaces.forEach { ret += stringifyInterface(it) }
        m.interfaces.forEach { ret += "${m.name.toAlias()} ..|>  ${it.name.toAlias()}\n" }
        return ret
    }

    /**
     * Transforms an [Interface] to its [String] representation in PlantUML notation.
     * @param i the [Interface] to transform.
     * @return [String] in PlantUML notation.
     */
    private fun stringifyInterface(i: Interface): String {
        return "interface ${i.name.toAlias()} {\n" +
                (if (!i.endpoint.isNullOrEmpty()) "endpoint = ${i.endpoint}\n" else "") +
                (if (!i.communicationType.isNullOrEmpty()) "communicationType = ${i.communicationType}\n" else "") +
                i.operations.joinToString(separator = "\n", transform = { it -> stringifyOperation(it) }) + "\n" +
                "}\n"
    }

    /**
     * Transforms an [Operation] to its [String] representation in PlantUML notation.
     * @param o the [Operation] to transform.
     * @return [String] in PlantUML notation.
     */
    private fun stringifyOperation(o: Operation): String {
        return "${o.returnValue ?: "void"} ${o.name}(" +
                "${o.parameters.joinToString(separator = ", ", transform = { it -> stringifyParameter(it) })}" +
                ")"
    }

    /**
     * Transforms an [Parameter] to its [String] representation in PlantUML notation.
     * @param p the [Parameter] to transform.
     * @return [String] in PlantUML notation.
     */
    private fun stringifyParameter(p: Parameter): String {
        return "${p.name} ${if (!p.type.isNullOrEmpty()) " : " + p.type else ""}"
    }

    /**
     * Transforms an [Contract] to its [String] representation in PlantUML notation.
     * @param c the [Contract] to transform.
     * @return [String] in PlantUML notation.
     */
    private fun stringifyContract(c: Contract): String {
        //   "Comp2 ..> Iface1 : consumes >\n"
        return "${c.consumer.name.toAlias()} ..> ${c.owner.name.toAlias()} : uses >\n"
    }
}



