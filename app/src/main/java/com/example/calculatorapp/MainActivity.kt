package com.example.calculatorapp // CHANGE THIS TO MATCH YOUR PROJECT

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.databinding.ActivityMainBinding // CHANGE THIS TO MATCH YOUR PROJECT

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var clipboard: ClipboardManager
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val appendableButtons = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnPlus to "+", binding.btnMinus to "-",
            binding.btnMultiply to "*", binding.btnDivide to "/", binding.btnDot to "."
            // Removed parentheses buttons as they are complex to add to this eval logic
        )

        appendableButtons.forEach { (button, text) ->
            button.setOnClickListener {
                if (binding.tvInput.text.toString() == "Error") {
                    binding.tvInput.text = ""
                }
                binding.tvInput.append(text)
                vibrate()
            }
        }

        binding.btnClear.setOnClickListener {
            binding.tvInput.text = ""
            vibrate()
        }

        binding.btnDelete.setOnClickListener {
            val current = binding.tvInput.text.toString()
            if (current.isNotEmpty()) {
                binding.tvInput.text = current.dropLast(1)
            }
            vibrate()
        }

        binding.btnEqual.setOnClickListener {
            val input = binding.tvInput.text.toString()
            try {
                if (input.isBlank()) return@setOnClickListener
                val result = eval(input)
                val formatted = formatResult(result)
                binding.tvInput.text = formatted
                copyToClipboard(formatted)
                vibrate()
            } catch (e: Exception) {
                binding.tvInput.text = "Error"
            }
        }

        // Connect the new feature buttons
        binding.btnPercent.setOnClickListener { onPercentClicked() }
        binding.btnSqrt.setOnClickListener { onSqrtClicked() }
    }

    private fun onPercentClicked() {
        try {
            val result = eval(binding.tvInput.text.toString()) / 100
            binding.tvInput.text = formatResult(result)
        } catch (e: Exception) {
            binding.tvInput.text = "Error"
        }
        vibrate()
    }

    private fun onSqrtClicked() {
        try {
            val result = eval(binding.tvInput.text.toString())
            if (result < 0) {
                binding.tvInput.text = "Error"
            } else {
                binding.tvInput.text = formatResult(kotlin.math.sqrt(result))
            }
        } catch (e: Exception) {
            binding.tvInput.text = "Error"
        }
        vibrate()
    }

    private fun eval(expression: String): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() { ch = if (++pos < expression.length) expression[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected: " + expression[pos])
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    x = when {
                        eat('+'.code) -> x + parseTerm()
                        eat('-'.code) -> x - parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    x = when {
                        eat('*'.code) -> x * parseFactor()
                        eat('/'.code) -> x / parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing ')'")
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }

    private fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.5f", result).trimEnd('0').trimEnd('.')
        }
    }

    private fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Result copied!", Toast.LENGTH_SHORT).show()
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}