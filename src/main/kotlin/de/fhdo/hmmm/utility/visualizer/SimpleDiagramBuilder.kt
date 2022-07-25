package de.fhdo.hmmm.utility.visualizer

import de.fhdo.hmmm.utility.model.Microservice
import de.fhdo.hmmm.utility.model.System
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.StringWriter
import java.io.Writer


/**
 * This class is responsible to create a **graph-based diagram** based on a given [System].
 * For each [Microservice] in the [System] the a vertex is generated.
 * Contracts are depicted as edges between vertices.
 *
 * The generate the graph, **JgraphT** as well as **GraphViz** are used.
 * *A working installation of GraphViz is required in order to use the visualization properly.*
 *
 * The class can be initialized like this:
 * ```
 * exampleDiagram = de.fhdo.hmmm.utility.visualizer.SimpleDiagram.Builder()
 * .system(yourSystem)
 * .outputFormat(de.fhdo.hmmm.utility.visualizer.EOutputFormat.SVG)
 * .build()
 * ```
 *
 * @author Jonas Sorgalla
 */
class SimpleDiagram private constructor(
    val system: System?,
    val format: EOutputFormat?
) : IDiagram {
    data class Builder(
        internal var system: System? = null,
        var format: EOutputFormat? = null
    ) {
        fun system(system: System) = apply { this.system = system }
        fun outputFormat(EOutputFormat: EOutputFormat) = apply { this.format = EOutputFormat }
        fun build() = SimpleDiagram(system, format)
    }

    val logger = LoggerFactory.getLogger(SimpleDiagram::class.java)

    /**
     * Builds a simple directed graph diagram build with **GraphViz**.
     *
     * First, **JGraphT** is used to generate and populate the graph.
     * Second, the JGraphT graph is transformed using a [DOTExporter] to its DOT representation.
     * The resulting DOT string is used to generate the actual diagram with GraphViz.
     *
     * Microservices are depicted as vertices. Contracts as edges. The owning interface of a contract is hidden
     * and instead the microservice owning the interface is used to draw the edge to the consuming microservice.
     *
     * If [system] or [format] is not set it throws an [Exception].
     *
     * @param file - the file where the generated image is persisted.
     */
    override fun visualize(file: File) {
        if (this.format == null)
            throw Exception("Need to set output format!")
        if (this.format != EOutputFormat.SVG)
            throw Exception("Unsupported output format for simple diagrams!")
        if (this.system == null)
            throw Exception("No system declared for visualization!")

        logger.debug(
            "It is heavily recommended to have the 'dot' command from GraphViz installed. " +
                    "Might timeout if command is not available."
        )

        logger.debug("Creating simple directed JGraphT graph...")
        val g: Graph<Microservice, DefaultEdge> = SimpleDirectedGraph(DefaultEdge::class.java)

        logger.debug("Populating graph...")
        this.system.microservices.forEach {
            logger.debug("Adding microservice ${it.name} as vertex...")
            g.addVertex(it)
        }
        logger.debug("Drawing edges...")
        this.system.contracts.forEach {
            logger.debug("Drawing edge between ${it.owner.provider} and ${it.consumer}...")
            g.addEdge(it.consumer, it.owner.provider)
        }
        logger.debug("JGraphT graph built.")
        logger.debug(g.toString())

        logger.debug("Transforming to DOT representation...")
        // GraphViz stuff
        val exporter: DOTExporter<Microservice, DefaultEdge> = DOTExporter { v -> v.name.toAlias() }
        exporter.setVertexAttributeProvider { v ->
            val map: MutableMap<String, Attribute> = LinkedHashMap<String, Attribute>()
            map["label"] = DefaultAttribute.createAttribute(v.name)
            map
        }
        val writer: Writer = StringWriter()
        exporter.exportGraph(g, writer)
        logger.info(writer.toString())

        val pictureGraph: MutableGraph = Parser().read(writer.toString())
        Graphviz.fromGraph(pictureGraph).width(1024).render(Format.SVG).toFile(file)
    }
}