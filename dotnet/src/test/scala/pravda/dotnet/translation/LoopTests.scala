package pravda.dotnet

package translation

import pravda.vm.asm.PravdaAssembler
import utest._

object LoopTests extends TestSuite {

  val tests = Tests {
    'loopTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("loop.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |push null
            |sexist
            |jumpi @methods
            |call @ctor
            |@methods:
            |meta method { int8(-1): "loops", int8(-2): int8(0) }
            |dup
            |push "loops"
            |eq
            |jumpi @method_loops
            |jump @stop
            |@method_loops:
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(5)
            |swapn
            |pop
            |push int32(0)
            |push int32(4)
            |swapn
            |pop
            |jump @br17
            |@br7:
            |push int32(4)
            |dupn
            |push int32(2)
            |add
            |push int32(5)
            |swapn
            |pop
            |push int32(3)
            |dupn
            |push int32(1)
            |add
            |push int32(4)
            |swapn
            |pop
            |@br17:
            |push int32(3)
            |dupn
            |push int32(10)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(3)
            |swapn
            |pop
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |jumpi @br7
            |jump @br34
            |@br28:
            |push int32(4)
            |dupn
            |push int32(2)
            |mul
            |push int32(5)
            |swapn
            |pop
            |@br34:
            |push int32(4)
            |dupn
            |push int32(10000)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int32(1)
            |eq
            |jumpi @br28
            |pop
            |pop
            |pop
            |pop
            |pop
            |jump @stop
            |@ctor:
            |push null
            |dup
            |sput
            |ret
            |@stop:
          """.stripMargin).right.get
      )
    }

    'nestedLoopTranslation - {
      val Right((_, cilData, methods, signatures)) = parseFile("loop_nested.exe")

      assertWithAsmDiff(
        Translator.translateAsm(methods, cilData, signatures).right.get,
        PravdaAssembler.parse("""
            |push null
            |sexist
            |jumpi @methods
            |call @ctor
            |@methods:
            |meta method { int8(-1): "loops", int8(-2): int8(0) }
            |dup
            |push "loops"
            |eq
            |jumpi @method_loops
            |jump @stop
            |@method_loops:
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(0)
            |push int32(8)
            |swapn
            |pop
            |push int32(0)
            |push int32(7)
            |swapn
            |pop
            |jump @br58
            |@br7:
            |push int32(0)
            |push int32(6)
            |swapn
            |pop
            |jump @br42
            |@br12:
            |push int32(7)
            |dupn
            |push int32(2)
            |swap
            |mod
            |push int32(0)
            |eq
            |push int8(3)
            |cast
            |push int32(5)
            |swapn
            |pop
            |push int32(4)
            |dupn
            |push int8(9)
            |cast
            |not
            |push int8(3)
            |cast
            |push int32(1)
            |eq
            |jumpi @br37
            |push int32(7)
            |dupn
            |push int32(7)
            |dupn
            |push int32(7)
            |dupn
            |add
            |push int32(1000000007)
            |swap
            |mod
            |add
            |push int32(8)
            |swapn
            |pop
            |@br37:
            |push int32(5)
            |dupn
            |push int32(1)
            |add
            |push int32(6)
            |swapn
            |pop
            |@br42:
            |push int32(5)
            |dupn
            |push int32(20)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(4)
            |swapn
            |pop
            |push int32(3)
            |dupn
            |push int32(1)
            |eq
            |jumpi @br12
            |push int32(6)
            |dupn
            |push int32(1)
            |add
            |push int32(7)
            |swapn
            |pop
            |@br58:
            |push int32(6)
            |dupn
            |push int32(10)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(3)
            |swapn
            |pop
            |push int32(2)
            |dupn
            |push int32(1)
            |eq
            |jumpi @br7
            |jump @br77
            |@br71:
            |push int32(7)
            |dupn
            |push int32(2)
            |mul
            |push int32(8)
            |swapn
            |pop
            |@br77:
            |push int32(7)
            |dupn
            |push int32(10000)
            |swap
            |lt
            |push int8(3)
            |cast
            |push int32(2)
            |swapn
            |pop
            |push int32(1)
            |dupn
            |push int32(1)
            |eq
            |jumpi @br71
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |pop
            |jump @stop
            |@ctor:
            |push null
            |dup
            |sput
            |ret
            |@stop:
          """.stripMargin).right.get
      )
    }
  }
}
