/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.vm.operations

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.cryptography
import pravda.common.data.blockchain._
import pravda.common.vm.{Data, Error, MarshalledData}
import pravda.common.vm.Opcodes._
import pravda.common.vm.Error.{OperationDenied, UserError}
import pravda.vm.WattCounter._
import pravda.vm._
import pravda.vm.operations.annotation.OpcodeImplementation

import scala.collection.mutable

final class SystemOperations(program: ByteBuffer,
                             memory: Memory,
                             currentStorage: Option[Storage],
                             wattCounter: WattCounter,
                             env: Environment,
                             maybeProgramAddress: Option[Address],
                             standardLibrary: Map[Long, (Memory, WattCounter) => Unit],
                             vm: Vm) {

  import SystemOperations._

  @OpcodeImplementation(
    opcode = STOP,
    description = "Stops program execution."
  )
  def stop(): Unit = {
    // See VmImpl!
  }

  @OpcodeImplementation(
    opcode = CODE,
    description = "Take the address of a program. Pushes program bytecode to the stack"
  )
  def code(): Unit = {
    val programAddress = address(memory.pop())
    wattCounter.cpuUsage(CpuStorageUse)
    env.getProgram(programAddress) match {
      case Some(context) =>
        wattCounter.memoryUsage(context.code.size().toLong)
        memory.push(Data.Primitive.Bytes(context.code))
      case None => throw ThrowableVmError(Error.NoSuchProgram)
    }
  }

  @OpcodeImplementation(
    opcode = PCALL,
    description = "Takes two words by which it is followed. " +
      "They are address `a` and the number of parameters `n`, " +
      "respectively. Then it executes the program with " +
      "the address `a` and only passes there $n$ top elements " +
      "of the stack."
  )
  def pcall(): Unit = {
    wattCounter.cpuUsage(CpuExtCall, CpuStorageUse)
    val argumentsCount = integer(memory.pop())
    val programAddress = address(memory.pop())
    memory.limit(argumentsCount.toInt)
    memory.enterProgram(programAddress)
    vm.run(programAddress, env, memory, wattCounter, pcallAllowed = true)
    memory.exitProgram()
    memory.dropLimit()
    program.position(memory.currentCounter)
  }

  @OpcodeImplementation(
    opcode = LCALL,
    description = "Takes three words by which it is followed." +
      "They are address `a`, function `f` and the number of " +
      "parameters `n`, respectively. Then it executes the " +
      "function `f` of the library (which is a special form of program) " +
      "with address `a` and only passes there $n$ top elements " +
      "of the stack."
  )
  def lcall(): Unit = {
    val argumentsCount = integer(memory.pop())
    val addr = address(memory.pop())
    memory.limit(argumentsCount.toInt)
    memory.enterProgram(addr)
    vm.run(addr, env, memory, wattCounter, pcallAllowed = false) // TODO disallow to change storage
    memory.exitProgram()
    memory.dropLimit()
    program.position(memory.currentCounter)
  }

  @OpcodeImplementation(
    opcode = SCALL,
    description = "Takes id of the function from the standard library and executes it."
  )
  def scall(): Unit = {
    val id = integer(memory.pop())
    wattCounter.cpuUsage(CpuArithmetic)
    standardLibrary.get(id) match {
      case None           => throw ThrowableVmError(Error.InvalidAddress)
      case Some(function) => function(memory, wattCounter)
    }
  }

  @OpcodeImplementation(
    opcode = PUPDATE,
    description = "Takes the address of the existing program, code and signature. " +
      "The signature is computed from the concatenation of an old code and a new code of the program"
  )
  def pupdate(): Unit = {
    val signature = bytes(memory.pop()).toByteArray
    val newCode = bytes(memory.pop())
    val programAddress = address(memory.pop())

    wattCounter.cpuUsage(CpuStorageUse)

    env.getProgram(programAddress) match {
      case Some(ProgramContext(_, oldCode, false)) =>
        wattCounter.cpuUsage((newCode.size() + oldCode.size()) * CpuArithmetic * 2)
        val message = oldCode.concat(newCode).toByteArray
        if (cryptography.verify(programAddress.toByteArray, message, signature)) {
          wattCounter.storageUsage(newCode.size().toLong, oldCode.size().toLong)
          env.updateProgram(programAddress, newCode)
        } else {
          throw ThrowableVmError(Error.OperationDenied)
        }
      case Some(ProgramContext(_, _, true)) => throw ThrowableVmError(Error.ProgramIsSealed)
      case None                             => throw ThrowableVmError(Error.NoSuchProgram)
    }
  }

  @OpcodeImplementation(
    opcode = PCREATE,
    description = "Takes the address (pubKey), bytecode, and its ed25519 signature. " +
      "If the signature is valid and the program didn't exist before at the specified address, creates a new program."
  )
  def pcreate(): Unit = {
    val signature = bytes(memory.pop()).toByteArray
    val code = bytes(memory.pop())
    val programAddress = address(memory.pop())

    wattCounter.cpuUsage(CpuStorageUse)
    wattCounter.cpuUsage(code.size() * CpuArithmetic)

    env.getProgram(programAddress) match {
      case None if cryptography.verify(programAddress.toByteArray, code.toByteArray, signature) =>
        wattCounter.storageUsage(occupiedBytes = code.size().toLong)
        env.createProgram(programAddress, code)
      case _ =>
        throw ThrowableVmError(Error.OperationDenied)
    }
  }

  @OpcodeImplementation(
    opcode = SEAL,
    description = "Takes the address of the existing program and signature of code with a seal mark. " +
      "The signature is computed from the concatenation of the 'Seal' word (in UTF8 encoding) and the current program code."
  )
  def seal(): Unit = {
    val signature = bytes(memory.pop()).toByteArray
    val programAddress = address(memory.pop())

    wattCounter.cpuUsage(CpuStorageUse)

    env.getProgram(programAddress) match {
      case Some(ProgramContext(_, code, false)) =>
        wattCounter.cpuUsage(code.size() * CpuArithmetic * 2)
        val message = SealTag.concat(code).toByteArray // Seal ++ Code
        if (cryptography.verify(programAddress.toByteArray, message, signature)) {
          env.sealProgram(programAddress)
        } else {
          throw ThrowableVmError(Error.OperationDenied)
        }
      case Some(ProgramContext(_, _, true)) => throw ThrowableVmError(Error.ProgramIsSealed)
      case None                             => throw ThrowableVmError(Error.NoSuchProgram)
    }
  }

  @OpcodeImplementation(
    opcode = PADDR,
    description = "Gives the current program address."
  )
  def paddr(): Unit = {
    maybeProgramAddress match {
      case Some(programAddress) =>
        val data = address(programAddress)
        wattCounter.memoryUsage(data.volume.toLong)
        memory.push(data)
      case None => throw ThrowableVmError(OperationDenied)
    }
  }

  @OpcodeImplementation(
    opcode = FROM,
    description = "Gives the current executor address."
  )
  def from(): Unit = {
    val datum = address(env.executor)
    wattCounter.memoryUsage(datum.volume.toLong)
    memory.push(datum)
  }

  @OpcodeImplementation(
    opcode = THROW,
    description = "Takes a string from the stack and throws an error with description " +
      "as the given string that stops the program."
  )
  def `throw`(): Unit = {
    val message = utf8(memory.pop())
    throw ThrowableVmError(UserError(message))
  }

  @OpcodeImplementation(
    opcode = EVENT,
    description = "Takes string and arbitrary data from the stack, " +
      "create a new event with a name as the given string and with the given data."
  )
  def event(): Unit = {

    def marshalData(data: Data): (Data, Map[Data.Primitive.Ref, Data]) = {
      // (Original -> (UpdatedData, AssignedRef))
      val pHeap = mutable.Map.empty[Data.Primitive.Ref, (Data.Primitive.Ref, Data)]
      def extract(ref: Data.Primitive.Ref) = {
        val k = Data.Primitive.Ref(pHeap.size)
        val v = aux(memory.heapGet(ref))
        wattCounter.storageUsage(k.volume.toLong)
        wattCounter.storageUsage(v.volume.toLong)
        wattCounter.cpuUsage(10)
        pHeap.getOrElseUpdate(ref, k -> v)
      }
      def aux(data: Data): Data = data match {
        // TODO drop private fields by convention
        case struct: Data.Struct =>
          val updated = struct.data collect {
            case (_: Data.Primitive.Ref, _) =>
              // Using ref as key is not allowed for marshalling
              throw ThrowableVmError(Error.WrongType)
            case (k, orig: Data.Primitive.Ref) =>
              val (ref, _) = extract(orig)
              (k, ref)
            case tpl => tpl
          }
          Data.Struct(updated)
        case Data.Array.RefArray(xs) =>
          val updated = xs.map(x => extract(Data.Primitive.Ref(x))._1.data)
          Data.Array.RefArray(updated)
        case _ => data
      }
      (aux(data), pHeap.values.toMap)
    }

    val name = utf8(memory.pop())
    val data = memory.pop() match {
      case ref: Data.Primitive.Ref =>
        marshalData(memory.heapGet(ref)) match {
          case (d, ph) if ph.isEmpty => MarshalledData.Simple(d)
          case (d, ph)               => MarshalledData.Complex(d, ph)
        }
      case value => MarshalledData.Simple(value)
    }

    maybeProgramAddress match {
      case Some(addr) => env.event(addr, name, data)
      case None       => throw ThrowableVmError(OperationDenied)
    }
  }

  @OpcodeImplementation(
    opcode = CALLERS,
    description = "Gets caller's 'call stack' (see CALL opcode) " +
      "and pushes it to the stack"
  )
  def callers(): Unit = {
    val cs = Data.Array.BytesArray(
      memory.callStack.flatMap(_._1).toBuffer
    )
    wattCounter.memoryUsage(cs.volume.toLong)
    wattCounter.cpuUsage(CpuStorageUse)
    memory.push(memory.heapPut(cs))
  }

  @OpcodeImplementation(
    opcode = HEIGHT,
    description = "Gets the current height of the blockchain and pushes it to the stack."
  )
  def chainHeight(): Unit = {
    val data = env.chainHeight
    wattCounter.cpuUsage(CpuStorageUse)
    memory.push(Data.Primitive.Int64(data))
  }

  @OpcodeImplementation(
    opcode = HASH,
    description = "Gets the hash of the last block and pushes it to the stack."
  )
  def lastBlockHash(): Unit = {
    val data = env.lastBlockHash
    wattCounter.cpuUsage(CpuStorageUse)
    memory.push(Data.Primitive.Bytes(data))
  }

  @OpcodeImplementation(
    opcode = TIME,
    description = "Gets the timestamp of the last block and pushes it to the stack."
  )
  def lastBlockTime(): Unit = {
    val data = env.lastBlockTime
    wattCounter.cpuUsage(CpuStorageUse)
    memory.push(Data.Primitive.Int64(data))
  }

  @OpcodeImplementation(
    opcode = PEXIST,
    description =
      "Takes the address (pubKey). Returns true if the program with the given address exists, otherwise false"
  )
  def pexist(): Unit = {
    val programAddress = address(memory.pop())

    wattCounter.cpuUsage(CpuStorageUse)

    memory.push(Data.Primitive.Bool(env.getProgram(programAddress).isDefined))
  }

  @OpcodeImplementation(
    opcode = VUPDATE,
    description = "Takes address (pubKey). Returns true if the program with the given address exists, otherwise false."
  )
  def vupdate(): Unit = {
    val power = integer(memory.pop())
    val validatorAddress = address(memory.pop())

    wattCounter.cpuUsage(CpuStorageUse)

    env.updateValidator(validatorAddress, power)
  }
}

object SystemOperations {
  final val SealTag = ByteString.copyFromUtf8("Seal")
}
