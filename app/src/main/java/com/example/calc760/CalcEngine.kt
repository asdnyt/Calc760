package com.example.calc760

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Step(val op: String, val value: BigDecimal)

data class UiState(
    val text: String = "0",
    val memory: Boolean = false,
    val gt: Boolean = false,
    val error: Boolean = false,
    val tag: String = "",
    val on: Boolean = true
)

/** محرك حاسبة 12 خانة: CHECK & CORRECT, GT, MU, M+/M-/MRC, AUTO REPLAY */
class CalcEngine {
    var ui by mutableStateOf(UiState())
        private set

    private val mc = MathContext(20)
    private var entry = "0"
    private var entering = false
    private var shown = BigDecimal.ZERO
    private var acc: BigDecimal? = null
    private var op: String? = null
    private var lastOp: String? = null
    private var lastOperand = BigDecimal.ZERO
    private var mem = BigDecimal.ZERO
    private var gt = BigDecimal.ZERO
    private var error = false
    private var on = true
    private var lastKey = ""
    private val tape = mutableListOf<Step>()
    private var checkIdx = -1

    fun press(k: String) {
        try { handle(k) } catch (e: ArithmeticException) { error = true }
        lastKey = k
        publish()
    }

    private fun cur(): BigDecimal = if (entering) BigDecimal(entry) else shown

    private fun calc(a: BigDecimal, o: String, b: BigDecimal): BigDecimal = when (o) {
        "+" -> a.add(b)
        "-" -> a.subtract(b)
        "×" -> a.multiply(b)
        else -> a.divide(b, mc) // ÷ (ArithmeticException إذا القسمة على صفر)
    }

    private fun clearAll() {
        entry = "0"; entering = false; shown = BigDecimal.ZERO
        acc = null; op = null; lastOp = null
        tape.clear(); checkIdx = -1; error = false
    }

    private fun handle(k: String) {
        if (k == "AC") { on = true; clearAll(); return }
        if (!on) return
        if (k == "OFF") { on = false; clearAll(); return }
        if (k == "CE") { error = false; entry = "0"; entering = true; checkIdx = -1; return }
        if (error) return
        if (k != "CHK<" && k != "CHK>" && k != "CORRECT") checkIdx = -1
        when (k) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> digit(k)
            "00" -> digit(if (!entering || entry == "0") "0" else "00")
            "." -> {
                if (!entering) { entry = "0"; entering = true }
                if (!entry.contains('.')) entry += "."
            }
            "+", "-", "×", "÷" -> operator(k)
            "=" -> equals()
            "REPLAY" -> repeatLast()
            "±" -> { val c = cur().negate(); entry = c.toPlainString(); entering = true }
            "%" -> percent()
            "MU" -> markup()
            "√" -> {
                val c = cur()
                if (c.signum() < 0) throw ArithmeticException()
                shown = BigDecimal(Math.sqrt(c.toDouble())).round(MathContext(13))
                entering = false
            }
            "GT" -> {
                if (lastKey == "GT") gt = BigDecimal.ZERO
                shown = gt; entering = false
            }
            "M+" -> { if (op != null) equals(); val v = cur(); mem = mem.add(v); shown = v; entering = false }
            "M-" -> { if (op != null) equals(); val v = cur(); mem = mem.subtract(v); shown = v; entering = false }
            "MRC" -> {
                if (lastKey == "MRC") mem = BigDecimal.ZERO
                shown = mem; entering = false
            }
            "CHK<" -> checkMove(-1)
            "CHK>" -> checkMove(+1)
            "CORRECT" -> correct()
        }
    }

    private fun digit(s: String) {
        if (!entering) { entry = "0"; entering = true }
        val base = if (entry == "0") "" else entry
        val cand = base + s
        if (cand.count { it.isDigit() } <= 12) entry = if (cand.isEmpty()) "0" else cand
    }

    private fun addTape(s: Step) {
        tape.add(s)
        if (tape.size > 112) tape.removeAt(0)
    }

    private fun operator(o: String) {
        val c = cur()
        if (op == null) {
            acc = c; tape.clear(); addTape(Step("+", c))
        } else if (entering) {
            addTape(Step(op!!, c))
            acc = calc(acc!!, op!!, c)
        }
        op = o; entering = false; shown = acc!!
    }

    private fun equals() {
        val o = op
        if (o != null) {
            val c = cur()
            val res = calc(acc!!, o, c)
            addTape(Step(o, c))
            lastOp = o; lastOperand = c
            acc = res; shown = res; gt = gt.add(res)
            op = null; entering = false
        } else repeatLast()
    }

    private fun repeatLast() {
        val lo = lastOp ?: return
        val res = calc(cur(), lo, lastOperand)
        shown = res; acc = res; gt = gt.add(res); entering = false
    }

    private fun percent() {
        val o = op
        val b = cur()
        val h = BigDecimal(100)
        if (o == null) { shown = b.divide(h, mc); entering = false; return }
        val a = acc!!
        when (o) {
            "×" -> { val r = a.multiply(b).divide(h, mc); acc = r; shown = r; op = null; entering = false }
            "÷" -> { val r = a.divide(b, mc).multiply(h); acc = r; shown = r; op = null; entering = false }
            else -> { entry = a.multiply(b).divide(h, mc).toPlainString(); entering = true }
        }
    }

    /** Mark-Up: A × B MU = A(1+B%) ; A ÷ B MU = A/(1-B%) ; A - B MU = A(1-B%) */
    private fun markup() {
        val o = op ?: return
        val a = acc!!
        val p = cur().divide(BigDecimal(100), mc)
        val one = BigDecimal.ONE
        val r = when (o) {
            "÷" -> a.divide(one.subtract(p), mc)
            "-" -> a.multiply(one.subtract(p))
            else -> a.multiply(one.add(p))
        }
        acc = r; shown = r; op = null; entering = false
    }

    private fun checkMove(d: Int) {
        if (tape.isEmpty()) return
        checkIdx = when {
            checkIdx == -1 -> if (d < 0) tape.lastIndex else -1
            else -> {
                val n = checkIdx + d
                if (n < 0) 0 else if (n > tape.lastIndex) -1 else n
            }
        }
    }

    private fun correct() {
        if (checkIdx >= 0 && entering) {
            tape[checkIdx] = Step(tape[checkIdx].op, cur())
            var r = BigDecimal.ZERO
            for (s in tape) r = calc(r, s.op, s.value)
            acc = r; shown = r; op = null; entering = false; checkIdx = -1
        } else if (entering) {
            entry = entry.dropLast(1)
            if (entry.isEmpty() || entry == "-") entry = "0"
        }
    }

    private fun fmt(v: BigDecimal): String? {
        var x = v.stripTrailingZeros()
        if (x.signum() == 0) return "0"
        val intDigits = x.precision() - x.scale()
        if (intDigits > 12) return null
        val sc = minOf(maxOf(x.scale(), 0), 12 - maxOf(intDigits, 0))
        x = x.setScale(sc, RoundingMode.HALF_UP).stripTrailingZeros()
        return x.toPlainString()
    }

    private fun publish() {
        var tag = ""
        var text: String? = when {
            checkIdx >= 0 -> {
                val s = tape[checkIdx]
                tag = "CHECK ${checkIdx + 1}/${tape.size}  ${s.op}"
                fmt(s.value)
            }
            entering -> entry
            else -> fmt(shown)
        }
        if (text == null || fmt(gt) == null) { error = true }
        ui = UiState(
            text = if (error) "E" else text ?: "",
            memory = mem.signum() != 0,
            gt = gt.signum() != 0,
            error = error,
            tag = tag,
            on = on
        )
    }
}
