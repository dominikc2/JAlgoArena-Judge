package com.jalgoarena.compile
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
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

            System.out.println(Date())
            when (compileAndReturnExitCode(out, sourceFile)) {
                ExitCode.OK -> return runWithJavaCompiler(className, out)
                else -> throw CompileErrorException(errMessageBytes.toString("utf-8"))
            }
        } finally {
            //tmpDir.deleteRecursively()
            System.setErr(origErr)
        }
    }

    private fun runWithJavaCompiler(className: String, out: File): MutableMap<String, ByteArray?> {
        System.out.println(Date())
        val compiler = InMemoryJavaCompiler()


        val javaSourceFile = out.listFiles()
                .filter { it.absolutePath.endsWith(".java") }
                .first()


        val source = File(out, javaSourceFile.name).readText()

        return compiler.run(className, source)
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
        val jRubyJar = File("lib/jruby-complete-9.1.14.0.jar")

        val classPath = listOf(
                jRubyJar,
                File("build/classes/main").absolutePath,
                File("build/resources/main").absolutePath
        ).joinToString(File.pathSeparator)


        var processBuilder = ProcessBuilder(
                "java",
                "-jar", jRubyJar.absolutePath,
                "-S", "jrubyc",
                sourceFile.name,
                "--java",
                "-t", "out"
        ).directory(out.parentFile)

        processBuilder.environment().set("JAVA_TOOL_OPTIONS", "-Xmx32m -Xss512k -Dfile.encoding=UTF-8")

        var process = processBuilder.inheritIO().start()
        process.waitFor(60, TimeUnit.SECONDS)
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
