import de.fhdo.hmmm.utility.visualizer.EOutputFormat
import de.fhdo.hmmm.utility.visualizer.SimpleDiagram
import de.fhdo.hmmm.utility.model.*
import de.fhdo.hmmm.utility.visualizer.toAlias
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


/**
 * Tests for the generation of simple graph-based diagrams.
 *
 * This test class sets up a small test system comprising 2-3 microservices and tests the visualize method.
 * Generated artifacts are added to the generatedPictures dir
 * which is checked in to Git to be able to check the contents of the generated pictures.
 * To do this more properly, we may use JUnit5's TempDir extension in the future.
 *
 * @author Jonas Sorgalla
 */
internal class SimpleDiagramTest {

    lateinit var testSimpleDiagram: SimpleDiagram

    @BeforeEach
    fun setup() {
        // Build example system
        val testSystem = System("MySystem")

        val testMicroservice = Microservice("My Microservice")
        testMicroservice.technology = null

        val testMicroservice2 = Microservice("Another Microservice")
        testMicroservice2.technology = null

        val testMicroservice3 = Microservice("Yet Another Microservice")
        testMicroservice3.technology = null

        val testInterface = Interface("MyInterface", testMicroservice)
        testInterface.endpoint = "TestEndpoint"
        testInterface.communicationType = null

        val testOperation1 = Operation("MyOperation1")
        testOperation1.returnValue = "String"

        val testOperation2 = Operation("MyOperation2")

        // Linking everything
        testMicroservice.interfaces.add(testInterface)

        testOperation1.parameters.add(Parameter("testParam", "TestType"))
        testOperation1.parameters.add(Parameter("testParam2"))

        testInterface.operations.add(testOperation1)
        testInterface.operations.add(testOperation2)

        testSystem.microservices.add(testMicroservice)
        testSystem.microservices.add(testMicroservice2)
        testSystem.microservices.add(testMicroservice3)
        testSystem.contracts.add(Contract(testInterface, testMicroservice2))
        testSystem.contracts.add(Contract(testInterface, testMicroservice3))

        testSimpleDiagram = SimpleDiagram.Builder()
            .system(testSystem)
            .outputFormat(EOutputFormat.SVG)
            .build()
    }

    @Test
    fun visualize() {
        val resourceFile: Path = Paths.get(
            "src", "test", "resources", "gen",
            "${testSimpleDiagram.system!!.name.toAlias()}_simplDiagram.svg"
        )
        testSimpleDiagram.visualize(resourceFile.toFile())
        assertTrue(File(resourceFile.toFile().absolutePath).exists())
    }
}