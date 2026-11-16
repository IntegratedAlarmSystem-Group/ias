package org.eso.ias.pytfserver.test

import org.eso.ias.logging.IASLogger

import scala.util.Try

/** Shared base that starts/manages a PyTfServer subprocess for integration tests.
  *
  * Spawns `iasPyTfServer` from IAS_ROOT/bin on a random free port in
  * [[startServer]] and tears it down in [[stopServer]].  The script is
  * invoked directly — PYTHONPATH and Java/Scala classpath are inherited from
  * `iasRun`, which already wires the full environment.  The test Python
  * modules directory ([[testPythonDir]]) is appended to PYTHONPATH so that
  * modules like `test_tf_module` are importable by the server.
  *
  * Subclasses get the server port via [[serverPort]] and should pass it to
  * `GrpcPythonExecutor` by using [[serverProps]].
  *
  * Concrete test classes should call [[startServer]] at the beginning of
  * `beforeAll` and [[stopServer]] at the end of `afterAll`.
  */
trait PyTfServerBase {

  protected val serverLogger = IASLogger.getLogger(classOf[PyTfServerBase])

  private var _serverProcess: Process | Null = null
  private var _serverPort: Int = 0

  /** The TCP port the server is listening on (0 until [[startServer]] runs). */
  protected def serverPort: Int = _serverPort

  /** Path to the Python test modules directory. */
  protected def testPythonDir(): String = {
    s"src/test/python"
  }

  /** Spawn the PyTfServer subprocess on a random free port. */
  def startServer(): Unit = {
    _serverPort = findFreePort()

    val serverScript = "iasPyTfServer"
    val cmd = List(serverScript, "-p", _serverPort.toString)
    serverLogger.info(s"Starting PyTfServer on port $_serverPort")

    val pb = new ProcessBuilder(cmd*)
    val existingPath = System.getenv().getOrDefault("PYTHONPATH", "")
    val testDir = testPythonDir()
    val extendedPath = if (existingPath.nonEmpty) s"$testDir:$existingPath" else testDir
    pb.environment().put("PYTHONPATH", extendedPath)
    _serverProcess = pb.start()

    Thread.sleep(2000)
    val isAlive = Try(_serverProcess.exitValue()).isFailure
    if (!isAlive) {
      val err = scala.io.Source.fromInputStream(_serverProcess.getInputStream).mkString
      val out = scala.io.Source.fromInputStream(_serverProcess.getErrorStream).mkString
      throw new IllegalStateException(s"PyTfServer failed to start. stdout: $err stderr: $out")
    }
    serverLogger.info(s"PyTfServer started successfully on port $_serverPort")
  }

  /** Destroy the PyTfServer subprocess. */
  def stopServer(): Unit = {
    if (_serverProcess != null) {
      _serverProcess.destroy()
      Try(_serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
      serverLogger.info(s"PyTfServer on port $_serverPort destroyed")
    }
  }

  /** Return a free TCP port on localhost using a zero-bound ServerSocket. */
  protected def findFreePort(): Int = {
    val serverSocket = new java.net.ServerSocket(0)
    val port = serverSocket.getLocalPort
    serverSocket.close()
    port
  }

  /** Returns a Properties instance with `pyTfServerAddr` set to the running server. */
  protected def serverProps(): java.util.Properties = {
    val p = new java.util.Properties()
    p.setProperty("pyTfServerAddr", s"localhost:$serverPort")
    p
  }
}
