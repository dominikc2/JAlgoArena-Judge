package com.jalgoarena.compile
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

class RubyCompiler : JvmCompiler {
    override fun programmingLanguage() = "ruby"

    override fun run(className: String, source: String): MutableMap<String, ByteArray?> {

        val tmpDir = createTmpDir()
        val origErr = System.err

        try {
            val sourceFile = writeSourceFile(tmpDir, "$className.rb", source)
            val out = File(tmpDir, "out")
            out.mkdirs()

            val errMessageBytes = ByteArrayOutputStream()
            System.setErr(PrintStream(errMessageBytes))

            when (compileAndReturnExitCode(out, sourceFile)) {
                ExitCode.OK -> return readClassBytes(out)
                else -> throw CompileErrorException(errMessageBytes.toString("utf-8"))
            }
        } finally {
            tmpDir.deleteRecursively()
            System.setErr(origErr)
        }
    }

    private fun readClassBytes(out: File): MutableMap<String, ByteArray?> {
        val classBytes: MutableMap<String, ByteArray?> = HashMap()

        out.listFiles()
                .filter { it.absolutePath.endsWith(".class") }
                .forEach {
                    val byteCode = File(out, it.name).readBytes()
                    classBytes.put(it.nameWithoutExtension, byteCode)
                }

        return classBytes
    }

    private fun compileAndReturnExitCode(out: File, sourceFile: File): ExitCode {
        val home = System.getProperty("user.home")

        val command = listOf(
                Paths.get(home, ".rbenv", "shims", "jrubyc").toString(),
                sourceFile.absolutePath,
                "--javac",
                "-t", out.absolutePath,
                "-classpath", listOf(
                File("build/classes/main").absolutePath,
                File("build/resources/main").absolutePath
        ).joinToString(File.pathSeparator)
        ).joinToString(" ")

        val runtime = Runtime.getRuntime()
        val process = runtime.exec(command)
        process.waitFor(10, TimeUnit.SECONDS)

        return processToExitCode(process)
    }


    private fun processToExitCode(process: Process): ExitCode {
        return when (process.exitValue()) {
            0 -> ExitCode.OK
            1 -> ExitCode.COMPILATION_ERROR
            3 -> ExitCode.SCRIPT_EXECUTION_ERROR
            else -> {
                ExitCode.INTERNAL_ERROR
            }
        }
    }

    private fun createTmpDir(): File {
        val tmpDir = File("tmp", "${UUID.randomUUID()}")
        tmpDir.mkdirs()
        return tmpDir
    }

    private fun writeSourceFile(tmpDir: File, fileName: String, sourceCode: String): File {
        val sourceFile = File(tmpDir, fileName)
        sourceFile.writeText(sourceCode)
        return sourceFile
    }
}
