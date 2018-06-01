package pravda.dotnet

import pravda.dotnet.CIL._
import pravda.dotnet.Signatures.SigType._
import pravda.dotnet.Signatures._
import pravda.dotnet.TablesData._
import utest._

object ObjectsTests extends TestSuite {

  val tests = Tests {
    'objectsParse - {
      val Right((_, cilData, methods, signatures)) = FileParser.parsePe("objects.exe")
      methods ==> List(
        Method(List(LdArg0,
                    Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
                    Nop,
                    Nop,
                    LdArg0,
                    LdArg1,
                    StFld(FieldData(1, "a", 37)),
                    Ret),
               0,
               None),
        Method(List(Nop, LdArg0, LdFld(FieldData(1, "a", 37)), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
               2,
               Some(16)),
        Method(List(LdArg0,
                    Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
                    Nop,
                    Nop,
                    LdArg0,
                    LdArg1,
                    StFld(FieldData(1, "b", 37)),
                    Ret),
               0,
               None),
        Method(List(Nop, LdArg0, LdFld(FieldData(1, "b", 37)), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
               2,
               Some(16)),
        Method(
          List(
            Nop,
            LdcI4S(-42),
            NewObj(MethodDefData(0, 6278, ".ctor", 1, List(ParamData(0, 1, "_a")))),
            StLoc0,
            LdcI40,
            NewObj(MethodDefData(0, 6278, ".ctor", 1, List(ParamData(0, 1, "_b")))),
            StLoc1,
            LdLoc0,
            CallVirt(MethodDefData(0, 134, "answerA", 40, List())),
            LdLoc1,
            CallVirt(MethodDefData(0, 134, "answerB", 40, List())),
            Add,
            StLoc2,
            Ret
          ),
          2,
          Some(20)
        ),
        Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
      )

      signatures.toList.sortBy(_._1) ==> List(
        (1, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false)))),
        (6, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List())),
        (10,
         MethodRefDefSig(true,
                         false,
                         false,
                         false,
                         0,
                         Tpe(Void, false),
                         List(Tpe(ValueTpe(TypeRefData(15, "DebuggingModes", "")), false)))),
        (16, LocalVarSig(List(LocalVar(I4, false)))),
        (20,
         LocalVarSig(
           List(
             LocalVar(
               Cls(TypeDefData(
                 1048577,
                 "A",
                 "",
                 Ignored,
                 List(FieldData(1, "a", 37)),
                 List(MethodDefData(0, 6278, ".ctor", 1, List(ParamData(0, 1, "_a"))),
                      MethodDefData(0, 134, "answerA", 40, List()))
               )),
               false
             ),
             LocalVar(
               Cls(TypeDefData(
                 1048577,
                 "B",
                 "",
                 Ignored,
                 List(FieldData(1, "b", 37)),
                 List(MethodDefData(0, 6278, ".ctor", 1, List(ParamData(0, 1, "_b"))),
                      MethodDefData(0, 134, "answerB", 40, List()))
               )),
               false
             ),
             LocalVar(I4, false)
           ))),
        (37, FieldSig(I4)),
        (40, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
        (44, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
      )

    }
  }
}
