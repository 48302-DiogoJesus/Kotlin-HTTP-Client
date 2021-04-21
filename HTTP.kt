// FOR THE JAR FILE GO TO https://github.com/j3z-repos/Kotlin-HTTP-Client/releases/tag/1

import java.io.*
import java.net.InetAddress
import java.net.Socket
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("Commands:\n-> \"GET\" to show/hide GET Requests\n-> \"close\" or \"exit\" to close the HTTP client" +
            "\n\nValid URL formats:\n-> google.com\n-> www.google.com\n-> google.com/something\n-> www.google.com/something\n")
    while (true) {
        print("Insert the URL: ")
        val url = readLine()!!.toString()
        HTTP.resetVariables()
        HTTP.tcpClient(url)
    }
}

object HTTP {
    // VARIABLES TO MAKE THE "tcpClient()" WORK AS INTENDED
    private var validHost = ""
    private var validPath = ""
    private var showGetRequests = false

    // VARIABLES TO AVOID LOOPS (MAKE SURE WE ARE NOT REDIRECTING TO THE SAME IP TO MANY TIMES)
    private var previousIp = ""
    private var currentIp = "-"
    private var loopAvoidCounter = 0

    fun resetVariables() {
        previousIp = ""
        currentIp = "-"
        loopAvoidCounter = 0
    }

    /*
    FUNCTION THAT WILL ESTABLISH THE TCP SOCKET CONNECTION WITH THE TARGET,
    SEND AND RECEIVE HTTP MESSAGES
    */
    fun tcpClient(host: String,path:String="/") {

        if(host=="GET") {
            showGetRequests = !showGetRequests
            if (showGetRequests) println("GET Requests: ENABLED") else println("GET Requests: DISABLED")

        } else if (host=="close" || host=="exit") { exitProcess(-1) }

        else {
            //----MAKE SURE THE URL AND PATH ARE VALID BEFORE ESTABLISHING CONNECTION----//
            if (host.contains("/") && path != "/") {
                validPath = host.substring(path.indexOfFirst {it == '/'})
                validHost = host.substring(0,host.indexOfFirst { it == '/'})
            } else if (!host.contains("http") && path =="/" && host.contains("/")) {
                validHost = host.substring(0,host.indexOf("/"))
                validPath = host.substring(host.indexOf("/"))
            }
            else {
                validPath = path
                validHost = host
            }
            //--------------------------------------------------------------------------//

            //----TRY TO CONNECT AND SEND HTTP MESSAGES----//
            try {

                // RETURN ADDRESS (www.google.com/216.23.45.2) FROM HOSTNAME (www.google.com)
                val hostname = InetAddress.getByName(validHost)

                // RETURN IP ADDRESS (216.23.45.2) FROM ADDRESS (www.google.com/216.23.45.2)
                currentIp = hostname.hostAddress

                // OPEN A TCP SOCKET WITH THE TARGET IP IN PORT 80
                val socket = Socket(hostname, 80)

                //----TELL THE USER IF THE TCP SOCKET CONNECTION WAS SUCCESSFUL----//

                val successMessage = "| TCP Socket Connection established with $currentIp on port 80 |"
                println("")

                MISC.db(successMessage)
                println(successMessage)
                MISC.db(successMessage)

                //-----------------------------------------------------------------//

                //----GET REQUEST -> OPEN WRITE CHANNEL, BUILD REQUEST AND SEND IT----//

                val wr = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF8"))
                wr.write("GET $validPath HTTP/1.1\r\n")
                wr.write("Host: $currentIp\r\n")
                wr.write("Connection: close\r\n") // NO NEED FOR KEEP-ALIVE
                wr.write("\r\n")

                // SEND THE MESSAGE
                wr.flush()
                //--------------------------------------------------------------------//

                // SHOW GET REQUEST IF USER HAS SET "showGetRequests" TO "true"
                if (showGetRequests) println("\nHTTP GET Request:\n\nGET $validPath HTTP/1.1\r\nHost: $currentIp\r\nConnection: close\r")

                // START STREAM READER CHANNEL TO CAPTURE THE SERVER RESPONSE
                val rd = BufferedReader(InputStreamReader(socket.getInputStream()))

                // VARIABLE THAT WILL STORE THE FULL SERVER RESPONSE
                var response = ""

                //--------READ OUTPUT LINE BY LINE AND SAVE IT TO A VARIABLE--------//
                var line: String?
                while (rd.readLine().also { line = it } != null) {
                    response += line + "\n"
                }
                //------------------------------------------------------------------//

                // SENDS THE HTTP RESPONSE TO THE OUTPUT HANDLER
                outputHandler(response)

                // CLOSE THE READ AND WRITE CHANNELS
                wr.close()
                rd.close()

            } catch (e: Exception) {
                // IN CASE OF ERROR DURING THE PROCESS INSIDE THE "try" BLOCK THE ERROR MESSAGE WILL BE DISPLAYED
                println("\n${e.message}")
            }
        }
    }

    // RESPONSIBLE FOR TAKING IN THE HTTP RESPONSE AND MAKE REDIRECTIONS IF NEEDED
    private fun outputHandler(out:String) {

        if (out.length > 2000) {
            println("\nThe HTTP Response was trimmed because it had more than 2000 characters:\n\n${out.substring(0,2000)}\n\n")
        } else {
            println("\nHTTP Response:\n\n$out")
        }

        if (out.contains("301 Moved") || out.contains("302 Found") || out.contains("303 See Other") || out.contains("307 Temporary Redirect") || out.contains("308 Permanent Redirect")) {

            previousIp = currentIp

            // KEEP ALL THE TEXT AFTER "LOCATION: " IN THE RESPONSE
            val findLocation = out.substring(out.indexOf("Location:"))
            // GET THE REDIRECT URL = PATH
            val path = findLocation.substring(10,findLocation.indexOf("\n"))

            // GET THE DOMAIN FROM THE FULL LINK
            // https://192.168.1.6/hello -> 192.168.1.6/hello
            val url1 = path.substring(path.indexOf("/")+2)
            // 192.168.1.6/hello -> 192.168.1.6
            val host = url1.substring(0,url1.indexOf("/"))

            // CONTROLLING THE REDIRECTS (AVOID INFINITE LOOPS)
            if (loopAvoidCounter == 2) {
                println("\n$host is redirecting you to the same page repeatedly.\nYour HTTP client cant connect to: $host\n")
                loopAvoidCounter=0

            } else if (previousIp == currentIp) {

                loopAvoidCounter++
                println("You will be redirected to: $path")
                tcpClient(host,path)
            }
            else {
                loopAvoidCounter=0
                println("You will be redirected to: $host")
                tcpClient(host,path)
            }
        }
    }
}

// EXTRA
object MISC {
    // DRAW BORDERS EFFECT
    fun db(txt: String) {
        val i = txt.length
        for (a in 0..i) {
            if (a == i) {
                println("")
            } else {
                print("-")
            }
        }
    }
}
