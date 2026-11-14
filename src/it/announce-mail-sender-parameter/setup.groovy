
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.net.ServerSocket
import java.net.Socket

// A minimal SMTP server that accepts one message and keeps its raw text for verify.groovy.
ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName('127.0.0.1'))
List messages = Collections.synchronizedList([])

Thread thread = new Thread({
    while (!server.isClosed()) {
        try {
            Socket socket = server.accept()
            socket.withCloseable {
                def reader = new BufferedReader(new InputStreamReader(socket.inputStream, 'ISO-8859-1'))
                def writer = new PrintWriter(new OutputStreamWriter(socket.outputStream, 'ISO-8859-1'), true)
                def send = { String line -> writer.print(line + '\r\n'); writer.flush() }
                StringBuilder envelope = new StringBuilder()
                send('220 localhost fake SMTP')
                String line
                while ((line = reader.readLine()) != null) {
                    String cmd = line.toUpperCase()
                    if (cmd.startsWith('EHLO') || cmd.startsWith('HELO')) {
                        send('250 localhost')
                    } else if (cmd.startsWith('MAIL FROM') || cmd.startsWith('RCPT TO')) {
                        envelope.append(line).append('\n')
                        send('250 OK')
                    } else if (cmd.startsWith('DATA')) {
                        send('354 go ahead')
                        StringBuilder data = new StringBuilder()
                        while ((line = reader.readLine()) != null && line != '.') {
                            data.append(line).append('\n')
                        }
                        messages.add(envelope.toString() + '\n' + data.toString())
                        send('250 queued')
                    } else if (cmd.startsWith('QUIT')) {
                        send('221 bye')
                        break
                    } else {
                        send('250 OK')
                    }
                }
            }
        } catch (IOException ignored) {
            // server closed
        }
    }
} as Runnable)
thread.daemon = true
thread.start()

context.get('userProperties').put('smtpPort', String.valueOf(server.localPort))
context.put('smtpServer', server)
context.put('smtpMessages', messages)
true
