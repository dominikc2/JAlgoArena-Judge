package com.jalgoarena.judge

import com.jalgoarena.compile.RubyCompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RubyCompilerTest {

    @Test
    fun compileAndRunStaticMethod() {

        val (instance, method) = RubyCompiler().compileMethod(
                "Solution", "greeting", 1, HELLO_WORLD_SOURCE_CODE
        )

        val result = method.invoke(instance, "Julia")
        assertThat(result).isEqualTo("Hello Julia")
    }

    companion object {

        private val HELLO_WORLD_SOURCE_CODE = """class Solution
  def greeting(name)
    "Hello #{name}"
  end
end
"""
    }
}
