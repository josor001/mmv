import de.fhdo.hmmm.utility.visualizer.EOutputFormat
import de.fhdo.hmmm.utility.visualizer.UmlDiagram
import de.fhdo.hmmm.utility.visualizer.UmlDiagramType
import de.fhdo.hmmm.utility.model.*
import de.fhdo.hmmm.utility.visualizer.toAlias
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Tests for the generation of UML diagrams.
 *
 * This test class sets up a small test system comprising 2-3 microservices and tests the visualize method.
 * Generated artifacts are added to the generatedPictures dir
 * which is checked in to Git to be able to check the contents of the generated pictures.
 * To do this more properly, we may use JUnit5's TempDir extension in the future.
 *
 * @author Jonas Sorgalla
 */
internal class UmlDiagramTest {

    lateinit var testUmlDiagram: UmlDiagram

    @BeforeEach
    internal fun setUp() {
        // Build example system
        val testSystem = SystemFragment("MySystem")
        val teamA = Team("Team A")

        val testMicroservice = Microservice("My Microservice", teamA)
        testMicroservice.technology = null

        val testMicroservice2 = Microservice("Another Microservice", teamA)
        testMicroservice2.technology = null

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
        testSystem.contracts.add(Contract(testInterface, testMicroservice2))

        // Build UmlDiagram
        testUmlDiagram = UmlDiagram.Builder()
            .system(testSystem)
            .outputFormat(EOutputFormat.SVG)
            .type(UmlDiagramType.COMPONENT)
            .build()
    }


    @Test
    fun visualize() {
        val resourceFile: Path = Paths.get(
            "src", "test", "resources", "gen",
            "${testUmlDiagram.system!!.name.toAlias()}_umlDiagram.svg"
        )
        testUmlDiagram.visualize(resourceFile.toFile())

        assertTrue(File(resourceFile.toFile().absolutePath).exists())
    }

    @Test
    fun visualizeWithInvalidContract() {
        val testService = Microservice("test", Team("Team B"))
        testUmlDiagram.system!!.contracts.add(Contract(Interface("", testService), testService))

        assertThrows(Exception::class.java) {
            val resourceFile: Path = Paths.get(
                "src", "test", "resources", "gen",
                "${testUmlDiagram.system!!.name.toAlias()}_umlDiagram2.svg"
            )
            testUmlDiagram.visualize(resourceFile.toFile())
        }
    }
}