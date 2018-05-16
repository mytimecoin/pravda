package io.mytc.sood.vm.libs

import com.google.protobuf.ByteString
import io.mytc.sood.vm._
import io.mytc.sood.vm.VmUtils._
import io.mytc.sood.vm.Opcodes._
import serialization._

import utest._

object TypedTests extends TestSuite {
  val tests = Tests {
    'typedI32 - {
      val program = prog
        .opcode(PUSHX)
        .put(2)
        .opcode(PUSHX)
        .put(3)
        .opcode(PUSHX)
        .put(0x0abcdef1)
      val typedi32 = program
        .opcode(LCALL)
        .put("Typed")
        .put("typedI32")
        .put(3)

      exec(typedi32) ==> stack(binaryData(1, 0, 0, 0, 2),
        binaryData(1, 0, 0, 0, 3),
        binaryData(1, 0x0a, 0xbc, 0xde, 0xf1))
    }

    'typedR64 - {
      val program = prog
        .opcode(PUSHX)
        .put(1.0)
        .opcode(PUSHX)
        .put(math.Pi)

      exec(program) ==> stack(binaryData(0x3f, 0xf0, 0, 0, 0, 0, 0, 0),
        binaryData(0x40, 0x09, 0x21, 0xfb, 0x54, 0x44, 0x2d, 0x18))

      val typedr64 = program
        .opcode(LCALL)
        .put("Typed")
        .put("typedR64")
        .put(2)

      exec(typedr64) ==> stack(binaryData(2, 0x3f, 0xf0, 0, 0, 0, 0, 0, 0),
        binaryData(2, 0x40, 0x09, 0x21, 0xfb, 0x54, 0x44, 0x2d, 0x18))
    }

    def testTypedArithmetics(i1: Int,
                                     i2: Int,
                                     f1: Double,
                                     f2: Double,
                                     typedFunc: String,
                                     iFunc: (Int, Int) => Int,
                                     fFunc: (Double, Double) => Double): Unit

    =
    {

      val programI = prog
        .opcode(PUSHX)
        .put(i1)
        .opcode(PUSHX)
        .put(i2)

      val typedFuncI = programI
        .opcode(LCALL)
        .put("Typed")
        .put("typedI32")
        .put(2)
        .opcode(LCALL)
        .put("Typed")
        .put(typedFunc)
        .put(2)

      val execRes = exec(typedFuncI)

      execRes ==> stack(ByteString.copyFrom(Array(1.toByte)) concat int32ToData(iFunc(i1, i2)))

      val programR = prog
        .opcode(PUSHX)
        .put(f1)
        .opcode(PUSHX)
        .put(f2)

      val typedAddR = programR
        .opcode(LCALL)
        .put("Typed")
        .put("typedR64")
        .put(2)
        .opcode(LCALL)
        .put("Typed")
        .put(typedFunc)
        .put(2)

      exec(typedAddR) ==> stack(ByteString.copyFrom(Array(2.toByte)) concat doubleToData(fFunc(f1, f2)))
    }

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedAdd", _ + _, _ + _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMul", _ * _, _ * _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedDiv", _ / _, _ / _)

    * - testTypedArithmetics(1, 2, 1.0, 2.0, "typedMod", _ % _, _ % _)
  }
}