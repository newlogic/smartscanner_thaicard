import java.util.*
import javax.smartcardio.*

fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }

fun String.toByteArray(): ByteArray {
    val s = replace("\\s".toRegex(), "")
    val buf = ByteArray(s.length / 2)
    var i = 0
    var j = 0
    while (i < s.length && j < buf.size) {
        buf[j] = s.substring(i, i + 2).toInt(16).toByte()
        i += 2
        j++
    }
    return buf
}

lateinit var channel: CardChannel
lateinit var GET_RESPONSE: ByteArray

val AID_TEST_APPLET = "07 DEC0DE00000101".toByteArray() // my Java Card test applet
val AID_THAI_APPLET_DAT = "08 A0 000000 54480001".toByteArray() // storage data
val AID_THAI_APPLET_EXT = "08 A0 000000 84060002".toByteArray() // extension
val AID_THAI_APPLET_BIO = "08 A0 000000 84060000".toByteArray() // bio

val CMD_SELECT      = "00A40400".toByteArray()

/**
 * Thailand card applet specific APDUs
 */
val GET_RESPONSE1   = "00C00001".toByteArray()
val GET_RESPONSE0   = "00C00000".toByteArray()

val CMD_CID         = "80B00004 02 000D".toByteArray()
val CMD_THFULLNAME  = "80B00011 02 0064".toByteArray()
val CMD_ENFULLNAME  = "80B00075 02 0064".toByteArray()
val CMD_BIRTH       = "80B000D9 02 0008".toByteArray()
val CMD_GENDER      = "80B000E1 02 0001".toByteArray()
val CMD_ISSUER      = "80B000F6 02 0064".toByteArray()
val CMD_ISSUE       = "80B00167 02 0008".toByteArray()
val CMD_EXPIRE      = "80B0016F 02 0008".toByteArray()
val CMD_ADDRESS     = "80B01579 02 0064".toByteArray()

val CMD_PHOTO1      = "80B0017B 02 00FF".toByteArray()
val CMD_PHOTO2      = "80B0027A 02 00FF".toByteArray()
val CMD_PHOTO3      = "80B00379 02 00FF".toByteArray()
val CMD_PHOTO4      = "80B00478 02 00FF".toByteArray()
val CMD_PHOTO5      = "80B00577 02 00FF".toByteArray()
val CMD_PHOTO6      = "80B00676 02 00FF".toByteArray()
val CMD_PHOTO7      = "80B00775 02 00FF".toByteArray()
val CMD_PHOTO8      = "80B00874 02 00FF".toByteArray()
val CMD_PHOTO9      = "80B00973 02 00FF".toByteArray()
val CMD_PHOTO10     = "80B00A72 02 00FF".toByteArray()
val CMD_PHOTO11     = "80B00B71 02 00FF".toByteArray()
val CMD_PHOTO12     = "80B00C70 02 00FF".toByteArray()
val CMD_PHOTO13     = "80B00D6F 02 00FF".toByteArray()
val CMD_PHOTO14     = "80B00E6E 02 00FF".toByteArray()
val CMD_PHOTO15     = "80B00F6D 02 00FF".toByteArray()
val CMD_PHOTO16     = "80B0106C 02 00FF".toByteArray()
val CMD_PHOTO17     = "80B0116B 02 00FF".toByteArray()
val CMD_PHOTO18     = "80B0126A 02 00FF".toByteArray()
val CMD_PHOTO19     = "80B01369 02 00FF".toByteArray()
val CMD_PHOTO20     = "80B01468 02 00FF".toByteArray()

fun main(args: Array<String>)
{
    val tf = TerminalFactory.getDefault()

    try {
        val terminals = tf.terminals().list()
        if (terminals.size == 0) {
            print("No terminals")
            return
        }

        var i = 1
        terminals.forEach {
            println("$i) $it")
            i++
        }

        val kbInput = Scanner(System.`in`)
        print("Enter a number: ")
        var index = kbInput.nextInt()
        index--

        val terminal = terminals[index] ?: return

        if (!terminal.isCardPresent) {
            println("No Card detected")
            return
        }

        println("Connecting ...")

        val card = terminal.connect("*")
        channel = card.basicChannel

        val atr = card.atr
        val atrBytes = atr.bytes

        println("atrBytes: ${atrBytes.toHexString()}")

        if (atrBytes[0] == 0x3B.toByte() && atrBytes[1] == 0x67.toByte()) {
            GET_RESPONSE = GET_RESPONSE1
        } else {
            GET_RESPONSE = GET_RESPONSE0
        }

        var commands = mutableListOf<ByteArray>(
            CMD_CID,
            CMD_ENFULLNAME,
            CMD_THFULLNAME,
            CMD_BIRTH,
            CMD_GENDER,
            CMD_ISSUER,
            CMD_ISSUE,
            CMD_EXPIRE,
            CMD_ADDRESS
        )

        commands.forEach {
            testCommand(it)
        }

        card.disconnect(true)

    } catch (e: Exception) {
        println(e.message)
    }
}

fun testCommand(cmd: ByteArray) {
    testSelectApplet(AID_THAI_APPLET_DAT)
    testReadThaiCardContent(cmd)
}

fun testReadThaiCardContent(cmd: ByteArray) {
    try {
        println("cmd: ${cmd.toHexString()}")
        val resp1 = channel.transmit(CommandAPDU(cmd))
        println("resp1: ${resp1.bytes.toHexString()}")
        //val resp2 = channel.transmit(CommandAPDU(GET_RESPONSE + cmd.last()))
        //println("resp2: ${resp2.bytes.toHexString()}")

        println("------------------oOo-------------------")
    } catch (e: Exception) {
        println("Exception: ${e.message}")
    }
}

fun testSelectApplet(aid: ByteArray) {
    println("Selecting aid: ${aid.toHexString()}")
    val cmd = CommandAPDU(CMD_SELECT + aid)
    val resp = channel.transmit(cmd)
    val sw = "0x%X".format(resp.sw)
    println("sw = ${sw}")
}

/**
 * AID_TEST_APPLET is default-selected
 */
fun testJavaCardApplet() {
    val payload = "04 FACEBABE".toByteArray()
    val echoCmd = CommandAPDU(0x00, 0xEC, 0x00, 0x00, payload) // echo INStruction
    val resp1 = channel.transmit(echoCmd)

    println("resp1: ${resp1.bytes.toHexString()}")

    val readCmd = CommandAPDU(0x00, 0x2D, 0x00, 0x00) // read INStruction
    val resp2 = channel.transmit(readCmd)
    val data = resp2.data
    val buf = resp2.bytes

    println("data: ${data.toHexString()}")
    println("buf: ${buf.toHexString()}")
}
